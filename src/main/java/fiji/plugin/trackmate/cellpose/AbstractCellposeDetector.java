/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2026 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.cellpose;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateApposeProgressListener;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.detection.SpotMeshUtils;
import fiji.plugin.trackmate.detection.SpotRoiUtils;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.appose.ShmImg;
import net.imglib2.cellpose.ApposeTaskListener;
import net.imglib2.cellpose.AxisInfo;
import net.imglib2.cellpose.Cellpose;
import net.imglib2.cellpose.CellposeParameters;
import net.imglib2.cellpose.CellposeRunner;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.ImgUtil;
import net.imglib2.view.Views;

public abstract class AbstractCellposeDetector< T extends RealType< T > & NativeType< T >, C extends CellposeBaseConfig< ? > > implements SpotGlobalDetector< T >, Cancelable, MultiThreaded
{

	private final ImgPlus< T > img;

	private final Interval interval;

	private Logger logger = Logger.VOID_LOGGER;

	private final String baseErrorMessage;

	private String errorMessage;

	private long processingTime;

	private SpotCollection spots;

	private String cancelReason;

	private boolean isCanceled;

	private final C config;

	public AbstractCellposeDetector(
			final ImgPlus< T > img,
			final Interval interval,
			final C config )
	{
		this.img = img;
		this.config = config;
		final String command = "Cellpose 3";
		this.baseErrorMessage = "[" + command + "Detector] ";
		/*
		 * Preprocess the interval. TrackMate gives us an interval where the
		 * C-axis is missing. But Cellpose needs it.
		 */
		final int cAxis = img.dimensionIndex( Axes.CHANNEL );
		final long[] min = img.minAsLongArray();
		final long[] max = img.maxAsLongArray();
		for ( final AxisType axisType : Axes.knownTypes() )
		{
			final int index = img.dimensionIndex( axisType );
			if ( index < 0 )
				continue;
			if ( axisType.equals( Axes.CHANNEL ) )
			{
				min[ index ] = 0;
				max[ index ] = img.dimension( index ) - 1;
			}
			else
			{
				int id;
				if ( cAxis >= 0 && index >= cAxis )
					id = index - 1;
				else
					id = index;
				min[ index ] = interval.min( id );
				max[ index ] = interval.max( id );
			}
		}
		this.interval = new FinalInterval( min, max );
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		isCanceled = false;
		cancelReason = null;
		spots = new SpotCollection();

		// Convert config to Cellpose parameters.
		final CellposeParameters params = toParams( config );

		// Other params.
		final boolean simplify = config.simplifyContour().getValue();
		final double smoothingScale = config.smoothingScale().getValue();

		// Axis info (for one time point).
		final AxisInfo axisInfo = getAxisInfo( img ).removeTimeDim();
		final double[] calibration = TMUtils.getSpatialCalibration( img );

		// Adapt listener -> TrackMate logger.
		final ApposeTaskListener listener = new TrackMateApposeProgressListener( "TrackMate-Cellpose 3", logger );

		// Single time point for creating placeholders
		RandomAccessibleInterval< T > singleTP;
		final int timeAxis = img.dimensionIndex( Axes.TIME );
		long nT;
		long minT;
		long maxT;
		if ( timeAxis < 0 )
		{
			singleTP = Views.interval( img, interval );
			nT = 1;
			minT = 0;
			maxT = 1;
		}
		else
		{
			singleTP = Views.hyperSlice( Views.interval( img, interval ), timeAxis, 0 );
			nT = interval.dimension( timeAxis );
			minT = interval.min( timeAxis );
			maxT = interval.max( timeAxis ) + 1;
		}

		try (final ShmImg< T > inputShmImg = Cellpose.createInputShmImg( singleTP );
				final ShmImg< UnsignedShortType > outputShmImg = Cellpose.createOutputLabelsShmImg( singleTP, axisInfo.removeTimeDim(), new UnsignedShortType() );
				CellposeRunner< T, UnsignedShortType > runner = createRunner( params, listener, inputShmImg, axisInfo, outputShmImg );)
		{
			// Init the Cellpose runner.
			logger.setStatus( "Initializing Cellpose..." );
			runner.init();

			// Loop over time points.
			for ( long t = minT; t < maxT; t++ )
			{
				logger.setStatus( "Cellpose running " + ( t - minT + 1 ) + "/" + nT + "..." );
				if ( isCanceled() )
				{
					logger.log( "Canceled.\n" );
					return true;
				}

				// Copy current time point into inputShmImg.
				if ( timeAxis < 0 )
				{
					ImgUtil.copy( Views.interval( img, interval ), inputShmImg );
				}
				else
				{
					final RandomAccessibleInterval< T > currentTP = Views.hyperSlice( Views.interval( img, interval ), timeAxis, t );
					ImgUtil.copy( currentTP, inputShmImg );
				}

				// Run Cellpose on the current time point.
				runner.run();

				// Build a labeling from Cellpose outputShmImg.
				final AtomicInteger max = new AtomicInteger( 0 );
				outputShmImg.forEach( p -> {
					final int val = p.getInteger();
					if ( val != 0 && val > max.get() )
						max.set( val );
				} );
				final List< Integer > indices = new ArrayList<>( max.get() );
				for ( int i = 0; i < max.get(); i++ )
					indices.add( Integer.valueOf( i + 1 ) );

				final ImgLabeling< Integer, UnsignedShortType > labeling = ImgLabeling.fromImageAndLabels( outputShmImg, indices );

				// Detect spots from the labeling.

				final List< Spot > frameSpots;
				if ( DetectionUtils.is2D( img ) )
				{
					frameSpots = SpotRoiUtils.from2DLabelingWithROI(
							labeling,
							interval.minAsDoubleArray(),
							calibration,
							simplify,
							smoothingScale,
							null );
				}
				else
				{
					frameSpots = SpotMeshUtils.from3DLabelingWithROI(
							labeling,
							interval.minAsDoubleArray(),
							calibration,
							simplify,
							smoothingScale,
							null );
				}
				spots.put( ( int ) t, frameSpots );
				logger.setProgress( ( double ) ( t + 1 ) / nT );
			}
			logger.setStatus( "Cellpose done" );
			return true;
		}
		catch ( final BuildException e )
		{
			logger.error( "Cellpose environment build error: " + e.getMessage() );
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			logger.error( "Cellpose script I/O error: " + e.getMessage() );
			e.printStackTrace();
		}
		catch ( final InterruptedException e )
		{
			logger.error( "Cellpose process interrupted: " + e.getMessage() );
			e.printStackTrace();
		}
		catch ( final TaskException e )
		{
			logger.error( "Error running Cellpose: " + e.getMessage() );
			e.printStackTrace();
		}
		finally
		{
			logger.setProgress( 1. );
			final long end = System.currentTimeMillis();
			this.processingTime = end - start;
		}
		return false;
	}

	/**
	 * Instantiates a CellposeRunner for the given parameters and input/output
	 * images. Subclasses can override this method to provide a different runner
	 * for different versions of Cellpose.
	 *
	 * @param params
	 *            the Cellpose parameters.
	 * @param listener
	 *            the listener to report progress to.
	 * @param inputShmImg
	 *            the input image in shared memory.
	 * @param axisInfo
	 *            the axis information of the input image.
	 * @param outputShmImg
	 *            the output image in shared memory.
	 * @return a CellposeRunner instance.
	 */
	protected abstract CellposeRunner< T, UnsignedShortType > createRunner( final CellposeParameters params, final ApposeTaskListener listener, final ShmImg< T > inputShmImg, final AxisInfo axisInfo, final ShmImg< UnsignedShortType > outputShmImg ) throws BuildException, IOException, InterruptedException, TaskException;

	@Override
	public SpotCollection getResult()
	{
		return spots;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	// --- org.scijava.Cancelable methods ---

	@Override
	public boolean isCanceled()
	{
		return isCanceled;
	}

	@Override
	public void cancel( final String reason )
	{
		isCanceled = true;
		cancelReason = reason;
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	protected abstract CellposeParameters toParams( final C config );

	private static AxisInfo getAxisInfo( final ImgPlus< ? > img )
	{
		final int x = img.dimensionIndex( Axes.X );
		final int y = img.dimensionIndex( Axes.Y );
		final int c = img.dimensionIndex( Axes.CHANNEL );
		final int z = img.dimensionIndex( Axes.Z );
		final int t = img.dimensionIndex( Axes.TIME );
		return new AxisInfo( x, y, c, z, t );
	}

	@Override
	public void setNumThreads()
	{
		// Ignored
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		// Ignored
	}

	@Override
	public int getNumThreads()
	{
		return 1;
	}
}

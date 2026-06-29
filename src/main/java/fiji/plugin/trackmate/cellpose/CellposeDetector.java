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
import java.util.Arrays;
import java.util.List;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateApposeProgressListener;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.appose.ShmImg;
import net.imglib2.cellpose.ApposeTaskListener;
import net.imglib2.cellpose.AxisInfo;
import net.imglib2.cellpose.Cellpose;
import net.imglib2.cellpose.Cellpose3Parameters;
import net.imglib2.cellpose.CellposeRunner;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class CellposeDetector< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetector< T >, Cancelable, MultiThreaded
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

	private final Cellpose3Config config;

	public CellposeDetector(
			final ImgPlus< T > img,
			final Interval interval,
			final Cellpose3Config config )
	{
		this.img = img;
		this.interval = interval;
		this.config = config;
		final String command = "Cellpose 3";
		this.baseErrorMessage = "[" + command + "Detector] ";
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		isCanceled = false;
		cancelReason = null;

		// Convert config to Cellpose parameters.
		final Cellpose3Parameters params = toParams( config );

		// Axis info
		final AxisInfo axisInfo = getAxisInfo( img );

		// Adapt listener -> TrackMate logger.
		final TrackMateApposeProgressListener l = new TrackMateApposeProgressListener( "TrackMate-Cellpose 3", logger );
		final ApposeTaskListener listener; // TODO adapt to Cellpose task listener.

		try (ShmImg< T > inputShmImg = createInputShmImg( img, interval );
				ShmImg< UnsignedShortType > outputShmImg = createOutputShmImg( inputShmImg );
				CellposeRunner< T, UnsignedShortType > runner = Cellpose.cellposeRunner(
						params, listener, inputShmImg, axisInfo, outputShmImg, null );)
		{
			// TODO
		}
		catch ( final BuildException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( final InterruptedException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( final TaskException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			final long end = System.currentTimeMillis();
			this.processingTime = end - start;
		}
		return true;
	}

	private static final ShmImg< UnsignedShortType > createOutputShmImg( final ShmImg< ? > inputShmImg )
	{
		// TODO Auto-generated method stub
		return null;
	}

	private static final < T > ShmImg< T > createInputShmImg( final ImgPlus< T > img, final Interval interval )
	{
		// TODO Auto-generated method stub
		return null;
	}

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
		if ( img.dimensionIndex( Axes.Z ) >= 0 )
		{
			errorMessage = baseErrorMessage + "Image must be 2D over time, got an image with multiple Z.";
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

	private final static Cellpose3Parameters toParams( final Cellpose3Config config )
	{
		final List< Integer > channels = Arrays.asList(
				config.chan1().getValue(),
				config.chan2().getValue() );

		final String selection = config.builtinOrCustom().getSelection().getKey();
		final boolean isBuiltin = selection.equals( "BUILTIN_MODEL" );

		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( isBuiltin ? config.builtinModel().getValue() : null )
				.customModel( isBuiltin ? null : config.customModel().getValue() )
				.diameter( config.diameter().getValue() )
				.channels( channels )
				.minSize( config.minSize().getValue() )
				.resample( true ) // Must be true here, as we expect the output
									// to have the same size as the input.
				.cellProbThreshold( config.flowThreshold().getValue() )
				.flowThreshold( config.flowThreshold().getValue() )
				.do3D( config.mode3D().getValue() )
				.stitchThreshold( config.stitchThreshold().getValue() )
				.flow3dSmooth( config.flow3DSmooth().getValue() )
				.torchVersion( config.torchVersion().getValue() )
				.useGpu( config.useGpu().getValue() )
				.build();
		return params;
	}

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

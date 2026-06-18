/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2025 TrackMate developers.
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

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.input.Tailer;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LabelImageDetectorFactory;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.omnipose.OmniposeCLI;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.cli.CLIConfigurator;
import fiji.plugin.trackmate.util.cli.CLIUtils;
import fiji.plugin.trackmate.util.cli.CLIUtils.LoggerTailerListener;
import fiji.plugin.trackmate.util.cli.CommandBuilder;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.process.ImageConverter;
import ij.process.StackConverter;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class CellposeDetector< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetector< T >, Cancelable, MultiThreaded
{

	private static final Function< Long, String > nameGen = ( frame ) -> String.format( "%d", frame );

	private final ImgPlus< T > img;

	private final Interval interval;

	private  Logger logger = Logger.VOID_LOGGER;

	private final String baseErrorMessage;

	private String errorMessage;

	private long processingTime;

	private SpotCollection spots;

	private String cancelReason;

	private boolean isCanceled;

	private final List< CellposeTask > processes = new ArrayList<>();

	private int numThreads;

	private final File cellposeLogFile;

	private final ICellposeCLI cli;

	public CellposeDetector(
			final ImgPlus< T > img,
			final Interval interval,
			final ICellposeCLI cli )
	{
		this.img = img;
		this.interval = interval;
		this.cli = cli;
		final String command = cli.getCommand();
		this.cellposeLogFile = new File( new File( System.getProperty( "user.home" ), "." + command ), "run.log" );
		this.baseErrorMessage = "[" + command + "Detector] ";
	}

	@Override
	public boolean process()
	{
		final String command = cli.getCommand();
		final long start = System.currentTimeMillis();
		isCanceled = false;
		cancelReason = null;

		/*
		 * Do we have time? If yes we need to fetch the min time index to
		 * reposition the spots in the correct frame at the end of the
		 * detection.
		 */
		final int timeIndex = img.dimensionIndex( Axes.TIME );
		final int minT = ( int ) ( ( timeIndex < 0 ) ? 0 : interval.min( interval.numDimensions() - 1 ) );
		final double frameInterval = ( timeIndex < 0 ) ? 1. : img.averageScale( timeIndex );

		/*
		 * Do we have Z?
		 */
//		final boolean is3d = !DetectionUtils.is2D( img );
//		final double anisotropy;
//		if ( is3d )
//		{
//			final int xIndex = img.dimensionIndex( Axes.X );
//			final int zIndex = img.dimensionIndex( Axes.Z );
//			anisotropy = img.averageScale( zIndex ) / img.averageScale( xIndex );
//		}
//		else
//		{
//			anisotropy = 1.;
//		}

		// get images nb frames and nb channels before to do the cropping
		final int nslices = ( int ) img.dimension( img.dimensionIndex( Axes.Z ) );
//                final int nchannels = (int) img.dimension( img.dimensionIndex( Axes.CHANNEL ) );

		/*
		 * Dispatch time-points to several tasks.
		 */

		final List< ImagePlus > imps = CellposeUtils.crop( img, interval, nameGen );

		final int nConcurrentTasks;
		/*
		 * We use multiprocessing ONLY if the user stated that they want to use
		 * the CPU and if we are on Mac. I tested multiprocessing on CPU under
		 * windows, and there is no benefit for Windows. But there is a strong
		 * speedup on Mac.
		 *
		 * On a PC with Windows, forcing cellpose to run with the CPU: There is
		 * no benefit from splitting the load between 1,2, 10 or 20 processes.
		 * It seems like 1 cellpose process can already use ALL the cores by
		 * itself and running several cellpose processes concurrently does not
		 * lead to shorter processing time.
		 *
		 * For a source image 1024x502 over 92 time-points, 3 channels: - 1
		 * thread -> 24.4 min - 8 thread -> 4.1 min (there is not a x8 speedup
		 * factor, which is to be expected)
		 */
		if ( !cli.useGPU().getValue() && IJ.isMacintosh() )
		{
			nConcurrentTasks = numThreads;
		}
		else
		{
			nConcurrentTasks = 1;
		}

		final List< List< ImagePlus > > timepoints = new ArrayList<>( nConcurrentTasks );
		for ( int i = 0; i < nConcurrentTasks; i++ )
		{
			timepoints.add( new ArrayList<>() );
		}

		Iterator< List< ImagePlus > > it = timepoints.iterator();
		for ( int t = 0; t < imps.size(); t++ )
		{
			if ( !it.hasNext() )
			{
				it = timepoints.iterator();
			}
			it.next().add( imps.get( t ) );
		}

		/*
		 * Create tasks for each list of imps.
		 */

		processes.clear();
		for ( final List< ImagePlus > list : timepoints )
		{
			processes.add( new CellposeTask( list ) );
		}

		/*
		 * Pass tasks to executors.
		 */

		// Redirect log to logger.
		final Tailer tailer = Tailer.builder()
				.setFile( cellposeLogFile )
				.setTailerListener( new LoggerTailerListener( logger ) )
				.setDelayDuration( Duration.ofMillis( 200 ) )
				.setTailFromEnd( true )
				.get();

		final ExecutorService executors = Executors.newFixedThreadPool( nConcurrentTasks );
		final List< String > resultDirs = new ArrayList<>( nConcurrentTasks );
		List< Future< String > > results;
		try
		{
			results = executors.invokeAll( processes );
			for ( final Future< String > future : results )
			{
				resultDirs.add( future.get() );
			}
		}
		catch ( final InterruptedException | ExecutionException e )
		{
			errorMessage = baseErrorMessage + "Problem running "
					+ command
					+ ":\n" + e.getMessage() + '\n';
			e.printStackTrace();
			return false;
		}
		finally
		{
			tailer.close();
			logger.setStatus( "" );
			logger.setProgress( 1. );
		}

		/*
		 * Did we have a problem with independent tasks?
		 */

		for ( final CellposeTask task : processes )
		{
			if ( !task.isOk() )
			{
				return false;
			}
		}

		/*
		 * Get the result masks back.
		 */

		logger.log( "Reading " + command + " masks.\n" );
		final List< ImagePlus > masks = new ArrayList<>( imps.size() );
		for ( int t = 0; t < imps.size(); t++ )
		{
			final String name = nameGen.apply( ( long ) minT + t ) + "_cp_masks.png";

			// Try to find corresponding mask in any of the result dirs we got.
			ImagePlus tpImp = null;
			for ( final String tmpDir : resultDirs )
			{
				final String path = new File( tmpDir.toString(), name ).getAbsolutePath();
				tpImp = IJ.openImage( path );
				if ( null != tpImp )
				{
					// Found it. Convert it to 16-bit if we have to.
					if ( tpImp.getType() != ImagePlus.GRAY16 )
					{
						if ( nslices > 1 )
						{
							new StackConverter( tpImp ).convertToGray16();
						}
						else
						{
							new ImageConverter( tpImp ).convertToGray16();
						}
					}
					break;
				}
			}

			// Did we succeed?
			if ( null == tpImp )
			{
				logger.append( "Could not find results file for timepoint: " + name + '\n' );
				final ImagePlus blank = NewImage.createImage(
						"blank_" + t,
						imps.get( 0 ).getWidth(),
						imps.get( 0 ).getHeight(),
						nslices,
						16, // bitdepth
						NewImage.FILL_BLACK );
				masks.add( blank );
			}
			else
			{
				masks.add( tpImp );
			}
		}
		final Concatenator concatenator = new Concatenator();
		final ImagePlus output = concatenator.concatenateHyperstacks(
				masks.toArray( new ImagePlus[] {} ),
				img.getName() + "_" + command + "Output", false );

		// Copy calibration.
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		output.getCalibration().pixelWidth = calibration[ 0 ];
		output.getCalibration().pixelHeight = calibration[ 1 ];
		output.getCalibration().pixelDepth = calibration[ 2 ];
		output.setDimensions( 1, nslices, imps.size() );
		output.setOpenAsHyperStack( true );

		/*
		 * Run in the label detector.
		 */

		logger.log( "Converting masks to spots.\n" );
		final Settings labelImgSettings = new Settings( output );
		final LabelImageDetectorFactory< ? > labeImageDetectorFactory = new LabelImageDetectorFactory<>();
		final Map< String, Object > detectorSettings = labeImageDetectorFactory.getDefaultSettings();
		detectorSettings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		detectorSettings.put( KEY_SIMPLIFY_CONTOURS, cli.simplifyContours().getValue() );
		labelImgSettings.detectorFactory = labeImageDetectorFactory;
		labelImgSettings.detectorSettings = detectorSettings;

		final TrackMate labelImgTrackMate = new TrackMate( labelImgSettings );
		labelImgTrackMate.setNumThreads( numThreads );
		if ( !labelImgTrackMate.execDetection() )
		{
			errorMessage = baseErrorMessage + labelImgTrackMate.getErrorMessage();
			return false;
		}
		final SpotCollection tmpSpots = labelImgTrackMate.getModel().getSpots();

		/*
		 * Reposition spots with respect to the interval and time.
		 */
		final List< Spot > slist = new ArrayList<>();
		for ( final Spot spot : tmpSpots.iterable( false ) )
		{
			for ( int d = 0; d < interval.numDimensions() - 1; d++ )
			{
				final double pos = spot.getDoublePosition( d ) + interval.min( d ) * calibration[ d ];
				spot.putFeature( Spot.POSITION_FEATURES[ d ], Double.valueOf( pos ) );
			}
			// Shift in time.
			final int frame = spot.getFeature( Spot.FRAME ).intValue() + minT;
			spot.putFeature( Spot.POSITION_T, frame * frameInterval );
			spot.putFeature( Spot.FRAME, Double.valueOf( frame ) );
			slist.add( spot );
		}
		spots = SpotCollection.fromCollection( slist );

		/*
		 * End.
		 */

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
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
		for ( final CellposeTask task : processes )
		{
			task.cancel();
		}
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}

	// --- Multithreaded methods ---

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors() / 2;
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	// --- private classes ---

	final class CellposeTask implements Callable< String >
	{

		private Process process;

		private final AtomicBoolean ok;

		private final List< ImagePlus > imps;

		public CellposeTask( final List< ImagePlus > imps )
		{
			this.imps = imps;
			this.ok = new AtomicBoolean( true );
		}

		public boolean isOk()
		{
			return ok.get();
		}

		void cancel()
		{
			if ( process != null )
			{
				process.toHandle().descendants().forEach( ProcessHandle::destroyForcibly );
				process.destroyForcibly();
			}
		}

		@Override
		public String call() throws Exception
		{
			final String command = cli.getCommand();

			/*
			 * Prepare tmp dir.
			 */
			Path tmpDir = null;
			try
			{
				tmpDir = Files.createTempDirectory( "TrackMate-" + command + "_" );
				CLIUtils.recursiveDeleteOnShutdownHook( tmpDir );
			}
			catch ( final IOException e1 )
			{
				errorMessage = baseErrorMessage + "Could not create tmp dir to save and load images:\n" + e1.getMessage();
				ok.set( false );
				return null;
			}

			/*
			 * Save time-points as individual frames.
			 */

			logger.log( "Saving single time-points.\n" );
			// Careful, now time starts at 0, even if in the interval it is not
			// the case.
			for ( final ImagePlus imp : imps )
			{
				final String name = imp.getShortTitle() + ".tif";
				// If we are running an omnipose detector, just save the
				// segmentation channel as tmp image
				if ( cli instanceof OmniposeCLI )
				{
					final OmniposeCLI ocli = ( OmniposeCLI ) cli;
					final String cStr = ocli.segmentationChannel().getValue();
					final int c = Integer.parseInt( cStr );
					final ImagePlus chanImp = new Duplicator().run( imp, c, c, 0, 0, 0, 0 );
					IJ.saveAsTiff( chanImp, Paths.get( tmpDir.toString(), name ).toString() );
				}
				else
				{
					IJ.saveAsTiff( imp, Paths.get( tmpDir.toString(), name ).toString() );
				}
			}

			/*
			 * Run Cellpose.
			 */

			try
			{
				final List< String > cmd;
				synchronized ( cli )
				{
					/*
					 * In synchronized block so that we can safely generate the
					 * command line even if one instance of the cli is used in
					 * several threads.
					 */
					cli.imageFolder().set( tmpDir.toString() );
					cmd = CommandBuilder.build( ( CLIConfigurator ) cli );
				}
				logger.setStatus( "Running " + command );
				logger.log( "Running " + command + " with args:\n" );
				logger.log( String.join( " ", cmd ) );
				logger.log( "\n" );

				if ( cli instanceof OmniposeCLI )
				{
					final ProcessBuilder pbOmni = new ProcessBuilder( cmd );
					pbOmni.redirectErrorStream( true );
					// Env variables.
					final Map< String, String > env = new HashMap<>();
					final String condaRootPrefix = CLIUtils.getCondaRootPrefix();
					env.put( "MAMBA_ROOT_PREFIX", condaRootPrefix );
					env.put( "CONDA_ROOT_PREFIX", condaRootPrefix );
					pbOmni.environment().putAll( env );

					process = pbOmni.start();

					final BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
					final String pythonErrorOutput = reader.lines().collect( Collectors.joining() );
					if ( pythonErrorOutput.contains( "size mismatch for output.2.bias:" ) )
					{
						int nClasses;
						if ( pythonErrorOutput.contains( "copying a param with shape torch.Size([4]) from checkpoint" ) )
						{
							nClasses = 3;
							logger.log( "Regarding the model loaded, --nclasses argument should be set to " + String.valueOf( nClasses ) + "\n" );
						}
						else
						{
							nClasses = 4;
							logger.log( "Regarding the model loaded, --nclasses argument should be set to " + String.valueOf( nClasses ) + "\n" );
						}

						final List< String > cmd2;
						synchronized ( cli )
						{
							// Update the command line to set the nClasses
							final OmniposeCLI ocli = ( OmniposeCLI ) cli;
							ocli.nClasses().set( nClasses );

							// Regen command line with the updated nClasses
							cmd2 = CommandBuilder.build( ocli );
						}

						logger.log( "Re-running " + command + " with args:\n" );
						logger.log( String.join( " ", cmd2 ) );
						logger.log( "\n" );
						final ProcessBuilder updatedPbOmni = new ProcessBuilder( cmd2 );
						updatedPbOmni.redirectOutput( ProcessBuilder.Redirect.INHERIT );
						updatedPbOmni.redirectError( ProcessBuilder.Redirect.INHERIT );
						updatedPbOmni.environment().putAll( env );

						process = updatedPbOmni.start();
						process.waitFor();
					}
					else if ( pythonErrorOutput.contains( "pretrained model has incorrect path" ) )
					{
						logger.log( "Pretrained model has incorrect path \n" );
					}
				}
				else
				{
					final ProcessBuilder pb = new ProcessBuilder( cmd );
					// stdout → log file (tailed to TrackMate logger for live output).
					// stderr → pipe so we can capture it and show it in the error message.
					cellposeLogFile.getParentFile().mkdirs();
					pb.redirectOutput( ProcessBuilder.Redirect.appendTo( cellposeLogFile ) );
					pb.redirectError( ProcessBuilder.Redirect.PIPE );
					// Env variables.
					final Map< String, String > env = new HashMap<>();
					final String condaRootPrefix = CLIUtils.getCondaRootPrefix();
					env.put( "MAMBA_ROOT_PREFIX", condaRootPrefix );
					env.put( "CONDA_ROOT_PREFIX", condaRootPrefix );
					pb.environment().putAll( env );

					process = pb.start();
					// Drain stderr in a background thread to prevent buffer blocking.
					final StringBuilder stderrCapture = new StringBuilder();
					final Thread stderrThread = new Thread( () -> {
						try ( final BufferedReader reader = new BufferedReader(
								new InputStreamReader( process.getErrorStream() ) ) )
						{
							reader.lines().forEach( line -> stderrCapture.append( line ).append( '\n' ) );
						}
						catch ( final IOException ignored )
						{}
					}, command + "-stderr-reader" );
					stderrThread.setDaemon( true );
					stderrThread.start();

					final int exitCode = process.waitFor();
					stderrThread.join( 5000L );

					if ( exitCode != 0 )
					{
						final String stderr = stderrCapture.toString().trim();
						final String detail = stderr.isEmpty() ? "" : "\n" + stderr;
						logger.log( baseErrorMessage + command + " exited with code " + exitCode + detail + "\n" );
						errorMessage = baseErrorMessage + command + " process exited with code " + exitCode + detail;
						ok.set( false );
						return null;
					}
				}
			}
			catch ( final Exception e )
			{
				errorMessage = baseErrorMessage + "Problem running " + command + ":\n" + e.getMessage();
				e.printStackTrace();
				ok.set( false );
				return null;
			}
			finally
			{
				process = null;
			}
			return tmpDir.toString();
		}
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}
}

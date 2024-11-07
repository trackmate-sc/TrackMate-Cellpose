package fiji.plugin.trackmate.cellpose;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LabelImageDetectorFactory;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.omnipose.OmniposeSettings;
import fiji.plugin.trackmate.util.TMUtils;
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
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class CellposeDetector< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetector< T >, Cancelable, MultiThreaded
{

	private static final Function< Long, String > nameGen = ( frame ) -> String.format( "%d", frame );

	private final ImgPlus< T > img;

	private final Interval interval;

	private final Logger logger;

	private final String baseErrorMessage;

	private String errorMessage;

	private long processingTime;

	private SpotCollection spots;

	private String cancelReason;

	private boolean isCanceled;

	private final List< CellposeTask > processes = new ArrayList<>();

	private int numThreads;

	private final AbstractCellposeSettings cellposeSettings;

	private final File cellposeLogFile;

	public CellposeDetector(
			final ImgPlus< T > img,
			final Interval interval,
			final AbstractCellposeSettings cellposeSettings,
			final Logger logger )
	{
		this.img = img;
		this.interval = interval;
		this.cellposeSettings = cellposeSettings;
		this.logger = ( logger == null ) ? Logger.VOID_LOGGER : logger;
		this.cellposeLogFile = new File( new File( System.getProperty( "user.home" ), "." + cellposeSettings.getExecutableName() ), "run.log" );
		this.baseErrorMessage = "[" + cellposeSettings.getExecutableName() + "Detector] ";
	}

	@Override
	public boolean process()
	{
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

		final List< ImagePlus > imps = crop( img, interval, nameGen );

		final int nConcurrentTasks;
		/*
		 * We use multiprocessing ONLY if the user stated that they want to use
		 * the CPU and if we are on Mac. I tested multiprocessing on CPU under
		 * windows, and there is no benefit for Windows. But there is a strong
		 * speedup on Mac.
		 *
		 * On a PC with Windows, forcing Cellpose to run with the CPU: There is
		 * no benefit from splitting the load between 1,2, 10 or 20 processes.
		 * It seems like 1 Cellpose process can already use ALL the cores by
		 * itself and running several Cellpose processes concurrently does not
		 * lead to shorter processing time.
		 *
		 * For a source image 1024x502 over 92 time-points, 3 channels: - 1
		 * thread -> 24.4 min - 8 thread -> 4.1 min (there is not a x8 speedup
		 * factor, which is to be expected)
		 */
		if ( !cellposeSettings.useGPU && IJ.isMacintosh() )
			nConcurrentTasks = numThreads;
		else
			nConcurrentTasks = 1;

		final List< List< ImagePlus > > timepoints = new ArrayList<>( nConcurrentTasks );
		for ( int i = 0; i < nConcurrentTasks; i++ )
			timepoints.add( new ArrayList<>() );

		Iterator< List< ImagePlus > > it = timepoints.iterator();
		for ( int t = 0; t < imps.size(); t++ )
		{
			if ( !it.hasNext() )
				it = timepoints.iterator();
			it.next().add( imps.get( t ) );
		}

		/*
		 * Create tasks for each list of imps.
		 */

		processes.clear();
		for ( final List< ImagePlus > list : timepoints )
			processes.add( new CellposeTask( list ) );

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
				resultDirs.add( future.get() );
		}
		catch ( final InterruptedException | ExecutionException e )
		{
			errorMessage = baseErrorMessage + "Problem running "
					+ cellposeSettings.getExecutableName()
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
				return false;
		}

		/*
		 * Get the result masks back.
		 */

		logger.log( "Reading " + cellposeSettings.getExecutableName() + " masks.\n" );
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
							new StackConverter( tpImp ).convertToGray16();
						else
							new ImageConverter( tpImp ).convertToGray16();
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
				img.getName() + "_" + cellposeSettings.getExecutableName() + "Output", false );

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
		detectorSettings.put( KEY_SIMPLIFY_CONTOURS, cellposeSettings.simplifyContours );
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

	/**
	 * Add a hook to delete the content of given path when Fiji quits. Taken
	 * from https://stackoverflow.com/a/20280989/201698
	 *
	 * @param path
	 */
	protected static void recursiveDeleteOnShutdownHook( final Path path )
	{
		Runtime.getRuntime().addShutdownHook( new Thread( new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Files.walkFileTree( path, new SimpleFileVisitor< Path >()
					{
						@Override
						public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs ) throws IOException
						{
							Files.delete( file );
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory( final Path dir, final IOException e ) throws IOException
						{
							if ( e == null )
							{
								Files.delete( dir );
								return FileVisitResult.CONTINUE;
							}
							throw e;
						}
					} );
				}
				catch ( final IOException e )
				{
					throw new RuntimeException( "Failed to delete " + path, e );
				}
			}
		} ) );
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

	private static class LoggerTailerListener extends TailerListenerAdapter
	{
		private final Logger logger;

		private final static Pattern PERCENTAGE_PATTERN = Pattern.compile( ".+\\D(\\d+(?:\\.\\d+)?)%.+" );

		private final static Pattern INFO_PATTERN = Pattern.compile( ".+\\[INFO\\]\\s+(.+)" );

		public LoggerTailerListener( final Logger logger )
		{
			this.logger = logger;
		}

		@Override
		public void handle( final String line )
		{
			// Do we have percentage?
			final Matcher matcher = PERCENTAGE_PATTERN.matcher( line );
			if ( matcher.matches() )
			{
				final String percent = matcher.group( 1 );
				logger.setProgress( Double.valueOf( percent ) / 100. );
			}
			else
			{
				final Matcher matcher2 = INFO_PATTERN.matcher( line );
				if ( matcher2.matches() )
				{
					final String str = matcher2.group( 1 ).trim();
					if ( str.length() > 2 )
						logger.setStatus( str );
				}
			}
		}
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
			task.cancel();
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
				process.destroy();
		}

		@Override
		public String call() throws Exception
		{

			/*
			 * Prepare tmp dir.
			 */
			Path tmpDir = null;
			try
			{
				tmpDir = Files.createTempDirectory( "TrackMate-" + cellposeSettings.getExecutableName() + "_" );
				recursiveDeleteOnShutdownHook( tmpDir );
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
				if ( cellposeSettings instanceof OmniposeSettings )
				{
					final ImagePlus chanImp = new Duplicator().run( imp, cellposeSettings.chan, cellposeSettings.chan, 0, 0, 0, 0 );
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
				if ( cellposeSettings instanceof OmniposeSettings )
				{
					final List< String > cmdOmni = new ArrayList<>( cellposeSettings.toCmdLine( tmpDir.toString() ) );

					int nClasses = 2; // 2 by default in custom models
					cmdOmni.add( "--nclasses" );
					cmdOmni.add( String.valueOf( nClasses ) );

					logger.setStatus( "Running " + cellposeSettings.getExecutableName() );
					logger.log( "Running " + cellposeSettings.getExecutableName() + " with args:\n" );
					logger.log( String.join( " ", cmdOmni ) );
					logger.log( "\n" );

					final ProcessBuilder pbOmni = new ProcessBuilder( cmdOmni );
					pbOmni.redirectErrorStream( true );
					process = pbOmni.start();

					final BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
					final String pythonErrorOutput = reader.lines().collect( Collectors.joining() );
					if ( pythonErrorOutput.contains( "size mismatch for output.2.bias:" ) )
					{
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

						cmdOmni.remove( cmdOmni.size() - 1 );
						cmdOmni.add( String.valueOf( nClasses ) );

						logger.log( "Running " + cellposeSettings.getExecutableName() + " with args:\n" );
						logger.log( String.join( " ", cmdOmni ) );
						logger.log( "\n" );
						final ProcessBuilder updatedPbOmni = new ProcessBuilder( cmdOmni );
						updatedPbOmni.redirectOutput( ProcessBuilder.Redirect.INHERIT );
						updatedPbOmni.redirectError( ProcessBuilder.Redirect.INHERIT );

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
					final List< String > cmd = cellposeSettings.toCmdLine( tmpDir.toString() );
					logger.setStatus( "Running " + cellposeSettings.getExecutableName() );
					logger.log( "Running " + cellposeSettings.getExecutableName() + " with args:\n" );
					logger.log( String.join( " ", cmd ) );
					logger.log( "\n" );
					final ProcessBuilder pb = new ProcessBuilder( cmd );
					pb.redirectOutput( ProcessBuilder.Redirect.INHERIT );
					pb.redirectError( ProcessBuilder.Redirect.INHERIT );

					process = pb.start();
					process.waitFor();
				}
			}
			catch ( final IOException e )
			{
				final String msg = e.getMessage();
				if ( msg.matches( ".+error=13.+" ) )
				{
					errorMessage = baseErrorMessage + "Problem running " + cellposeSettings.getExecutableName() + ":\n"
							+ "The executable does not have the file permission to run.\n"
							+ "Please see https://github.com/MouseLand/cellpose#run-cellpose-without-local-python-installation for more information.\n";
				}
				else
				{
					errorMessage = baseErrorMessage + "Problem running " + cellposeSettings.getExecutableName() + ":\n" + e.getMessage();
				}
				e.printStackTrace();
				ok.set( false );
				return null;
			}
			catch ( final Exception e )
			{
				errorMessage = baseErrorMessage + "Problem running " + cellposeSettings.getExecutableName() + ":\n" + e.getMessage();
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

	private static final < T extends RealType< T > & NativeType< T > > List< ImagePlus > crop( final ImgPlus< T > img, final Interval interval, final Function< Long, String > nameGen )
	{
		final int zIndex = img.dimensionIndex( Axes.Z );
		final int cIndex = img.dimensionIndex( Axes.CHANNEL );
		final Interval cropInterval;
		if ( zIndex < 0 )
		{
			// 2D
			if ( cIndex < 0 )
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ),
						interval.max( 0 ), interval.max( 1 ) );
			else
				// Include all channels
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ), img.min( cIndex ),
						interval.max( 0 ), interval.max( 1 ), img.max( cIndex ) );
		}
		else
		{
			if ( cIndex < 0 )
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ), interval.min( 2 ),
						interval.max( 0 ), interval.max( 1 ), interval.max( 2 ) );
			else
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ), interval.min( 2 ), img.min( cIndex ),
						interval.max( 0 ), interval.max( 1 ), interval.max( 2 ), img.max( cIndex ) );
		}

		final List< ImagePlus > imps = new ArrayList<>();
		final int timeIndex = img.dimensionIndex( Axes.TIME );
		if ( timeIndex < 0 )
		{
			// No time.
			final IntervalView< T > crop = Views.interval( img, cropInterval );
			final String name = nameGen.apply( 0l ) + ".tif";
			imps.add( ImageJFunctions.wrap( crop, name ) );
		}
		else
		{
			// In the interval, time is always the last.
			final long minT = interval.min( interval.numDimensions() - 1 );
			final long maxT = interval.max( interval.numDimensions() - 1 );
			for ( long t = minT; t <= maxT; t++ )
			{
				final ImgPlus< T > tpTCZ = ImgPlusViews.hyperSlice( img, timeIndex, t );

				// Put if necessary the channel axis as the last one (CellPose
				// format)
				final int chanDim = tpTCZ.dimensionIndex( Axes.CHANNEL );
				ImgPlus< T > tp = tpTCZ;
				if ( chanDim > 1 )
				{
					tp = ImgPlusViews.moveAxis( tpTCZ, chanDim, tpTCZ.numDimensions() - 1 );
				}
				// possibly 2D or 3D with or without channel.
				final IntervalView< T > crop = Views.interval( tp, cropInterval );
				final String name = nameGen.apply( t ) + ".tif";
				imps.add( ImageJFunctions.wrap( crop, name ) );
			}
		}
		return imps;
	}
}

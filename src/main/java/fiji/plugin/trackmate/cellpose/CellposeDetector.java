package fiji.plugin.trackmate.cellpose;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LabeImageDetectorFactory;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class CellposeDetector< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetector< T >
{
	private final static String BASE_ERROR_MESSAGE = "CellposeDetector: ";

	protected final ImgPlus< T > img;

	protected final Interval interval;

	private final CellposeSettings cellposeSettings;

	private final Logger logger;

	protected String baseErrorMessage;

	protected String errorMessage;

	protected long processingTime;

	protected SpotCollection spots;

	public CellposeDetector(
			final ImgPlus< T > img,
			final Interval interval,
			final CellposeSettings cellposeSettings,
			final Logger logger )
	{
		this.img = img;
		this.interval = interval;
		this.cellposeSettings = cellposeSettings;
		this.logger = logger;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Prepare tmp dir.
		 */
		Path tmpDir = null;
		try
		{
			tmpDir = Files.createTempDirectory( "TrackMate-Cellpose_" );
			recursiveDeleteOnShutdownHook( tmpDir );
		}
		catch ( final IOException e1 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Could not create tmp dir to save and load images:\n" + e1.getMessage();
			return false;
		}

		/*
		 * Time & Z.
		 */

		final int zIndex = img.dimensionIndex( Axes.Z );
		final int nZSlices = ( zIndex < 0 ) ? 1 : ( int ) img.dimension( zIndex );

		/*
		 * Interval.
		 */

		final int timeIndex = img.dimensionIndex( Axes.TIME );
		final int minT;
		final int maxT;
		final int nTimepoints;
		if ( timeIndex < 0 )
		{
			minT = 0;
			maxT = 0;
			nTimepoints = 1;
		}
		else
		{
			// Min and Max timepoint are in the last dimension of the interval.
			minT = Math.max( 0, ( int ) interval.min( timeIndex ) );
			maxT = Math.min( ( int ) img.max( timeIndex ), ( int ) interval.max( timeIndex ) );
			nTimepoints = maxT - minT + 1;

		}
		// Remove time dimension.
		final long[] minBounds = new long[ interval.numDimensions() - 1 ];
		final long[] maxBounds = new long[ interval.numDimensions() - 1 ];
		int d2 = 0;
		for ( int d = 0; d < interval.numDimensions(); d++ )
		{
			if ( d == timeIndex )
				continue;
			minBounds[ d2 ] = interval.min( d );
			maxBounds[ d2 ] = interval.max( d );
			d2++;
		}
		final Interval spatialInterval;
		spatialInterval = new FinalInterval( minBounds, maxBounds );

		/*
		 * Save time-points as individual frames.
		 */

		final Function< Integer, String > nameGen = ( frame ) -> String.format( "%d", frame );
		if ( timeIndex < 0 )
		{
			final String name = nameGen.apply( 0 ) + ".tif";
			final IntervalView< T > crop = Views.interval( img, spatialInterval );
			final ImagePlus tpImp = ImageJFunctions.wrap( crop, name );
			IJ.saveAsTiff( tpImp, Paths.get( tmpDir.toString(), name ).toString() );
		}
		else
		{
			// Many time-points.
			for ( int tp = minT; tp <= maxT; tp++ )
			{
				final String name = nameGen.apply( tp ) + ".tif";
				final ImgPlus< T > tpImg = CellposeUtils.hyperSlice( img, tp );
				final IntervalView< T > crop = Views.interval( tpImg, spatialInterval );
				final ImagePlus tpImp = ImageJFunctions.wrap( crop, name );
				IJ.saveAsTiff( tpImp, Paths.get( tmpDir.toString(), name ).toString() );
			}
		}

		/*
		 * Run Cellpose.
		 */

		try
		{
			final List< String > cmd = cellposeSettings.toCmdLine( tmpDir.toString() );
			final ProcessBuilder pb = new ProcessBuilder( cmd );
			pb.redirectOutput( ProcessBuilder.Redirect.INHERIT );
			pb.redirectError( ProcessBuilder.Redirect.INHERIT );
			final Process p = pb.start();
			CellposeUtils.redirectToLogger( p.getErrorStream(), logger );
			CellposeUtils.redirectToErrLogger( p.getInputStream(), logger );
			p.waitFor();
		}
		catch ( final Exception e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Problem running Cellpose:\n" + e.getMessage();
			e.printStackTrace();
			return false;
		}

		/*
		 * Get the result masks back.
		 */

		final ImagePlus output;
		if ( timeIndex < 0 )
		{
			final String name = nameGen.apply( 0 ) + "_cp_masks.png";
			final String path = new File( tmpDir.toString(), name ).getAbsolutePath();
			output = IJ.openImage( path );
			if ( null == output )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Could not find results file for: " + name;
				return false;
			}
		}
		else
		{
			final List< ImagePlus > masks = new ArrayList<>( nTimepoints );
			for ( int tp = minT; tp <= maxT; tp++ )
			{
				final String name = nameGen.apply( tp ) + "_cp_masks.png";
				final String path = new File( tmpDir.toString(), name ).getAbsolutePath();
				final ImagePlus tpImp = IJ.openImage( path );
				if ( null == tpImp )
				{
					errorMessage = BASE_ERROR_MESSAGE + "Could not find results file for timepoint: " + name;
					return false;
				}
				masks.add( tpImp );
			}
			final Concatenator concatenator = new Concatenator();
			output = concatenator.concatenateHyperstacks(
					masks.toArray( new ImagePlus[] {} ),
					img.getName() + "_CellposeOutput", false );
		}

		// Copy calibration.
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		output.getCalibration().pixelWidth = calibration[ 0 ];
		output.getCalibration().pixelHeight = calibration[ 1 ];
		output.getCalibration().pixelDepth = calibration[ 2 ];
		output.setDimensions( 1, nZSlices, nTimepoints );
		output.setOpenAsHyperStack( true );

		/*
		 * Run in the label detector.
		 */

		final Settings labelImgSettings = new Settings( output );
		final LabeImageDetectorFactory< ? > labeImageDetectorFactory = new LabeImageDetectorFactory<>();
		final Map< String, Object > detectorSettings = labeImageDetectorFactory.getDefaultSettings();
		detectorSettings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		detectorSettings.put( KEY_SIMPLIFY_CONTOURS, cellposeSettings.simplifyContours );
		labelImgSettings.detectorFactory = labeImageDetectorFactory;
		labelImgSettings.detectorSettings = detectorSettings;

		final TrackMate labelImgTrackMate = new TrackMate( labelImgSettings );
		if ( !labelImgTrackMate.execDetection() )
		{
			errorMessage = BASE_ERROR_MESSAGE + labelImgTrackMate.getErrorMessage();
			return false;
		}
		this.spots = labelImgTrackMate.getModel().getSpots();

		/*
		 * Reposition spots with respect to the interval.
		 */

		for ( final Spot spot : spots.iterable( false ) )
		{
			for ( int d = 0; d < interval.numDimensions() - 1; d++ )
			{
				final double pos = spot.getDoublePosition( d ) + interval.min( d ) * calibration[ d ];
				spot.putFeature( Spot.POSITION_FEATURES[ d ], Double.valueOf( pos ) );
			}
		}

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

	/**
	 * Add a hook to delete the content of given path when Fiji quits. Taken
	 * from https://stackoverflow.com/a/20280989/201698
	 * 
	 * @param path
	 */
	private static void recursiveDeleteOnShutdownHook( final Path path )
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
}

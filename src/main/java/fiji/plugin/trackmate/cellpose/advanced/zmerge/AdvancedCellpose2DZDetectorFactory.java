package fiji.plugin.trackmate.cellpose.advanced.zmerge;

import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.DEFAULT_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.DEFAULT_FLOW_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_FLOW_THRESHOLD;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SMOOTHING_SCALE;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_IOU_CALCULATION;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_MIN_IOU;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_SCALE_FACTOR;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory;
import fiji.plugin.trackmate.cellpose.zmerge.Cellpose2DZDetectorFactory;
import fiji.plugin.trackmate.detection.Process2DZ;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW - 3.3 )
public class AdvancedCellpose2DZDetectorFactory< T extends RealType< T > & NativeType< T > > extends Cellpose2DZDetectorFactory< T >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "CELLPOSE_2DZ_ADVANCED_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose 2D+Z advanced detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on cellpose to detect the objects in 3D images, "
			+ "but does it in a different manner that what cellpose normally do "
			+ "with 3D images. "
			+ "<p> "
			+ "It processes each 2D slice of the source image with cellpose in 2D and "
			+ "merges the resulting overlapping 2D contours in one 3D mesh per object."
			+ "It only works for 3D images."
			+ "<p> "
			+ "It is identical to the Cellpose 2D+Z detector, except that it allows to "
			+ "tweak the 'flow threshold' and 'cell probability threshold' parameters of the "
			+ "cellpose algorithm."
			+ "</html>";

	private ImgPlus< T > img;

	private Map< String, Object > settings;

	protected String errorMessage;

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public AdvancedCellpose2DZDetectorConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new AdvancedCellpose2DZDetectorConfigurationPanel( settings, model );
	}

	@Override
	public AdvancedCellpose2DZDetectorFactory< T > copy()
	{
		return new AdvancedCellpose2DZDetectorFactory<>();
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > s = super.getDefaultSettings();
		s.put( KEY_FLOW_THRESHOLD, DEFAULT_FLOW_THRESHOLD );
		s.put( KEY_CELL_PROB_THRESHOLD, DEFAULT_CELL_PROB_THRESHOLD );
		return s;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		final Map< String, Object > cellposeSettings = new HashMap<>( settings );
		cellposeSettings.remove( KEY_FLOW_THRESHOLD );
		cellposeSettings.remove( KEY_CELL_PROB_THRESHOLD );
		boolean ok = super.checkSettings( cellposeSettings );
		if ( !ok )
		{
			errorMessage = super.getErrorMessage();
			return false;
		}

		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_FLOW_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELL_PROB_THRESHOLD, Double.class, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();

		return ok;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		boolean ok = super.marshall( settings, element );
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & writeAttribute( settings, element, KEY_FLOW_THRESHOLD, Double.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_CELL_PROB_THRESHOLD, Double.class, errorHolder );
		if ( !ok )
			errorMessage = "[" + getKey() + "] " + errorHolder.toString();

		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		boolean ok = super.unmarshall( element, settings );
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & readDoubleAttribute( element, settings, KEY_FLOW_THRESHOLD, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_CELL_PROB_THRESHOLD, errorHolder );
		if ( !ok )
		{
			errorMessage = "[" + getKey() + "] " + errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		// Configure 2D detector.
		final AdvancedCellposeDetectorFactory< T > cellposeFactory = new AdvancedCellposeDetectorFactory<>();
		final Map< String, Object > cellposeSettings = new HashMap<>( settings );
		cellposeSettings.remove( KEY_IOU_CALCULATION );
		cellposeSettings.remove( KEY_MIN_IOU );
		cellposeSettings.remove( KEY_SCALE_FACTOR );

		// Configure Overlap tracker as a 2D merger.
		final OverlapTrackerFactory trackerFactory = new OverlapTrackerFactory();
		final Map< String, Object > trackerSettings = trackerFactory.getDefaultSettings();
		trackerSettings.put( OverlapTrackerFactory.KEY_IOU_CALCULATION, OverlapTrackerFactory.PRECISE_CALCULATION );
		trackerSettings.put( OverlapTrackerFactory.KEY_MIN_IOU, settings.get( KEY_MIN_IOU ) );
		trackerSettings.put( OverlapTrackerFactory.KEY_SCALE_FACTOR, settings.get( KEY_SCALE_FACTOR ) );

		final Settings s = new Settings();
		s.detectorFactory = cellposeFactory;
		s.detectorSettings = cellposeSettings;
		s.trackerFactory = trackerFactory;
		s.trackerSettings = trackerSettings;

		final int timeDim = img.dimensionIndex( Axes.TIME );
		final ImgPlus< T > imgT = timeDim < 0 ? img : ImgPlusViews.hyperSlice( img, timeDim, frame );
		final double smoothingScale = ( ( Number ) settings.get( KEY_SMOOTHING_SCALE ) ).doubleValue();

		final double[] calibration = TMUtils.getSpatialCalibration( img );
		return new Process2DZ<>( imgT, interval, calibration, s, true, smoothingScale );
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}
}

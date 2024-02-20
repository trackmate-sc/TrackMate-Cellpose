/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2023 TrackMate developers.
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
package fiji.plugin.trackmate.cellpose.advanced;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;
import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readStringAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.CellposeDetector;
import fiji.plugin.trackmate.cellpose.CellposeDetectorFactory;
import fiji.plugin.trackmate.cellpose.CellposeSettings.PretrainedModelCellpose;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.util.TMUtils;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW - 1. )
public class AdvancedCellposeDetectorFactory< T extends RealType< T > & NativeType< T > > extends CellposeDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/**
	 * The key to the parameter that store the flow threshold value. From cellpose docs:
	 * <p>
	 * Note there is nothing keeping the neural network from predicting horizontal and
	 * vertical flows that do not correspond to any real shapes at all. In practice,
	 * most predicted flows are consistent with real shapes, because the network was only
	 * trained on image flows that are consistent with real shapes, but sometimes when the
	 * network is uncertain it may output inconsistent flows. To check that the recovered
	 * shapes after the flow dynamics step are consistent with real ROIs, we recompute the
	 * flow gradients for these putative predicted ROIs, and compute the mean squared error
	 * between them and the flows predicted by the network.
	 * <p>
	 * The flow_threshold parameter is the maximum allowed error of the flows for each mask.
	 * The default is flow_threshold=0.4. Increase this threshold if cellpose is not returning
	 * as many ROIs as you’d expect. Similarly, decrease this threshold if cellpose is returning
	 * too many ill-shaped ROIs.
	 */
	public static final String KEY_FLOW_THRESHOLD = "FLOW_THRESHOLD";

	public static final Double DEFAULT_FLOW_THRESHOLD = Double.valueOf( 0.4 );

	/**
	 * The key to the parameter that store the cell probability threshold value.
	 * From cellpose docs:
	 * <p>
	 * The network predicts 3 outputs: flows in X, flows in Y, and cell “probability”.
	 * The predictions the network makes of the probability are the inputs to a sigmoid
	 * centered at zero (1 / (1 + e^-x)), so they vary from around -6 to +6. The pixels
	 * greater than the cellprob_threshold are used to run dynamics and determine ROIs.
	 * The default is cellprob_threshold=0.0. Decrease this threshold if cellpose is not
	 * returning as many ROIs as you’d expect. Similarly, increase this threshold if
	 * cellpose is returning too ROIs particularly from dim areas.
	 */
	public static final String KEY_CELL_PROB_THRESHOLD = "CELL_PROB_THRESHOLD";

	public static final Double DEFAULT_CELL_PROB_THRESHOLD = Double.valueOf( 0. );
        
        /**
	 * The key to the parameter that store the minimum size to keep masks.
	 * Used only if do_3D mode or 2D+Z and stitch_threshold > 0
         * From cellpose docs:
	 * <p>
	 * Minimum number of pixels per mask, can turn off with -1. 
	 */
	public static final String KEY_CELL_MIN_SIZE = "CELL_MIN_SIZE";

	public static final Double DEFAULT_CELL_MIN_SIZE = Double.valueOf( 15. );
      
        /**
	 * The key to the parameter that store the resampling option.
	 * From cellpose docs:
	 * <p>
	 * The cellpose network is run on your rescaled image – where the rescaling factor is determined by the diameter you input (or determined automatically as above). 
         * For instance, if you have an image with 60 pixel diameter cells, the rescaling factor is 30./60. = 0.5. 
         * After determining the flows (dX, dY, cellprob), the model runs the dynamics. 
         * The dynamics can be run at the rescaled size (resample=False), or the dynamics can be run on the resampled, interpolated flows at the true image size (resample=True). 
         * resample=True will create smoother ROIs when the cells are large but will be slower in case; 
         * resample=False will find more ROIs when the cells are small but will be slower in this case. 
         * By default in versions >=1.0 resample=True.
	 */
        
        public static final Boolean DEFAULT_RESAMPLE = true;
        
        public static final String KEY_RESAMPLE = "RESAMPLE";

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "CELLPOSE_ADVANCED_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose advanced detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on cellpose to detect objects."
			+ "<p>" + "It is identical to the Cellpose detector, except that it allows to "
			+ "tweak the 'flow threshold' and 'cell probability threshold' parameters of the "
			+ "cellpose algorithm."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the Cellpose paper: "
			+ "<a href=\"https://doi.org/10.1038/s41592-020-01018-x\">Stringer, C., Wang, T., Michaelos, M. et al. "
			+ "Cellpose: a generalist algorithm for cellular segmentation. "
			+ "Nat Methods 18, 100–106 (2021)</a>"
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"https://imagej.net/plugins/trackmate/trackmate-advanced-cellpose\">on the ImageJ Wiki</a>."
			+ "</html>";

	/*
	 * METHODS
	 */

	@Override
	public SpotGlobalDetector< T > getDetector( final Interval interval )
	{
		// Base settings.

		final String cellposePythonPath = ( String ) settings.get( KEY_CELLPOSE_PYTHON_FILEPATH );
		final PretrainedModelCellpose model = ( PretrainedModelCellpose ) settings.get( KEY_CELLPOSE_MODEL );
		final String customModelPath = ( String ) settings.get( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH );
		final boolean simplifyContours = ( boolean ) settings.get( KEY_SIMPLIFY_CONTOURS );
		final boolean useGPU = ( boolean ) settings.get( KEY_USE_GPU );

		// Channels are 0-based (0: grayscale, then R & G & B).
		final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL );
		final int channel2 = ( Integer ) settings.get( KEY_OPTIONAL_CHANNEL_2 );

		// Convert to diameter in pixels.
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final double diameter = ( double ) settings.get( KEY_CELL_DIAMETER ) / calibration[ 0 ];

		// Advanced settings.

		final double flowThreshold = ( Double ) settings.get( KEY_FLOW_THRESHOLD );
		final double cellProbThreshold = ( Double ) settings.get( KEY_CELL_PROB_THRESHOLD );        
		final boolean resample = ( Boolean ) settings.get( KEY_RESAMPLE );
                // min size: get rid of masks with few pixels in CP (but done on original image size, so possible bias of anisotropy)       
                final double cellMinSize = ( double ) settings.get( KEY_CELL_MIN_SIZE ); // in pixels
                
		final AdvancedCellposeSettings cellposeSettings = AdvancedCellposeSettings
				.create()
				.cellposePythonPath( cellposePythonPath )
				.customModel( customModelPath )
				.model( model )
				.channel1( channel )
				.channel2( channel2 )
				.diameter( diameter )
				.useGPU( useGPU )
				.simplifyContours( simplifyContours )
				.flowThreshold( flowThreshold )
				.cellProbThreshold( cellProbThreshold )
				.cellMinSize( cellMinSize )
//                                .do2DZ( do2DZ )
//                                .iouThreshold( iouThreshold )
                                .resample(resample)
				.get();

		// Logger.
		final Logger logger = ( Logger ) settings.get( KEY_LOGGER );
		final CellposeDetector< T > detector = new CellposeDetector<>( img, interval, cellposeSettings, logger );
		return detector;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		if ( !super.marshall( settings, element ) )
			return false;

		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = writeAttribute( settings, element, KEY_FLOW_THRESHOLD, Double.class, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_CELL_PROB_THRESHOLD, Double.class, errorHolder );
                ok = ok && writeAttribute( settings, element, KEY_CELL_MIN_SIZE, Double.class, errorHolder );
//                ok = ok && writeAttribute( settings, element, KEY_DO2DZ, Boolean.class, errorHolder );
//                ok = ok && writeAttribute( settings, element, KEY_IOU_THRESHOLD, Double.class, errorHolder );
                ok = ok && writeAttribute( settings, element, KEY_RESAMPLE, Boolean.class, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok && readStringAttribute( element, settings, KEY_CELLPOSE_PYTHON_FILEPATH, errorHolder );
		ok = ok && readStringAttribute( element, settings, KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH, errorHolder );
		ok = ok && readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		ok = ok && readIntegerAttribute( element, settings, KEY_OPTIONAL_CHANNEL_2, errorHolder );
		ok = ok && readDoubleAttribute( element, settings, KEY_CELL_DIAMETER, errorHolder );
		ok = ok && readBooleanAttribute( element, settings, KEY_USE_GPU, errorHolder );
		ok = ok && readBooleanAttribute( element, settings, KEY_SIMPLIFY_CONTOURS, errorHolder );
		ok = ok && readDoubleAttribute( element, settings, KEY_FLOW_THRESHOLD, errorHolder );
		ok = ok && readDoubleAttribute( element, settings, KEY_CELL_PROB_THRESHOLD, errorHolder );
		ok = ok && readDoubleAttribute( element, settings, KEY_CELL_MIN_SIZE, errorHolder );
//                ok = ok && readBooleanAttribute( element, settings, KEY_DO2DZ, errorHolder );
//                ok = ok && readDoubleAttribute( element, settings, KEY_IOU_THRESHOLD, errorHolder );
                ok = ok && readBooleanAttribute( element, settings, KEY_RESAMPLE, errorHolder );

		// Read model.
		final String str = element.getAttributeValue( KEY_CELLPOSE_MODEL );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + KEY_CELLPOSE_MODEL + " could not be found in XML element.\n" );
			ok = false;
		}
		settings.put( KEY_CELLPOSE_MODEL, PretrainedModelCellpose.valueOf( str ) );

		return checkSettings( settings );
	}

	@Override
	public AdvancedCellposeDetectorConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new AdvancedCellposeDetectorConfigurationPanel( settings, model );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = super.getDefaultSettings();
		settings.put( KEY_FLOW_THRESHOLD, DEFAULT_FLOW_THRESHOLD );
		settings.put( KEY_CELL_PROB_THRESHOLD, DEFAULT_CELL_PROB_THRESHOLD );
                settings.put( KEY_CELL_MIN_SIZE, DEFAULT_CELL_MIN_SIZE );
//                settings.put( KEY_DO2DZ, DEFAULT_DO2DZ );
//                settings.put( KEY_IOU_THRESHOLD, DEFAULT_IOU_THRESHOLD );
                settings.put( KEY_RESAMPLE, DEFAULT_RESAMPLE );
		return settings;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_CELLPOSE_PYTHON_FILEPATH, String.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH, String.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELLPOSE_MODEL, PretrainedModelCellpose.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_OPTIONAL_CHANNEL_2, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELL_DIAMETER, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_USE_GPU, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_SIMPLIFY_CONTOURS, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_FLOW_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELL_PROB_THRESHOLD, Double.class, errorHolder );
                ok = ok & checkParameter( settings, KEY_CELL_MIN_SIZE, Double.class, errorHolder );
//                ok = ok & checkParameter( settings, KEY_DO2DZ, Boolean.class, errorHolder );
//                ok = ok & checkParameter( settings, KEY_IOU_THRESHOLD, Double.class, errorHolder );
                ok = ok & checkParameter( settings, KEY_RESAMPLE, Boolean.class, errorHolder );
//		ok = ok & checkOptionalParameter( settings, KEY_SMOOTHING_SCALE, Double.class, errorHolder );

		// If we have a logger, test it is of the right class.
		final Object loggerObj = settings.get( KEY_LOGGER );
		if ( loggerObj != null && !Logger.class.isInstance( loggerObj ) )
		{
			errorHolder.append( "Value for parameter " + KEY_LOGGER + " is not of the right class. "
					+ "Expected " + Logger.class.getName() + ", got " + loggerObj.getClass().getName() + ".\n" );
			ok = false;
		}

		final List< String > mandatoryKeys = Arrays.asList(
				KEY_CELLPOSE_PYTHON_FILEPATH,
				KEY_CELLPOSE_MODEL,
				KEY_TARGET_CHANNEL,
				KEY_OPTIONAL_CHANNEL_2,
				KEY_CELL_DIAMETER,
				KEY_USE_GPU,
				KEY_SIMPLIFY_CONTOURS );
		final List< String > optionalKeys = Arrays.asList(
				KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH,
				KEY_LOGGER,
				KEY_FLOW_THRESHOLD,
				KEY_CELL_PROB_THRESHOLD,
                                KEY_CELL_MIN_SIZE,
//                                KEY_DO2DZ,
//                                KEY_IOU_THRESHOLD,
				KEY_RESAMPLE );
//				KEY_SMOOTHING_SCALE );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();

		// Extra test to make sure we can read the classifier file.
		if ( ok )
		{
			final Object obj = settings.get( KEY_CELLPOSE_PYTHON_FILEPATH );
			if ( obj == null )
			{
				errorMessage = "The path to the Cellpose python executable is not set.";
				return false;
			}

			if ( !IOUtils.canReadFile( ( String ) obj, errorHolder ) )
			{
				errorMessage = "Problem with Cellpose python executable: " + errorHolder.toString();
				return false;
			}
		}
		return ok;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
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
	public boolean has2Dsegmentation()
	{
		return true;
	}

	@Override
	public SpotDetectorFactoryBase< T > copy()
	{
		return new AdvancedCellposeDetectorFactory<>();
	}
}

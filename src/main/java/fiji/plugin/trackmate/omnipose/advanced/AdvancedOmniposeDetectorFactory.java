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
package fiji.plugin.trackmate.omnipose.advanced;

import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.DEFAULT_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.DEFAULT_FLOW_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_FLOW_THRESHOLD;
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
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.omnipose.OmniposeDetectorFactory;
import fiji.plugin.trackmate.omnipose.OmniposeSettings.PretrainedModelOmnipose;
import fiji.plugin.trackmate.util.TMUtils;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW - 1. )
public class AdvancedOmniposeDetectorFactory< T extends RealType< T > & NativeType< T > > extends OmniposeDetectorFactory< T >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "OMNIPOSE_ADVANCED_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Omnipose advanced detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on omnipose to detect objects."
			+ "<p>" + "It is identical to the Omnipose detector, except that it allows to "
			+ "tweak the 'flow threshold' and 'cell probability threshold' parameters of the "
			+ "algorithm."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the omnipose paper: <a href=\"https://doi.org/10.1038/s41592-022-01639-4\">Cutler, Kevin J., et al., "
			+ "'Omnipose: A High-Precision Morphology-Independent Solution for Bacterial Cell Segmentation.' "
			+ "Nature Methods 19, no. 11 (November 2022): 1438–48.</a>"
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"https://imagej.net/plugins/trackmate/trackmate-advanced-omnipose\">on the ImageJ Wiki</a>."
			+ "</html>";

         /**
	 * The key to the parameter that store the output number of classes. 
	 */
        public static final String KEY_NB_CLASSES = "NB_CLASSES";

	public static final Integer DEFAULT_NB_CLASSES = Integer.valueOf( 0 );

        
	/*
	 * METHODS
	 */

	@Override
	public SpotGlobalDetector< T > getDetector( final Interval interval )
	{
		// Base settings.

		final String omniposePythonPath = ( String ) settings.get( KEY_OMNIPOSE_PYTHON_FILEPATH );
		final PretrainedModelOmnipose model = ( PretrainedModelOmnipose ) settings.get( KEY_OMNIPOSE_MODEL );
		final String customModelPath = ( String ) settings.get( KEY_OMNIPOSE_CUSTOM_MODEL_FILEPATH );
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
                final int nbClasses = (Integer) settings.get( KEY_NB_CLASSES );

		final AdvancedOmniposeSettings cellposeSettings = AdvancedOmniposeSettings
				.create()
				.omniposePythonPath( omniposePythonPath )
				.customModel( customModelPath )
				.model( model )
				.channel1( channel )
				.channel2( channel2 )
				.diameter( diameter )
				.useGPU( useGPU )
				.simplifyContours( simplifyContours )
				.flowThreshold( flowThreshold )
				.cellProbThreshold( cellProbThreshold )
                                .nbClasses(nbClasses)
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
                ok = ok && writeAttribute( settings, element, KEY_NB_CLASSES, Integer.class, errorHolder );
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
		ok = ok && readStringAttribute( element, settings, KEY_OMNIPOSE_PYTHON_FILEPATH, errorHolder );
		ok = ok && readStringAttribute( element, settings, KEY_OMNIPOSE_CUSTOM_MODEL_FILEPATH, errorHolder );
		ok = ok && readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		ok = ok && readIntegerAttribute( element, settings, KEY_OPTIONAL_CHANNEL_2, errorHolder );
		ok = ok && readDoubleAttribute( element, settings, KEY_CELL_DIAMETER, errorHolder );
		ok = ok && readBooleanAttribute( element, settings, KEY_USE_GPU, errorHolder );
		ok = ok && readBooleanAttribute( element, settings, KEY_SIMPLIFY_CONTOURS, errorHolder );
		ok = ok && readDoubleAttribute( element, settings, KEY_FLOW_THRESHOLD, errorHolder );
		ok = ok && readDoubleAttribute( element, settings, KEY_CELL_PROB_THRESHOLD, errorHolder );
                ok = ok && readDoubleAttribute( element, settings, KEY_NB_CLASSES, errorHolder );

		// Read model.
		final String str = element.getAttributeValue( KEY_OMNIPOSE_MODEL );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + KEY_OMNIPOSE_MODEL + " could not be found in XML element.\n" );
			ok = false;
		}
		settings.put( KEY_OMNIPOSE_MODEL, PretrainedModelOmnipose.valueOf( str ) );

		return checkSettings( settings );
	}

	@Override
	public AdvancedOmniposeDetectorConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new AdvancedOmniposeDetectorConfigurationPanel( settings, model );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = super.getDefaultSettings();
		settings.put( KEY_FLOW_THRESHOLD, DEFAULT_FLOW_THRESHOLD );
		settings.put( KEY_CELL_PROB_THRESHOLD, DEFAULT_CELL_PROB_THRESHOLD );
                settings.put( KEY_NB_CLASSES, DEFAULT_NB_CLASSES );
		return settings;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_OMNIPOSE_PYTHON_FILEPATH, String.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_OMNIPOSE_CUSTOM_MODEL_FILEPATH, String.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_OMNIPOSE_MODEL, PretrainedModelOmnipose.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_OPTIONAL_CHANNEL_2, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELL_DIAMETER, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_USE_GPU, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_SIMPLIFY_CONTOURS, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_FLOW_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELL_PROB_THRESHOLD, Double.class, errorHolder );
                ok = ok & checkParameter( settings, KEY_NB_CLASSES, Integer.class, errorHolder );

		// If we have a logger, test it is of the right class.
		final Object loggerObj = settings.get( KEY_LOGGER );
		if ( loggerObj != null && !Logger.class.isInstance( loggerObj ) )
		{
			errorHolder.append( "Value for parameter " + KEY_LOGGER + " is not of the right class. "
					+ "Expected " + Logger.class.getName() + ", got " + loggerObj.getClass().getName() + ".\n" );
			ok = false;
		}

		final List< String > mandatoryKeys = Arrays.asList(
				KEY_OMNIPOSE_PYTHON_FILEPATH,
				KEY_OMNIPOSE_MODEL,
				KEY_TARGET_CHANNEL,
				KEY_OPTIONAL_CHANNEL_2,
				KEY_CELL_DIAMETER,
				KEY_USE_GPU,
				KEY_SIMPLIFY_CONTOURS );
		final List< String > optionalKeys = Arrays.asList(
				KEY_OMNIPOSE_CUSTOM_MODEL_FILEPATH,
				KEY_LOGGER,
				KEY_FLOW_THRESHOLD,
				KEY_CELL_PROB_THRESHOLD,
                                KEY_NB_CLASSES);
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();

		// Extra test to make sure we can read the classifier file.
		if ( ok )
		{
			final Object obj = settings.get( KEY_OMNIPOSE_PYTHON_FILEPATH );
			if ( obj == null )
			{
				errorMessage = "The path to the Omnipose python executable is not set.";
				return false;
			}

			if ( !IOUtils.canReadFile( ( String ) obj, errorHolder ) )
			{
				errorMessage = "Problem with Omnipose python executable: " + errorHolder.toString();
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
	public AdvancedOmniposeDetectorFactory< T > copy()
	{
		return new AdvancedOmniposeDetectorFactory<>();
	}
}

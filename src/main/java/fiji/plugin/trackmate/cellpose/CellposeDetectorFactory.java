/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2022 TrackMate developers.
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
import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readStringAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.cellpose.CellposeSettings.PretrainedModel;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.detection.SpotGlobalDetectorFactory;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class )
public class CellposeDetectorFactory< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/**
	 * The key to the parameter that stores the path the cellpose model to use.
	 * Value can be {@link CellposeSettings.PretrainedModel}.
	 */
	public static final String KEY_CELLPOSE_MODEL = "CELLPOSE_MODEL";

	public static final PretrainedModel DEFAULT_CELLPOSE_MODEL = PretrainedModel.CYTO;

	/**
	 * The key to the parameter that stores the path to the Python instance that
	 * can run cellpose if you installed it via Conda or the cellpose executable
	 * if you have installed the standalone version. Something like
	 * '/opt/anaconda3/envs/cellpose/bin/python' or
	 * 'C:\Users\tinevez\Applications\cellpose.exe'.
	 */
	public static final String KEY_CELLPOSE_PYTHON_FILEPATH = "CELLPOSE_PYTHON_FILEPATH";

	public static final String DEFAULT_CELLPOSE_PYTHON_FILEPATH = "/opt/anaconda3/envs/cellpose/bin/python";

	/**
	 * The key to the parameter that stores the path to the custom model file to
	 * use with Cellpose. It must be an absolute file path.
	 */
	public static final String KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH = "CELLPOSE_MODEL_FILEPATH";

	public static final String DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH = "";

	/**
	 * The key to the parameter that stores the second optional channel to
	 * segment. Use -1 to ignore.
	 */
	public static final String KEY_OPTIONAL_CHANNEL_2 = "OPTIONAL_CHANNEL_2";

	public static final Integer DEFAULT_OPTIONAL_CHANNEL_2 = Integer.valueOf( 0 );

	/**
	 * The key to the parameter that store the estimated cell diameter. Contrary
	 * to Cellpose, this must be specified in physical units (e.g. µm) and
	 * TrackMate wil do the conversion. Use 0 or a negative value to have
	 * Cellpose determine this automatically (but it will take a bit longer).
	 */
	public static final String KEY_CELL_DIAMETER = "CELL_DIAMETER";

	public static final Double DEFAULT_CELL_DIAMETER = Double.valueOf( 30. );

	/**
	 * They key to the parameter that configures whether Cellpose will try to
	 * use GPU acceleration. For this to work, a working Cellpose with working
	 * GPU support must be present on the system. If not, Cellpose will default
	 * to using the CPU.
	 */
	public static final String KEY_USE_GPU = "USE_GPU";

	public static final Boolean DEFAULT_USE_GPU = Boolean.valueOf( true );

	/**
	 * The key to the parameter that stores the logger instance, to which
	 * Cellpose messages wil be sent. Values must be implementing
	 * {@link Logger}. This parameter won't be serialized.
	 * 
	 */
	public static final String KEY_LOGGER = "LOGGER";

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "CELLPOSE_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on cellpose to detect objects."
			+ "<p>"
			+ "The detector simply calls an external cellpose installation. So for this "
			+ "to work, you must have a cellpose installation running on your computer. "
			+ "Please follow the instructions from the cellpose website: "
			+ "<u><a href=\"https://github.com/MouseLand/cellpose#local-installation\">https://github.com/MouseLand/cellpose#local-installation</a></u>"
			+ "<p>"
			+ "You will also need to specify the path to the <b>Python executable</b> that can run cellpose "
			+ "or the <b>cellpose executable</b> directly. "
			+ "For instance if you used anaconda to install cellpose, and that you have a "
			+ "Conda environment called 'cellpose', this path will be something along the line of "
			+ "'/opt/anaconda3/envs/cellpose/bin/python'  or 'C:\\\\Users\\\\tinevez\\\\anaconda3\\\\envs\\\\cellpose_biop_gpu\\\\python.exe' "
			+ "If you installed the standalone version, the path to it would something like "
			+ "this on Windows: 'C:\\Users\\tinevez\\Applications\\cellpose.exe'. "
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the Cellpose paper: <a href=\"https://doi.org/10.1038/s41592-020-01018-x\">Stringer, C., Wang, T., Michaelos, M. et al. "
			+ "Cellpose: a generalist algorithm for cellular segmentation. "
			+ "Nat Methods 18, 100–106 (2021)</a>"
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"https://imagej.net/plugins/trackmate/trackmate-cellpose\">on the ImageJ Wiki</a>."
			+ "</html>";

	/*
	 * FIELDS
	 */

	/** The image to operate on. Multiple frames, single channel. */
	protected ImgPlus< T > img;

	protected Map< String, Object > settings;

	protected String errorMessage;

	/*
	 * METHODS
	 */

	@Override
	public SpotGlobalDetector< T > getDetector( final Interval interval )
	{
		final String cellposePythonPath = ( String ) settings.get( KEY_CELLPOSE_PYTHON_FILEPATH );
		final PretrainedModel model = ( PretrainedModel ) settings.get( KEY_CELLPOSE_MODEL );
		final String customModelPath = ( String ) settings.get( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH );
		final boolean simplifyContours = ( boolean ) settings.get( KEY_SIMPLIFY_CONTOURS );
		final boolean useGPU = ( boolean ) settings.get( KEY_USE_GPU );

		// Channels are 0-based (0: grayscale, then R & G & B).
		final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL );
		final int channel2 = ( Integer ) settings.get( KEY_OPTIONAL_CHANNEL_2 );

		// Convert to diameter in pixels.
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final double diameter = ( double ) settings.get( KEY_CELL_DIAMETER ) / calibration[ 0 ];

		final CellposeSettings cellposeSettings = CellposeSettings.create()
				.cellposePythonPath( cellposePythonPath )
				.customModel( customModelPath )
				.model( model )
				.channel1( channel )
				.channel2( channel2 )
				.diameter( diameter )
				.useGPU( useGPU )
				.simplifyContours( simplifyContours )
				.get();

		// Logger.
		final Logger logger = ( Logger ) settings.get( KEY_LOGGER );
		final CellposeDetector< T > detector = new CellposeDetector<>(
				img,
				interval,
				cellposeSettings,
				logger );
		return detector;
	}

	@Override
	public boolean forbidMultithreading()
	{
		/*
		 * We want to run one frame after another, because the inference for one
		 * frame takes all the resources anyway.
		 */
		return true;
	}

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = writeTargetChannel( settings, element, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_CELLPOSE_PYTHON_FILEPATH, String.class, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH, String.class, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_OPTIONAL_CHANNEL_2, Integer.class, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_CELL_DIAMETER, Double.class, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_USE_GPU, Boolean.class, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_SIMPLIFY_CONTOURS, Boolean.class, errorHolder );

		final PretrainedModel model = ( PretrainedModel ) settings.get( KEY_CELLPOSE_MODEL );
		element.setAttribute( KEY_CELLPOSE_MODEL, model.name() );

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

		// Read model.
		final String str = element.getAttributeValue( KEY_CELLPOSE_MODEL );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + KEY_CELLPOSE_MODEL + " could not be found in XML element.\n" );
			ok = false;
		}
		settings.put( KEY_CELLPOSE_MODEL, PretrainedModel.valueOf( str ) );

		return checkSettings( settings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new CellposeDetectorConfigurationPanel( settings, model );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_CELLPOSE_PYTHON_FILEPATH, DEFAULT_CELLPOSE_PYTHON_FILEPATH );
		settings.put( KEY_CELLPOSE_MODEL, DEFAULT_CELLPOSE_MODEL );
		settings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		settings.put( KEY_OPTIONAL_CHANNEL_2, DEFAULT_OPTIONAL_CHANNEL_2 );
		settings.put( KEY_CELL_DIAMETER, DEFAULT_CELL_DIAMETER );
		settings.put( KEY_USE_GPU, DEFAULT_USE_GPU );
		settings.put( KEY_SIMPLIFY_CONTOURS, true );
		settings.put( KEY_LOGGER, Logger.DEFAULT_LOGGER );
		settings.put( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH, DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH );
		return settings;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_CELLPOSE_PYTHON_FILEPATH, String.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH, String.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELLPOSE_MODEL, PretrainedModel.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_OPTIONAL_CHANNEL_2, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CELL_DIAMETER, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_USE_GPU, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_SIMPLIFY_CONTOURS, Boolean.class, errorHolder );

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
				KEY_LOGGER );
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
		return new CellposeDetectorFactory<>();
	}
}

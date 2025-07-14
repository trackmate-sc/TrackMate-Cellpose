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
package fiji.plugin.trackmate.cellpose;

import java.util.Map;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.detection.SpotGlobalDetectorFactory;
import fiji.plugin.trackmate.util.cli.AbstractCLIDetectorFactory;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW )
public class CellposeDetectorFactory< T extends RealType< T > & NativeType< T > >
		extends AbstractCLIDetectorFactory< T, CellposeCLI >
		implements SpotGlobalDetectorFactory< T >
{

	public CellposeDetectorFactory()
	{
		super( new CellposeCLI( 0, "no units yet", 1. ),
				DETECTOR_KEY, NAME, INFO_TEXT, DOC_CELLPOSE_URL,
				CellposeUtils.cellposeLogo64() );
	}

	public static final String KEY_CELLPOSE_MODEL = "CELLPOSE_MODEL";

	public static final String DEFAULT_CELLPOSE_MODEL = "cyto3";

	/**
	 * The key to the parameter that stores the path to the custom model file to
	 * use with Cellpose. It must be an absolute file path.
	 */
	public static final String KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH = "CELLPOSE_MODEL_FILEPATH";

	public static final String DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH = "";

	public static final String KEY_CELLPOSE_PRETRAINED_OR_CUSTOM = "PRETRAINED_OR_CUSTOM";

	public static final String DEFAULT_CELLPOSE_PRETRAINED_OR_CUSTOM = KEY_CELLPOSE_MODEL;

	public static final String DEFAULT_TARGET_CHANNEL = "0";

	public static final String KEY_OPTIONAL_CHANNEL_2 = "OPTIONAL_CHANNEL_2";

	public static final String DEFAULT_OPTIONAL_CHANNEL_2 = "0";

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

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "CELLPOSE_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose detector";

	public static final String DOC_CELLPOSE_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-cellpose";

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
			+ "<a href=\"" + DOC_CELLPOSE_URL + "\">on the ImageJ Wiki</a>."
			+ "</html>";

	/*
	 * METHODS
	 */

	@Override
	protected CellposeCLI createCLIConfigurator()
	{
		final int cDim = img.dimensionIndex( Axes.CHANNEL );
		final int nChannels = cDim < 1 ? 1 : ( int ) img.dimension( cDim );
		final int xDim = img.dimensionIndex( Axes.X );
		final String units = img.axis( xDim ).unit();
		final double pixelSize = img.axis( xDim ).averageScale( 0., 1. );
		return new CellposeCLI( nChannels, units, pixelSize );
	}

	@Override
	public SpotGlobalDetector< T > getDetector( final Interval interval )
	{
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		final CellposeDetector< T > detector = new CellposeDetector<>(
				img,
				interval,
				cli );
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
//
//	@Override
//	public SpotDetectorFactoryBase< T > copy()
//	{
//		return new CellposeDetectorFactory< T >();
//	}
}

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
package fiji.plugin.trackmate.omnipose;

import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.cellpose.CellposeDetector;
import fiji.plugin.trackmate.cellpose.CellposeUtils;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryGenericConfig;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.detection.SpotGlobalDetectorFactory;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW )
public class OmniposeDetectorFactory< T extends RealType< T > & NativeType< T > >
		implements SpotGlobalDetectorFactory< T >, SpotDetectorFactoryGenericConfig< T, OmniposeCLI >
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "OMNIPOSE_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Omnipose detector";

	public static final String DOC_OMNI_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-omnipose";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on omnipose to detect objects."
			+ "<p>"
			+ "The detector simply calls an external omnipose installation. So for this "
			+ "to work, you must have a omnipose installation running on your computer. "
			+ "Please follow the instructions from the omnipose website: "
			+ "<u><a href=\"https://github.com/kevinjohncutler/omnipose\">https://github.com/kevinjohncutler/omnipose</a></u>"
			+ "<p>"
			+ "You will also need to specify the path to the <b>Python executable</b> that can run omnipose "
			+ "or the <b>omnipose executable</b> directly. "
			+ "For instance if you used anaconda to install omnipose, and that you have a "
			+ "Conda environment called 'omnipose', this path will be something along the line of "
			+ "'/opt/anaconda3/envs/omnipose/bin/python'  or 'C:\\\\Users\\\\tinevez\\\\anaconda3\\\\envs\\\\omnipose_biop_gpu\\\\python.exe' "
			+ "If you installed the standalone version, the path to it would something like "
			+ "this on Windows: 'C:\\Users\\tinevez\\Applications\\omnipose.exe'. "
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the omnipose paper: <a href=\"https://doi.org/10.1038/s41592-022-01639-4\">Cutler, Kevin J., et al., "
			+ "'Omnipose: A High-Precision Morphology-Independent Solution for Bacterial Cell Segmentation.' "
			+ "Nature Methods 19, no. 11 (November 2022): 1438–48.</a>"
			+ "</html>";

	/*
	 * METHODS
	 */

	@Override
	public OmniposeCLI getConfigurator( final ImagePlus imp )
	{
		final int nChannels = ( imp == null ) ? 1 : imp.getNChannels();
		final String units = ( imp == null ) ? "no input image" : imp.getCalibration().getUnit();
		final double pixelSize = ( imp == null ) ? 1. : imp.getCalibration().pixelWidth;
		return new OmniposeCLI( nChannels, units, pixelSize );
	}

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		final OmniposeCLI cli = getConfigurator( img );
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		final CellposeDetector< T > detector = new CellposeDetector<>(
				img,
				interval,
				cli );
		return detector;
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
	public String getUrl()
	{
		return DOC_OMNI_URL;
	}

	@Override
	public ImageIcon getIcon()
	{
		return CellposeUtils.omniposeLogo64();
	}
}

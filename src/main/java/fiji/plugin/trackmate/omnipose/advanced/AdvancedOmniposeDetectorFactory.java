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

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW - 1. )
public class AdvancedOmniposeDetectorFactory< T extends RealType< T > & NativeType< T > >
		implements SpotGlobalDetectorFactory< T >, SpotDetectorFactoryGenericConfig< T, AdvancedOmniposeCLI >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "OMNIPOSE_ADVANCED_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Omnipose advanced detector";

	public static final String DOC_ADV_OMNI_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-omnipose-advanced";

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
			+ "<a href=\"" + DOC_ADV_OMNI_URL + "\">on the ImageJ Wiki</a>."
			+ "</html>";

	/*
	 * METHODS
	 */

	@Override
	public AdvancedOmniposeCLI getConfigurator( final ImagePlus imp )
	{
		final int nChannels = ( imp == null ) ? 1 : imp.getNChannels();
		final String units = ( imp == null ) ? "no input image" : imp.getCalibration().getUnit();
		final double pixelSize = ( imp == null ) ? 1. : imp.getCalibration().pixelWidth;
		return new AdvancedOmniposeCLI( nChannels, units, pixelSize );
	}

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		final AdvancedOmniposeCLI cli = getConfigurator( img );
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		final CellposeDetector< T > detector = new CellposeDetector<>( img, interval, cli );
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
		return DOC_ADV_OMNI_URL;
	}

	@Override
	public ImageIcon getIcon()
	{
		return CellposeUtils.omniposeLogo64();
	}

	@Override
	public boolean has2Dsegmentation()
	{
		return true;
	}
}

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

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

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
public class CellposeDetectorFactory< T extends RealType< T > & NativeType< T > >
		implements SpotGlobalDetectorFactory< T >, SpotDetectorFactoryGenericConfig< T, CellposeCLI >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "CELLPOSE_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose detector";

	public static final String DOC_CELLPOSE_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-cellpose";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on cellpose to detect objects."
			+ "<p>"
			+ "The detector simply calls an external <b>cellpose 3</b> installation. So for this "
			+ "to work, you must have a cellpose installation running on your computer. "
			+ "Please follow the instructions on the TrackMate-Cellpose page, linked below, to install"
			+ "cellpose 3 on your computer."
			+ "<p>"
			+ "You must also configure properly the conda (or mamba) executable in Fiji. "
			+ "Run <u>Edit >  Options > Configure TrackMate Conda path...</u> to do so."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the cellpose paper: <a href=\"https://doi.org/10.1038/s41592-020-01018-x\">Stringer, C., Wang, T., Michaelos, M. et al. "
			+ "Cellpose: a generalist algorithm for cellular segmentation. "
			+ "Nat Methods 18, 100–106 (2021)</a>"
			+ "</html>";

	@Override
	public CellposeCLI getConfigurator( final ImagePlus imp )
	{
		final int nChannels = ( imp == null ) ? 1 : imp.getNChannels();
		final String units = ( imp == null ) ? "no input image" : imp.getCalibration().getUnit();
		final double pixelSize = ( imp == null ) ? 1. : imp.getCalibration().pixelWidth;
		return new CellposeCLI( nChannels, units, pixelSize );
	}

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		// Create the CLI and loads settings into it.
		final CellposeCLI cli = getConfigurator( img );
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		// Create the detector.
		return new CellposeDetector<>( img, interval, cli );
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
		return DOC_CELLPOSE_URL;
	}

	@Override
	public ImageIcon getIcon()
	{
		return CellposeUtils.cellposeLogo64();
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

}

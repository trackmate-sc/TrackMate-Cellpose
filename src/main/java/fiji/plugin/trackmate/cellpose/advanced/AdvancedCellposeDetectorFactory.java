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

import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.cellpose.CellposeCLI;
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
public class AdvancedCellposeDetectorFactory< T extends RealType< T > & NativeType< T > >
		implements SpotGlobalDetectorFactory< T >, SpotDetectorFactoryGenericConfig< T, AdvancedCellposeCLI >

{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "CELLPOSE_ADVANCED_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose advanced detector";

	public static final String DOC_ADV_CELLPOSE_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-cellpose-advanced";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on cellpose to detect objects."
			+ "<p>"
			+ "It is identical to the Cellpose detector, except that it allows "
			+ "tweaking the 'flow threshold' and 'cell probability threshold' parameters of the "
			+ "cellpose algorithm."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the Cellpose paper: "
			+ "<a href=\"https://doi.org/10.1038/s41592-020-01018-x\">Stringer, C., Wang, T., Michaelos, M. et al. "
			+ "Cellpose: a generalist algorithm for cellular segmentation. "
			+ "Nat Methods 18, 100–106 (2021)</a>"
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"" + DOC_ADV_CELLPOSE_URL + "\">on the ImageJ Wiki</a>."
			+ "</html>";

	/*
	 * METHODS
	 */

	@Override
	public AdvancedCellposeCLI getConfigurator( final ImagePlus imp )
	{
		final int nChannels = ( imp == null ) ? 1 : imp.getNChannels();
		final String units = ( imp == null ) ? "no input image" : imp.getCalibration().getUnit();
		final double pixelSize = ( imp == null ) ? 1. : imp.getCalibration().pixelWidth;
		return new AdvancedCellposeCLI( nChannels, units, pixelSize );
	}

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		// Create the CLI and loads settings into it.
		final CellposeCLI cli = getConfigurator( img );
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, cli );
		// Create the detector.
		final CellposeDetector< T > detector = new CellposeDetector<>( img, interval, cli );
		return detector;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return CellposeUtils.cellposeLogo64();
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
		return DOC_ADV_CELLPOSE_URL;
	}

	@Override
	public boolean has2Dsegmentation()
	{
		return true;
	}
}

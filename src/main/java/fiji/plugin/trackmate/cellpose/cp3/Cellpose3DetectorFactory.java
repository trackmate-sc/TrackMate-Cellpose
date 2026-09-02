/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.cellpose.cp3;

import java.util.Map;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;
import org.scijava.ui.config.visitors.Maps;

import fiji.plugin.trackmate.cellpose.AbstractCellposeDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW )
public class Cellpose3DetectorFactory< T extends RealType< T > & NativeType< T > >
		extends AbstractCellposeDetectorFactory< T, Cellpose3Config >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "CELLPOSE_3_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose-3 detector";

	public static final String DOC_CELLPOSE_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-cellpose";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on Cellpose to detect objects."
			+ "<p>"
			+ "The detector simply calls <b>Cellpose 3</b>, that will be installed on "
			+ "your system via Appose. Appose will download and install Cellpose for you, "
			+ "but it might take a few minutes the first time you use it."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the Cellpose paper: <a href=\"https://doi.org/10.1038/s41592-020-01018-x\">Stringer, C., Wang, T., Michaelos, M. et al. "
			+ "Cellpose: a generalist algorithm for cellular segmentation. "
			+ "Nat Methods 18, 100–106 (2021)</a>"
			+ "</html>";


	@Override
	public Cellpose3Config createConfig( final ImagePlus imp )
	{
		if ( imp == null )
			return new Cellpose3Config( 3, 1., "pixel" );

		final int nChannels = imp.getNChannels();
		final double pixelSize = imp.getCalibration().pixelWidth;
		final String units = imp.getCalibration().getUnit();
		return new Cellpose3Config( nChannels, pixelSize, units );
	}

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		final Cellpose3Config config = createConfig( img );
		Maps.fromMap( settings, config );
		return new Cellpose3Detector<>( img, interval, config );
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
}

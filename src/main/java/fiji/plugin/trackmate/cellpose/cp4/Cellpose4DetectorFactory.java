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
package fiji.plugin.trackmate.cellpose.cp4;

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
public class Cellpose4DetectorFactory< T extends RealType< T > & NativeType< T > >
		extends AbstractCellposeDetectorFactory< T, Cellpose4Config >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "CELLPOSE_SAM_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose-SAM detector";

	public static final String DOC_CELLPOSE_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-cellpose-sam";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on Cellpose-SAM to detect objects."
			+ "<p>"
			+ "The detector simply calls <b>Cellpose-SAM</b>, that will be installed on "
			+ "your system via Appose. Appose will download and install Cellpose for you, "
			+ "but it might take a few minutes the first time you use it."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the Cellpose SAM paper: "
			+ "<a href=\"https://doi.org/10.1101/2025.04.28.651001\">"
			+ "Marius Pachitariu, Michael Rariden, Carsen Stringer. "
			+ "Cellpose-SAM: superhuman generalization for cellular segmentation. "
			+ "bioRxiv 2025.04.28.651001"
			+ "</a>"
			+ "</html>";

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		final Cellpose4Config config = createConfig( img );
		Maps.fromMap( settings, config );
		return new Cellpose4Detector< T >( img, interval, config );
	}

	@Override
	public Cellpose4Config createConfig( final ImagePlus imp )
	{
		if ( imp == null )
			return new Cellpose4Config( 3, 1., "pixel" );

		final int nChannels = imp.getNChannels();
		final double pixelSize = imp.getCalibration().pixelWidth;
		final String units = imp.getCalibration().getUnit();
		return new Cellpose4Config( nChannels, pixelSize, units );
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

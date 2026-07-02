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
package fiji.plugin.trackmate.cellpose;

import static fiji.plugin.trackmate.gui.GuiUtils.getResource;
import static fiji.plugin.trackmate.gui.GuiUtils.scaleImage;

import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.ui.config.visitors.Maps;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotGlobalDetectorFactory;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.config.GenericConfigPanel;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class AbstractCellposeDetectorFactory< T extends RealType< T > & NativeType< T >, C extends CellposeBaseConfig< ? > > implements SpotGlobalDetectorFactory< T >
{

	protected abstract C createConfig( int nChannels, double pixelSize, String units );

	protected C createConfig( final ImgPlus< ? > img )
	{
		final int nChannels = img.dimensionIndex( Axes.CHANNEL ) < 0 ? 1 : ( int ) img.dimension( img.dimensionIndex( Axes.CHANNEL ) );
		final double pixelSize = img.averageScale( img.dimensionIndex( Axes.X ) );
		final String units = img.axis( img.dimensionIndex( Axes.X ) ).unit();
		return createConfig( nChannels, pixelSize, units );
	}

	private C createConfig( final Settings settings )
	{
		final ImagePlus imp = settings.imp;
		if ( imp == null )
			return createConfig( 1, 1., "pixel" );
		return createConfig( TMUtils.rawWraps( imp ) );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new GenericConfigPanel( settings, model, createConfig( settings ), () -> this );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final int nChannels = 3;
		final double pixelSize = 1.;
		final String units = "pixel";
		final C config = createConfig( nChannels, pixelSize, units );
		return Maps.toMap( config );
	}

	@Override
	public boolean has2Dsegmentation()
	{
		return true;
	}

	@Override
	public boolean has3Dsegmentation()
	{
		return true;
	}

	@Override
	public ImageIcon getIcon()
	{
		return cellposeLogo64();
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

	public static final ImageIcon cellposeLogo()
	{
		return new ImageIcon( getResource( "images/cellposelogo.png", AbstractCellposeDetectorFactory.class ) );
	}

	public static final ImageIcon cellposeLogo64()
	{
		return scaleImage( cellposeLogo(), 64, 64 );
	}
}

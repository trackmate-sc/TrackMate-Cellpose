/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2025 TrackMate developers.
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.ImageIcon;

import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class CellposeUtils
{

	public static final ImageIcon cellposeLogo()
	{
		return new ImageIcon( getResource( "images/cellposelogo.png", CellposeUtils.class ) );
	}

	public static final ImageIcon cellposeLogo64()
	{
		return scaleImage( cellposeLogo(), 64, 64 );
	}

	public static final ImageIcon omniposeLogo()
	{
		return new ImageIcon( getResource( "images/omniposelogo.png", CellposeUtils.class ) );
	}

	public static final ImageIcon omniposeLogo64()
	{
		return scaleImage( omniposeLogo(), 64, 64 );
	}

	public static final < T extends RealType< T > & NativeType< T > > List< ImagePlus > crop( final ImgPlus< T > img, final Interval interval, final Function< Long, String > nameGen )
	{
		final int zIndex = img.dimensionIndex( Axes.Z );
		final int cIndex = img.dimensionIndex( Axes.CHANNEL );
		final Interval cropInterval;
		if ( zIndex < 0 )
		{
			// 2D
			if ( cIndex < 0 )
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ),
						interval.max( 0 ), interval.max( 1 ) );
			else
				// Include all channels
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ), img.min( cIndex ),
						interval.max( 0 ), interval.max( 1 ), img.max( cIndex ) );
		}
		else
		{
			if ( cIndex < 0 )
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ), interval.min( 2 ),
						interval.max( 0 ), interval.max( 1 ), interval.max( 2 ) );
			else
				cropInterval = Intervals.createMinMax(
						interval.min( 0 ), interval.min( 1 ), interval.min( 2 ), img.min( cIndex ),
						interval.max( 0 ), interval.max( 1 ), interval.max( 2 ), img.max( cIndex ) );
		}

		final List< ImagePlus > imps = new ArrayList<>();
		final int timeIndex = img.dimensionIndex( Axes.TIME );
		if ( timeIndex < 0 )
		{
			// No time.
			final IntervalView< T > crop = Views.interval( img, cropInterval );
			final String name = nameGen.apply( 0l ) + ".tif";
			imps.add( ImageJFunctions.wrap( crop, name ) );
		}
		else
		{
			// In the interval, time is always the last.
			final long minT = interval.min( interval.numDimensions() - 1 );
			final long maxT = interval.max( interval.numDimensions() - 1 );
			for ( long t = minT; t <= maxT; t++ )
			{
				final ImgPlus< T > tpTCZ = ImgPlusViews.hyperSlice( img, timeIndex, t );

				// Put if necessary the channel axis as the last one (CellPose
				// format)
				final int chanDim = tpTCZ.dimensionIndex( Axes.CHANNEL );
				ImgPlus< T > tp = tpTCZ;
				if ( chanDim > 1 )
				{
					tp = ImgPlusViews.moveAxis( tpTCZ, chanDim, tpTCZ.numDimensions() - 1 );
				}
				// possibly 2D or 3D with or without channel.
				final IntervalView< T > crop = Views.interval( tp, cropInterval );
				final String name = nameGen.apply( t ) + ".tif";
				imps.add( ImageJFunctions.wrap( crop, name ) );
			}
		}
		return imps;
	}
}

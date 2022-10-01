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

import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Settings;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.Type;

public class CellposeUtils
{

	public static < T extends Type< T > > ImgPlus< T > hyperSlice( final ImgPlus< T > img, final long frame )
	{
		final int timeDim = img.dimensionIndex( Axes.TIME );
		final ImgPlus< T > imgTC = timeDim < 0
				? img
				: ImgPlusViews.hyperSlice( img, timeDim, frame );

		// Squeeze Z dimension if its size is 1.
		final int zDim = imgTC.dimensionIndex( Axes.Z );
		final ImgPlus< T > imgTCZ;
		if ( zDim >= 0 && imgTC.dimension( zDim ) <= 1 )
			imgTCZ = ImgPlusViews.hyperSlice( imgTC, zDim, imgTC.min( zDim ) );
		else
			imgTCZ = imgTC;

		return imgTCZ;
	}

	public static URL getResource( final String name )
	{
		return CellposeDetectorFactory.class.getClassLoader().getResource( name );
	}

	public static final ImageIcon logo()
	{
		return new ImageIcon( getResource( "images/cellposelogo.png" ) );
	}

	public static final ImageIcon logo64()
	{
		return scaleImage( logo(), 64, 64 );
	}

	public static final ImageIcon scaleImage( final ImageIcon icon, final int w, final int h )
	{
		int nw = icon.getIconWidth();
		int nh = icon.getIconHeight();

		if ( icon.getIconWidth() > w )
		{
			nw = w;
			nh = ( nw * icon.getIconHeight() ) / icon.getIconWidth();
		}

		if ( nh > h )
		{
			nh = h;
			nw = ( icon.getIconWidth() * nh ) / icon.getIconHeight();
		}

		return new ImageIcon( icon.getImage().getScaledInstance( nw, nh, Image.SCALE_DEFAULT ) );
	}

	public static final Interval getIntervalWithTime( final ImgPlus< ? > img, final Settings settings )
	{
		final long[] max = new long[ img.numDimensions() ];
		final long[] min = new long[ img.numDimensions() ];

		// X, we must have it.
		final int xindex = img.dimensionIndex( Axes.X );
		min[ xindex ] = settings.getXstart();
		max[ xindex ] = settings.getXend();

		// Y, we must have it.
		final int yindex = img.dimensionIndex( Axes.Y );
		min[ yindex ] = settings.getYstart();
		max[ yindex ] = settings.getYend();

		// Z, we MIGHT have it.
		final int zindex = img.dimensionIndex( Axes.Z );
		if ( zindex >= 0 )
		{
			min[ zindex ] = settings.zstart;
			max[ zindex ] = settings.zend;
		}

		// management to elsewhere.
		final int tindex = img.dimensionIndex( Axes.TIME );
		if ( tindex >= 0 )
		{
			min[ tindex ] = settings.tstart;
			max[ tindex ] = settings.tend;
		}

		// CHANNEL, we might have it, we drop it.
		final long[] max2;
		final long[] min2;
		final int cindex = img.dimensionIndex( Axes.CHANNEL );
		if ( cindex >= 0 )
		{
			max2 = new long[ img.numDimensions() - 1 ];
			min2 = new long[ img.numDimensions() - 1 ];
			int d2 = 0;
			for ( int d = 0; d < min.length; d++ )
			{
				if ( d != cindex )
				{
					min2[ d2 ] = min[ d ];
					max2[ d2 ] = max[ d ];
					d2++;
				}
			}
		}
		else
		{
			max2 = max;
			min2 = min;
		}

		final FinalInterval interval = new FinalInterval( min2, max2 );
		return interval;
	}

}

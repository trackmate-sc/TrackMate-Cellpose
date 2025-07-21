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

import static fiji.plugin.trackmate.gui.GuiUtils.getResource;
import static fiji.plugin.trackmate.gui.GuiUtils.scaleImage;

import javax.swing.ImageIcon;

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
}

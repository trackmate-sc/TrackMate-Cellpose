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
package fiji.plugin.trackmate.cellpose.sam;

import fiji.plugin.trackmate.cellpose.ICellposeCLI;
import fiji.plugin.trackmate.util.cli.Configurator.ChoiceArgument;

/**
 * Interface for Cellpose-SAM CLI configurators, exposing the arguments
 * required by {@code CellposeSAMDetector}.
 */
public interface ICellposeSAMCLI extends ICellposeCLI
{
	ChoiceArgument channels();
}

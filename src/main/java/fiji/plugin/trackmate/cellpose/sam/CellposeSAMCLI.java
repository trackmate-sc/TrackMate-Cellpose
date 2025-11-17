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

import static fiji.plugin.trackmate.cellpose.CellposeCLI.KEY_CELLPOSE_MODEL;
import static fiji.plugin.trackmate.cellpose.CellposeCLI.KEY_CELLPOSE_PRETRAINED_OR_CUSTOM;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import fiji.plugin.trackmate.cellpose.CellposeCLIBase;

public class CellposeSAMCLI extends CellposeCLIBase
{

	private final ChoiceArgument modelPretrained;

	private final SelectableArguments selectPretrainedOrCustom;

	private final ChoiceArgument channels;

	public CellposeSAMCLI( final int nChannels, final String units, final double pixelSize )
	{
		super( nChannels, units, pixelSize );

		// The pretrained model list.
		this.modelPretrained = addChoiceArgument()
				.name( "Pretrained model" )
				.help( "Name of the pretrained cellpose 4 model to use." )
				.argument( "--pretrained_model" )
				.required( true )
				.addChoice( "cpsam", "cellpose-SAM" )
				.defaultValue( "cpsam" )
				.key( KEY_CELLPOSE_MODEL )
				.get();

		// State that we can use pretrained or custom.
		this.selectPretrainedOrCustom = addSelectableArguments()
				.add( modelPretrained )
				.add( customModelPath() )
				.key( KEY_CELLPOSE_PRETRAINED_OR_CUSTOM );

		// Pass all channels or just one
		final ChoiceAdder chan1Adder = addChoiceArgument()
				.key( KEY_TARGET_CHANNEL )
				.inCLI( false )
				.name( "Use channels" )
				.help( "What channels of the image to pass to cellpose-SAM." )
				.required( true )
				.addChoice( "0", "all channels" );
		for ( int c = 1; c <= nChannels; c++ )
			chan1Adder.addChoice( "" + c, "channel " + c );
		chan1Adder.defaultValue( "0" );
		this.channels = chan1Adder.get();

		// Re-add the arguments at the desired position.
		arguments.remove( modelPretrained );
		arguments.add( 0, modelPretrained );
		arguments.remove( channels );
		arguments.add( 2, channels );
	}

	@Override
	public String getCommand()
	{
		return "cellpose";
	}

	public ChoiceArgument channels()
	{
		return channels;
	}

	public ChoiceArgument modelPretrained()
	{
		return modelPretrained;
	}

	public SelectableArguments selectPretrainedOrCustom()
	{
		return selectPretrainedOrCustom;
	}
}

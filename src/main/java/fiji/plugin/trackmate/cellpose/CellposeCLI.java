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

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.Collections;

import javax.swing.JFrame;

import fiji.plugin.trackmate.util.cli.CommandBuilder;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder.ConfigPanel;

public class CellposeCLI extends CellposeCLIBase
{

	/*
	 * CONSTANTS
	 */

	public static final String KEY_CELLPOSE_MODEL = "CELLPOSE_MODEL";

	public static final String DEFAULT_CELLPOSE_MODEL = "cyto3";

	public static final String KEY_CELLPOSE_PRETRAINED_OR_CUSTOM = "PRETRAINED_OR_CUSTOM";

	public static final String DEFAULT_CELLPOSE_PRETRAINED_OR_CUSTOM = KEY_CELLPOSE_MODEL;

	public static final String DEFAULT_TARGET_CHANNEL = "0";

	public static final String KEY_OPTIONAL_CHANNEL_2 = "OPTIONAL_CHANNEL_2";

	public static final String DEFAULT_OPTIONAL_CHANNEL_2 = "0";

	/**
	 * The key to the parameter that store the estimated cell diameter. Contrary
	 * to Cellpose, this must be specified in physical units (e.g. µm) and
	 * TrackMate wil do the conversion. Use 0 or a negative value to have
	 * Cellpose determine this automatically (but it will take a bit longer).
	 */
	public static final String KEY_CELL_DIAMETER = "CELL_DIAMETER";

	public static final Double DEFAULT_CELL_DIAMETER = Double.valueOf( 30. );

	private final ChoiceArgument modelPretrained;

	private final SelectableArguments selectPretrainedOrCustom;

	private final ChoiceArgument chan1;

	private final ChoiceArgument chan2;

	private final DoubleArgument diameter;

	public CellposeCLI( final int nChannels, final String units, final double pixelSize )
	{
		super( nChannels, units, pixelSize );

		// Main segmentation channel
		final ChoiceAdder chan1Adder = addChoiceArgument()
				.key( KEY_TARGET_CHANNEL )
				.argument( "--chan" )
				.name( "Target channel" )
				.help( "Index of the channel to segment." )
				.required( true )
				.addChoice( DEFAULT_TARGET_CHANNEL, "0 - gray" );
		for ( int c = 1; c <= nChannels; c++ )
			chan1Adder.addChoice( "" + c );
		chan1Adder.defaultValue( DEFAULT_TARGET_CHANNEL );
		this.chan1 = chan1Adder.get();

		// Second optional channel.
		final ChoiceAdder chan2Adder = addChoiceArgument()
				.key( KEY_OPTIONAL_CHANNEL_2 )
				.argument( "--chan2" )
				.name( "Second optional channel" )
				.help( "Second optional channel to segment for cyto* models." )
				.required( false )
				.addChoice( DEFAULT_OPTIONAL_CHANNEL_2, "0 - don't use" );
		for ( int c = 1; c <= nChannels; c++ )
			chan2Adder.addChoice( "" + c );
		chan2Adder.defaultValue( DEFAULT_OPTIONAL_CHANNEL_2 );
		this.chan2 = chan2Adder.get();

		// Object diameter
		this.diameter = addDoubleArgument()
				.name( "Cell diameter" )
				.help( "Cell diameter. If 0 will use the diameter of the training labels used in the model, or with built-in model will estimate diameter for each image." )
				.argument( "--diameter" )
				.key( KEY_CELL_DIAMETER )
				.defaultValue( DEFAULT_CELL_DIAMETER )
				.min( 0. )
				.units( units )
				.get();
		// Translate to pixel size.
		setCommandTranslator( diameter, d -> {
			final double diam = ( double ) d;
			final double diamPix = diam > 0 ? ( diam / pixelSize ) : 0.;
			return Collections.singletonList( "" + diamPix );
		} );

		// The pretrained model list.
		this.modelPretrained = addChoiceArgument()
				.name( "Pretrained model" )
				.help( "Name of the pretrained cellpose 3 model to use." )
				.argument( "--pretrained_model" )
				.required( true )
				.addChoice( "cyto3", "cyto3" )
				.addChoice( "nucleitorch_0", "nuclei" )
				.addChoice( "tissuenet_cp3" )
				.addChoice( "livecell_cp3" )
				.addChoice( "yeast_PhC_cp3" )
				.addChoice( "yeast_BF_cp3" )
				.addChoice( "bact_phase_cp3" )
				.addChoice( "bact_fluor_cp3" )
				.addChoice( "deepbacs_cp3" )
				.addChoice( "cyto2torch_0", "cyto2" )
				.addChoice( "cytotorch_0", "cyto" )
				.defaultValue( DEFAULT_CELLPOSE_MODEL )
				.key( KEY_CELLPOSE_MODEL )
				.get();

		// State that we can use pretrained or custom.
		this.selectPretrainedOrCustom = addSelectableArguments()
				.add( modelPretrained )
				.add( customModelPath() )
				.key( KEY_CELLPOSE_PRETRAINED_OR_CUSTOM );

		// Re-add it the arguments at the desired position.
		arguments.remove( modelPretrained );
		arguments.add( 0, modelPretrained );
		arguments.remove( chan1 );
		arguments.add( 2, chan1 );
		arguments.remove( chan2 );
		arguments.add( 3, chan2 );
		arguments.remove( diameter );
		arguments.add( 4, diameter );
	}

	public ChoiceArgument chan1()
	{
		return chan1;
	}

	public ChoiceArgument chan2()
	{
		return chan2;
	}

	public DoubleArgument diameter()
	{
		return diameter;
	}

	public ChoiceArgument modelPretrained()
	{
		return modelPretrained;
	}

	public SelectableArguments selectPretrainedOrCustom()
	{
		return selectPretrainedOrCustom;
	}

	@Override
	public String getCommand()
	{
		return "cellpose";
	}

	public static void main( final String[] args )
	{
		final CellposeCLI cli = new CellposeCLI( 4, "µm", 0.2 );
		System.out.println( cli );

		// Configure the CLI.
		cli.chan1().set( 2 );
		cli.chan2().set( 0 );
		cli.imageFolder().set( "/Users/tinevez/Desktop" );
		// cli.modelPretrained().set( "cyto2" );
		cli.customModelPath().set( "/Users/tinevez/Desktop/trololo" );
		cli.selectPretrainedOrCustom().select( cli.customModelPath() );

		// Output command line.
		System.out.println( "Command line: " );
		System.out.println( CommandBuilder.build( cli ) );

		// Show config panel.
		final ConfigPanel panel = ConfigGuiBuilder.build( cli );
		final JFrame frame = new JFrame( "cellpose CLI" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}
}

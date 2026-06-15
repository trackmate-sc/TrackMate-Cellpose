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
package fiji.plugin.trackmate.omnipose;

import static fiji.plugin.trackmate.cellpose.CellposeCLI.KEY_CELLPOSE_PRETRAINED_OR_CUSTOM;
import static fiji.plugin.trackmate.cellpose.CellposeCLI.KEY_CELL_DIAMETER;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.Collections;

import javax.swing.JFrame;

import fiji.plugin.trackmate.cellpose.CellposeCLIBase;
import fiji.plugin.trackmate.util.cli.CommandBuilder;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder.ConfigPanel;

public class OmniposeCLI extends CellposeCLIBase
{

	/**
	 * The key to the parameter that stores the name ofthe omnipose model to
	 * use.
	 */
	public static final String KEY_OMNIPOSE_MODEL = "OMNIPOSE_MODEL";

	private final ChoiceArgument modelPretrained;

	private final SelectableArguments selectPretrainedOrCustom;

	private final ChoiceArgument chan1;

	private final IntArgument nClasses;

	private DoubleArgument diameter;

	public OmniposeCLI( final int nChannels, final String units, final double pixelSize )
	{
		super( nChannels, units, pixelSize );

		// Pretrained model.
		this.modelPretrained = addChoiceArgument()
				.addChoice( "bact_phase_omni", "Bacteria phase contrast" )
				.addChoice( "bact_fluor_omni", "Bacteria fluorescence" )
				.defaultValue( 0 )
				.argument( "--pretrained_model" )
				.key( KEY_OMNIPOSE_MODEL )
				.name( "Pretrained model" )
				.help( "Use one of the pretrained omnipose models." )
				.get();

		// Main segmentation channel
		final ChoiceAdder chan1Adder = addChoiceArgument()
				.key( KEY_TARGET_CHANNEL )
				.argument( "--chan" )
				.name( "Target channel" )
				.help( "Index of the channel to segment." )
				.required( true );
		for ( int c = 1; c <= nChannels; c++ )
			chan1Adder.addChoice( "" + c );
		chan1Adder.defaultValue( "" + DEFAULT_TARGET_CHANNEL );
		this.chan1 = chan1Adder.get();

		// Object diameter
		this.diameter = addDoubleArgument()
				.name( "Cell diameter" )
				.help( "Cell diameter. If 0 will use the diameter of the training labels used in the model, or with built-in model will estimate diameter for each image." )
				.argument( "--diameter" )
				.key( KEY_CELL_DIAMETER )
				.defaultValue( 2. )
				.min( 0. )
				.units( units )
				.get();
		// Translate to pixel size.
		setCommandTranslator( diameter, d -> {
			final double diam = ( double ) d;
			final double diamPix = diam > 0 ? ( diam / pixelSize ) : 0.;
			return Collections.singletonList( "" + diamPix );
		} );

		// State that we can use pretrained or custom.
		this.selectPretrainedOrCustom = addSelectableArguments()
				.add( modelPretrained )
				.add( customModelPath() )
				.key( KEY_CELLPOSE_PRETRAINED_OR_CUSTOM );

		// Omni flag.
		addFlag()
				.name( "Omni flag" )
				.argument( "--omni" )
				.key( null )
				.visible( false )
				.defaultValue( true )
				.get()
				.set();

		// Nchan -> must be 1
		addIntArgument()
				.argument( "--nchan" )
				.name( "N. channels" )
				.help( "Number of channels on which model is trained" )
				.visible( false )
				.required( true )
				.defaultValue( 1 )
				.get()
				.set( 1 );

		// N-classes -> must be 2
		this.nClasses = addIntArgument()
				.argument( "--nclasses" )
				.name( "N. classes" )
				.help( "Number of classes on which model is trained" )
				.visible( false )
				.required( true )
				.defaultValue( 2 )
				.get();
		// 2 for custom models by default.
		nClasses.set( 2 );

		// Re-add it the arguments at the desired position.
		arguments.remove( modelPretrained );
		arguments.add( 0, modelPretrained );
		arguments.remove( chan1 );
		arguments.add( 2, chan1 );
		arguments.remove( diameter );
		arguments.add( 3, diameter );
	}

	public IntArgument nClasses()
	{
		return nClasses;
	}

	public ChoiceArgument segmentationChannel()
	{
		return chan1;
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
		return "omnipose";
	}

	public static void main( final String[] args )
	{
		final OmniposeCLI cli = new OmniposeCLI( 2, "µm", 0.1 );
		System.out.println( cli );

		// Configure the CLI.
		cli.imageFolder().set( "/Users/tinevez/Desktop" );
		cli.modelPretrained().set( 0 );
		cli.diameter().set( 2. );
		cli.selectPretrainedOrCustom().select( cli.modelPretrained() );
		cli.segmentationChannel().set( 2 );

		// Output command line.
		System.out.println( "Command line: " );
		System.out.println( CommandBuilder.build( cli ) );

		// Show config panel.
		final ConfigPanel panel = ConfigGuiBuilder.build( cli );
		final JFrame frame = new JFrame( cli.getCommand() + " CLI" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}

}

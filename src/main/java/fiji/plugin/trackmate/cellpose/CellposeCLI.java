package fiji.plugin.trackmate.cellpose;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

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

	private final ChoiceArgument modelPretrained;

	private final SelectableArguments selectPretrainedOrCustom;

	private final ChoiceArgument chan1;

	private final ChoiceArgument chan2;

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
	}

	public ChoiceArgument chan1()
	{
		return chan1;
	}

	public ChoiceArgument chan2()
	{
		return chan2;
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

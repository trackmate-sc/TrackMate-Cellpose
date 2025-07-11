package fiji.plugin.trackmate.cellpose;

import javax.swing.JFrame;

import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder.CliConfigPanel;
import fiji.plugin.trackmate.util.cli.CommandBuilder;

public class CellposeCLI extends CellposeCLIBase
{

	private final ChoiceArgument modelPretrained;

	private final SelectableArguments selectPretrainedOrCustom;

	/**
	 * The key to the parameter that stores the second optional channel to
	 * segment. Use -1 to ignore. 0-valued. Careful, the main channel is using
	 * the KEY_TARGET_CHANNEL key, which value is 1-valued...
	 *
	 * @see DetectorKeys#KEY_TARGET_CHANNEL
	 */
	public static final String KEY_OPTIONAL_CHANNEL_2 = "OPTIONAL_CHANNEL_2";

	private final ChoiceArgument chan2;

	public CellposeCLI( final int nChannels, final String units, final double pixelSize )
	{
		super( nChannels, units, pixelSize );

		// Second optional channel.
		final ChoiceAdder chan2Adder = addChoiceArgument()
				.key( KEY_OPTIONAL_CHANNEL_2 )
				.argument( "--chan2" )
				.name( "Second optional channel" )
				.help( "Second optional channel to segment for cyto* models." )
				.required( false )
				.addChoice( "0 - none", "0" );
		for ( int c = 1; c <= nChannels; c++ )
			chan2Adder.addChoice( "" + c );
		chan2Adder.defaultValue( 0 );
		this.chan2 = chan2Adder.get();

		// The pretrained model list.
		this.modelPretrained = addChoiceArgument()
				.name( "Pretrained model" )
				.help( "Name of the pretrained cellpose 3 model to use." )
				.argument( "--pretrained_model" )
				.required( true )
				.addChoice( "cyto3", "cyto3" )
				.addChoice( "nuclei", "nucleitorch_0" )
				.addChoice( "tissuenet_cp3" )
				.addChoice( "livecell_cp3" )
				.addChoice( "yeast_PhC_cp3" )
				.addChoice( "yeast_BF_cp3" )
				.addChoice( "bact_phase_cp3" )
				.addChoice( "bact_fluor_cp3" )
				.addChoice( "deepbacs_cp3" )
				.addChoice( "cyto2", "cyto2torch_0" )
				.addChoice( "cyto", "cytotorch_0" )
				.defaultValue( 0 )
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
		arguments.remove( chan2 );
		arguments.add( 3, chan2 );
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
	protected String getCommand()
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
		final CliConfigPanel panel = CliGuiBuilder.build( cli );
		final JFrame frame = new JFrame( "cellpose CLI" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}
}

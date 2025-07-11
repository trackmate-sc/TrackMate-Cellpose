package fiji.plugin.trackmate.omnipose;

import static fiji.plugin.trackmate.omnipose.OmniposeDetectorFactory.KEY_OMNIPOSE_MODEL;

import javax.swing.JFrame;

import fiji.plugin.trackmate.cellpose.CellposeCLIBase;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder.CliConfigPanel;
import fiji.plugin.trackmate.util.cli.CommandBuilder;

public class OmniposeCLI extends CellposeCLIBase
{

	private final ChoiceArgument modelPretrained;

	private final SelectableArguments selectPretrainedOrCustom;

	public OmniposeCLI( final int nChannels, final String units, final double pixelSize )
	{
		super( nChannels, units, pixelSize );

		// Pretrained model.
		this.modelPretrained = addChoiceArgument()
				.addChoice( "Bacteria phase contrast", "bact_phase_omni" )
				.addChoice( "Bacteria fluorescence", "bact_fluor_omni" )
				.defaultValue( 0 )
				.argument( "--pretrained_model" )
				.key( KEY_OMNIPOSE_MODEL )
				.name( "Pretrained model" )
				.help( "Use one of the pretrained omnipose models." )
				.get();

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

		// Re-add it the arguments at the desired position.
		arguments.remove( modelPretrained );
		arguments.add( 0, modelPretrained );
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
		return "omnipose";
	}

	public static void main( final String[] args )
	{
		final OmniposeCLI cli = new OmniposeCLI( 2, "µm", 0.1 );
		System.out.println( cli );

		// Configure the CLI.
		cli.chan1().set( 2 );
		cli.imageFolder().set( "/Users/tinevez/Desktop" );
		cli.modelPretrained().set( 0 );
		cli.diameter().set( 2. );
		cli.selectPretrainedOrCustom().select( cli.modelPretrained() );

		// Output command line.
		System.out.println( "Command line: " );
		System.out.println( CommandBuilder.build( cli ) );

		// Show config panel.
		final CliConfigPanel panel = CliGuiBuilder.build( cli );
		final JFrame frame = new JFrame( cli.getCommand() + " CLI" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}

}

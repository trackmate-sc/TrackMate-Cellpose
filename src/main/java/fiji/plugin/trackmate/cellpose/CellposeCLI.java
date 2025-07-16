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

	/**
	 * The key to the parameter that stores the path to the custom model file to
	 * use with Cellpose. It must be an absolute file path.
	 */
	public static final String KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH = "CELLPOSE_MODEL_FILEPATH";

	public static final String DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH = "";

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

	/**
	 * They key to the parameter that configures whether Cellpose will try to
	 * use GPU acceleration. For this to work, a working Cellpose with working
	 * GPU support must be present on the system. If not, Cellpose will default
	 * to using the CPU.
	 */
	public static final String KEY_USE_GPU = "USE_GPU";

	public static final Boolean DEFAULT_USE_GPU = Boolean.valueOf( true );

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "CELLPOSE_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Cellpose detector";

	public static final String DOC_CELLPOSE_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-cellpose";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on cellpose to detect objects."
			+ "<p>"
			+ "The detector simply calls an external cellpose installation. So for this "
			+ "to work, you must have a cellpose installation running on your computer. "
			+ "Please follow the instructions from the cellpose website: "
			+ "<u><a href=\"https://github.com/MouseLand/cellpose#local-installation\">https://github.com/MouseLand/cellpose#local-installation</a></u>"
			+ "<p>"
			+ "You will also need to specify the path to the <b>Python executable</b> that can run cellpose "
			+ "or the <b>cellpose executable</b> directly. "
			+ "For instance if you used anaconda to install cellpose, and that you have a "
			+ "Conda environment called 'cellpose', this path will be something along the line of "
			+ "'/opt/anaconda3/envs/cellpose/bin/python'  or 'C:\\\\Users\\\\tinevez\\\\anaconda3\\\\envs\\\\cellpose_biop_gpu\\\\python.exe' "
			+ "If you installed the standalone version, the path to it would something like "
			+ "this on Windows: 'C:\\Users\\tinevez\\Applications\\cellpose.exe'. "
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the Cellpose paper: <a href=\"https://doi.org/10.1038/s41592-020-01018-x\">Stringer, C., Wang, T., Michaelos, M. et al. "
			+ "Cellpose: a generalist algorithm for cellular segmentation. "
			+ "Nat Methods 18, 100–106 (2021)</a>"
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"" + DOC_CELLPOSE_URL + "\">on the ImageJ Wiki</a>."
			+ "</html>";

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

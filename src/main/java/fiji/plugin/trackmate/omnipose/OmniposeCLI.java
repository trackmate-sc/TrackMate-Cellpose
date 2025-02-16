package fiji.plugin.trackmate.omnipose;

import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELL_DIAMETER;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_OPTIONAL_CHANNEL_2;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_USE_GPU;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;
import static fiji.plugin.trackmate.omnipose.OmniposeDetectorFactory.KEY_OMNIPOSE_CUSTOM_MODEL_FILEPATH;
import static fiji.plugin.trackmate.omnipose.OmniposeDetectorFactory.KEY_OMNIPOSE_MODEL;

import java.util.Collections;
import java.util.List;

import fiji.plugin.trackmate.util.cli.CommandBuilder;
import fiji.plugin.trackmate.util.cli.CondaExecutableCLIConfigurator;

public class OmniposeCLI extends CondaExecutableCLIConfigurator
{

	private static final String KEY_PRETRAINED_SELECTED = "PRETRAINED_SELECTED";

	private PathArgument saveDir;

	public OmniposeCLI( final int nChannels, final double pixelSize, final String units )
	{
		// Pretrained model.
		final ChoiceArgument pretrainedModel = addChoiceArgument()
				.addChoice( "Bacteria phase contrast", "bact_phase_omni" )
				.addChoice( "Bacteria fluorescence", "bact_fluor_omni" )
				.defaultValue( 0 )
				.argument( "--pretrained_model" )
				.key( KEY_OMNIPOSE_MODEL )
				.name( "Pretrained model" )
				.help( "Use one of the pretrained omnipose models." )
				.get();

		// Path to a custom model.
		final PathArgument customModel = addPathArgument()
				.argument( "--pretrained_model" )
				.key( KEY_OMNIPOSE_CUSTOM_MODEL_FILEPATH )
				.name( "Custom model" )
				.help( "Path to a custom omnipose model" )
				.get();

		// Select pretrained or custom.
		addSelectableArguments()
				.add( customModel )
				.add( pretrainedModel )
				.key( KEY_PRETRAINED_SELECTED )
				.select( customModel );

		// First channel.
		addIntArgument()
				.name( "Channel to segment" )
				.key( KEY_TARGET_CHANNEL )
				.argument( "--chan" )
				.defaultValue( DEFAULT_TARGET_CHANNEL )
				.min( 1 )
				.max( nChannels )
				.help( "The main channel to segment." )
				.get();

		// Second channel.
		final ChoiceAdder secondChanArgAdder = addChoiceArgument()
				.name( "Optional second channel" )
				.key( KEY_OPTIONAL_CHANNEL_2 )
				.argument( "--chan2" )
				.help( "The main channel to segment." );
		secondChanArgAdder.addChoice( "0: None", "0" );
		for ( int i = 1; i <= nChannels; i++ )
			secondChanArgAdder.addChoice( "" + i );
		secondChanArgAdder.defaultValue( 0 );
		final ChoiceArgument secondChan = secondChanArgAdder.get();
		secondChan.set( 0 );
		// TODO: edit javadoc if this stays: it will be 1-valued.

		// Cell diameter.
		final DoubleArgument cellDiameter = addDoubleArgument()
				.name( "Cell diameter" )
				.argument( "--diameter" )
				.key( KEY_CELL_DIAMETER )
				.defaultValue( 3. )
				.units( units )
				.get();

		// Translate to pixel size.
		translators.put( cellDiameter, d -> {
			final double diam = ( double ) d;
			final double diamPix = diam > 0 ? ( diam / pixelSize ) : 0.;
			return Collections.singletonList( "" + diamPix );
		} );

		// Use GPU.
		this.addFlag()
				.name( "Use GPU" )
				.key( KEY_USE_GPU )
				.argument( "--use_gpu" )
				.defaultValue( true )
				.help( "If true the GPU will be used if it configured. "
						+ "If false, the CPU will be used, and several time-points "
						+ "may be processed in parallel." )
				.get();

		// Simplify contours.
		this.addFlag()
				.name( "Simplify contours" )
				.key( KEY_SIMPLIFY_CONTOURS )
				.defaultValue( true )
				.inCLI( false )
				.help( "If true the object contours will be simplified as smooth polygons." )
				.get();

		// Export results as PNG.
		addFlag()
				.name( "Export results as PNG" )
				.argument( "--save_png" )
				.key( null )
				.visible( false )
				.defaultValue( true )
				.get();

		// Do not save Numpy files.
		addFlag()
				.name( "Do not save Numpy files" )
				.argument( "--no_npy" )
				.key( null )
				.visible( false )
				.defaultValue( true )
				.get();

		// Omni flag.
		addFlag()
				.name( "Omni flag" )
				.argument( "--omni" )
				.key( null )
				.visible( false )
				.defaultValue( true )
				.get();

		// Target save directory.
		saveDir = addPathArgument()
				.name( "Save directory" )
				.argument( "--dir" )
				.key( null )
				.visible( false )
				.get();
	}

	public PathArgument saveDir()
	{
		return saveDir;
	}

	@Override
	protected String getCommand()
	{
		return "omnipose";
	}

	public static void main( final String[] args )
	{
		final OmniposeCLI cli = new OmniposeCLI( 2, 0.1, "µm" );
		cli.saveDir.set( "/Users/tinevez/Desktop" );

		final List< String > tokens = CommandBuilder.build( cli );
		System.out.println( String.join( " ", tokens ) );
	}

}

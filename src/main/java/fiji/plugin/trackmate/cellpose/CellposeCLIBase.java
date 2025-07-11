package fiji.plugin.trackmate.cellpose;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.Collections;

import fiji.plugin.trackmate.util.cli.CommonTrackMateArguments;
import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator;

public abstract class CellposeCLIBase extends CondaCLIConfigurator
{

	/**
	 * The key to the parameter that stores the path the cellpose model to use.
	 * Values depend on whether we use cellpose or omnipose and are strings that
	 * identify the pretrained or custom model.
	 */
	public static final String KEY_CELLPOSE_MODEL = "CELLPOSE_MODEL";

	/**
	 * The key to the parameter that stores the path to the custom model file to
	 * use with cellpose. It must be an absolute file path.
	 */
	public static final String KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH = "CELLPOSE_MODEL_FILEPATH";

	public static final String DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH = "";

	public static final String KEY_CELLPOSE_PRETRAINED_OR_CUSTOM = "PRETRAINED_OR_CUSTOM";

	public static final String DEFAULT_CELLPOSE_PRETRAINED_OR_CUSTOM = KEY_CELLPOSE_MODEL;

	/**
	 * The key to the parameter that store the estimated cell diameter. Contrary
	 * to Cellpose, this must be specified in physical units (e.g. µm) and
	 * TrackMate wil do the conversion. Use 0 or a negative value to have
	 * Cellpose determine this automatically (but it will take a bit longer).
	 */
	public static final String KEY_CELL_DIAMETER = "CELL_DIAMETER";

	public static final Double DEFAULT_CELL_DIAMETER = Double.valueOf( 30. );

	/**
	 * They key to the parameter that configures whether cellpose will try to
	 * use GPU acceleration. For this to work, a working cellpose with working
	 * GPU support must be present on the system. If not, cellpose will default
	 * to using the CPU.
	 */
	public static final String KEY_USE_GPU = "USE_GPU";

	public static final Boolean DEFAULT_USE_GPU = Boolean.valueOf( true );

	private final PathArgument customModelPath;

	private final Flag useGPU;

	private final PathArgument imageFolder;

	private final ChoiceArgument chan1;

	private final DoubleArgument diameter;

	private final Flag simplifyContours;

	public CellposeCLIBase( final int nChannels, final String units, final double pixelSize )
	{
		this.customModelPath = addPathArgument()
				.name( "Path to a custom model" )
				.argument( "--pretrained_model" )
				.required( true )
				.help( "Path to a custom cellpose model file." )
				.defaultValue( DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.key( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.get();

		// Main segmentation channel
		final ChoiceAdder chan1Adder = addChoiceArgument()
				.key( KEY_TARGET_CHANNEL )
				.argument( "--chan" )
				.name( "Target channel" )
				.help( "Index of the channel to segment." )
				.required( true )
				.addChoice( "0 - gray", "0" );
		for ( int c = 1; c <= nChannels; c++ )
			chan1Adder.addChoice( "" + c );
		chan1Adder.defaultValue( 0 );
		this.chan1 = chan1Adder.get();

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
		translators.put( diameter, d -> {
			final double diam = ( double ) d;
			final double diamPix = diam > 0 ? ( diam / pixelSize ) : 0.;
			return Collections.singletonList( "" + diamPix );
		} );

		// Use GPU?
		this.useGPU = addFlag()
				.name( "Use GPU" )
				.help( "Whether to use GPU acceleration, if installed." )
				.argument( "--use_gpu" )
				.key( KEY_USE_GPU )
				.defaultValue( DEFAULT_USE_GPU )
				.get();

		// Simplify contours
		this.simplifyContours = addExtraArgument( CommonTrackMateArguments.simplyContour() );

		// Folder to store input images.
		this.imageFolder = addPathArgument()
				.name( "Input image folder path" )
				.help( "Directory with series of .tif files." )
				.argument( "--dir" )
				.visible( false )
				.required( true )
				.get();

		// Configure cellpose outputs
		addFlag()
				.name( "Save as PNGs" )
				.argument( "--save_png" )
				.defaultValue( true )
				.required( true )
				.inCLI( true )
				.visible( false )
				.get();
		addFlag()
				.name( "Do not save Numpy files" )
				.argument( "--no_npy" )
				.defaultValue( true )
				.required( true )
				.inCLI( true )
				.visible( false )
				.get();
	}

	public ChoiceArgument chan1()
	{
		return chan1;
	}

	public PathArgument customModelPath()
	{
		return customModelPath;
	}

	public PathArgument imageFolder()
	{
		return imageFolder;
	}

	public Flag useGPU()
	{
		return useGPU;
	}

	public DoubleArgument diameter()
	{
		return diameter;
	}

	public Flag simplifyContours()
	{
		return simplifyContours;
	}
}

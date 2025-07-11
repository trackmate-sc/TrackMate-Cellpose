package fiji.plugin.trackmate.cellpose;

import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.DEFAULT_CELL_DIAMETER;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.DEFAULT_USE_GPU;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_CELL_DIAMETER;
import static fiji.plugin.trackmate.cellpose.CellposeDetectorFactory.KEY_USE_GPU;

import java.util.Collections;

import fiji.plugin.trackmate.util.cli.CommonTrackMateArguments;
import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator;

public abstract class CellposeCLIBase extends CondaCLIConfigurator
{

	private final PathArgument customModelPath;

	private final Flag useGPU;

	private final PathArgument imageFolder;

	private final DoubleArgument diameter;

	private final Flag simplifyContours;

	protected final int nChannels;

	protected final String units;

	protected final double pixelSize;

	public CellposeCLIBase( final int nChannels, final String units, final double pixelSize )
	{
		this.nChannels = nChannels;
		this.units = units;
		this.pixelSize = pixelSize;

		this.customModelPath = addPathArgument()
				.name( "Path to a custom model" )
				.argument( "--pretrained_model" )
				.required( true )
				.help( "Path to a custom cellpose model file." )
				.defaultValue( DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.key( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.get();

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
		addFlag()
				.name( "Verbose" )
				.argument( "--verbose" )
				.defaultValue( true )
				.required( true )
				.inCLI( true )
				.visible( false )
				.get();
	}

	@Override
	public abstract String getCommand();

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

package fiji.plugin.trackmate.cellpose;

import javax.swing.ImageIcon;

import org.scijava.ui.config.Parameters.BooleanParam;
import org.scijava.ui.config.Parameters.ChoiceParam;
import org.scijava.ui.config.Parameters.DoubleParam;
import org.scijava.ui.config.Parameters.EnumParam;
import org.scijava.ui.config.Parameters.IntParam;
import org.scijava.ui.config.Parameters.PathParam;

import fiji.plugin.trackmate.TrackMateConfigurator;

public class CellposeBaseConfig< CBM extends Enum< CBM > > extends TrackMateConfigurator
{

	private final EnumParam< CBM > builtinModel;

	private final PathParam customModel;

	private final SelectableParameters builtinOrCustom;

	private final DoubleParam diameter;

	private final DoubleParam flowThreshold;

	private final DoubleParam cellprobThreshold;

	private final IntParam minSize;

	private final BooleanParam do3Dseg;

	private final DoubleParam stitchThreshold;

	private final IntParam flow3DSmooth;

	private final BooleanParam useGpu;

	private final ChoiceParam torchVersion;

	private final BooleanParam simplifyContour;

	private final DoubleParam smoothingScale;

	protected CellposeBaseConfig(
			final String name,
			final String help,
			final Class< CBM > builtinModelEnum,
			final double pixelSize,
			final String units )
	{
		super( name, help );

		// Choice among an enum.
		this.builtinModel = addEnumParameter( builtinModelEnum )
				.key( "BUILTIN_MODEL" )
				.name( "Builtin model" )
				.help( "Select a builtin model to use. " )
				.get();

		// File path.
		this.customModel = addPathParameter()
				.key( "CUSTOM_MODEL_PATH" )
				.defaultValue( "" ) // Better than null.
				.name( "Path to custom model" )
				.help( "Path to a custom Cellpose model. " )
				.get();

		// One or the other, but not both.
		this.builtinOrCustom = addSelectableParameters()
				.key( "BUILTIN_OR_CUSTOM" )
				.add( builtinModel )
				.add( customModel )
				.get();

		// Diameter param is in pixel, but we want to display it in physical
		// units. So we set a translator that converts between the two.

		this.diameter = addDoubleParameter()
				.key( "DIAMETER" )
				.name( "Diameter" )
				.help( "<html>Estimated diameter of objects, in physical units "
						+ "(stored in pixel size internally). " +
						"Set to 0 to let Cellpose estimate it automatically.</html>" )
				.units( units )
				.defaultValue( 30. )
				.min( 0. ) // But no max
				.get();

		setDisplayTranslator( diameter, d -> d * pixelSize, d -> d / pixelSize );

		/*
		 * Advanced parameters.
		 */

		this.flowThreshold = addDoubleParameter()
				.key( "FLOW_THRESHOLD" )
				.name( "Flow threshold" )
				.help( "<html>Threshold for flow error filtering. Lower = more masks (permissive), Higher = fewer masks (strict).</html>" )
				.defaultValue( 0.4 )
				.min( 0. )
				.max( 3. )
				.get();

		this.cellprobThreshold = addDoubleParameter()
				.key( "CELLPROB_THRESHOLD" )
				.name( "Cell probability threshold" )
				.help( "<html>Threshold for cell probability. Increase to filter low-confidence detections.</html>" )
				.defaultValue( 0.0 )
				.min( -6. )
				.max( 6. )
				.get();

		this.minSize = addIntParameter()
				.key( "MIN_SIZE" )
				.name( "Minimum size" )
				.help( "Objects smaller than this are removed." )
				.defaultValue( 15 )
				.min( 0 )
				.units( "pixels" )
				.get();

		addGroup( "Advanced parameters" )
				.add( flowThreshold )
				.add( cellprobThreshold )
				.add( minSize )
				.collapsed( true )
				.get();

		/*
		 * 3D group.
		 */

		this.do3Dseg = addBooleanParameter()
				.key( "MODE_3D" )
				.name( "3D segmentation" )
				.help( "<html>How to handle 3D images for segmentation. "
						+ "If set, will use Cellpose 3D segmentation. "
						+ "Otherwise, segment each 2D plane and stitch objects in Z. See the 'Stitch threshold' parameter."
						+ "</html>" )
				.defaultValue( false )
				.get();

		this.stitchThreshold = addDoubleParameter()
				.key( "STITCH_THRESHOLD" )
				.name( "Stitch threshold" )
				.help( "<html>When in 2D+stitch 3D mode, this threshold is used to decide whether to stitch "
						+ "two objects across planes. Lower = more stitching (permissive), "
						+ "Higher = less stitching (strict).</html>" )
				.defaultValue( 0. )
				.min( 0. )
				.max( 1. )
				.get();

		this.flow3DSmooth = addIntParameter()
				.key( "FLOW_3D_SMOOTH" )
				.name( "3D flow smoothing" )
				.help( "<html>When in 3D mode, smooth flows with gaussian filter of this sigma. "
						+ "Helps smoothing objects masks in 3D. Set to 0 to disable smoothing.</html>" )
				.defaultValue( 0 )
				.min( 0 )
				.get();

		addGroup( "3D options" )
				.add( do3Dseg )
				.add( stitchThreshold )
				.add( flow3DSmooth )
				.collapsed( true )
				.get();

		/*
		 * GPU stuff.
		 */

		this.useGpu = addBooleanParameter()
				.key( "USE_GPU" )
				.name( "Use GPU" )
				.help( "If set, Cellpose will try to use the GPU. If not available, it will fallback to CPU." )
				.defaultValue( true )
				.get();

		this.torchVersion = addChoiceParameter()
				.key( "TORCH_VERSION" )
				.name( "Torch version" )
				.help( "On Windows and Linux, control which torch / cuda version to use." )
				.addChoice( "cpu" )
				.addChoice( "cu126" )
				.addChoice( "cu130" )
				.defaultValue( "cpu" )
				.get();

		addGroup( "GPU options" )
				.add( useGpu )
				.add( torchVersion )
				.collapsed( true )
				.get();

		/*
		 * Smooth contour.
		 */

		this.simplifyContour = addSimplifyContour();
		this.smoothingScale = addSmoothContour( units );

		addGroup( "Output smoothing" )
				.add( simplifyContour )
				.add( smoothingScale )
				.collapsed( false )
				.get();

		/*
		 * Icons.
		 */

		addIcon( new ImageIcon( this.getClass().getResource( "/images/cellposelogo.png" ) ).getImage() );
		addIcon( new ImageIcon( this.getClass().getResource( "/images/favicon.ico" ) ).getImage() );
	}

	public EnumParam< CBM > builtinModel()
	{
		return builtinModel;
	}

	public PathParam customModel()
	{
		return customModel;
	}

	public SelectableParameters builtinOrCustom()
	{
		return builtinOrCustom;
	}

	public DoubleParam diameter()
	{
		return diameter;
	}

	public DoubleParam flowThreshold()
	{
		return flowThreshold;
	}

	public DoubleParam cellprobThreshold()
	{
		return cellprobThreshold;
	}

	public IntParam minSize()
	{
		return minSize;
	}

	public BooleanParam mode3D()
	{
		return do3Dseg;
	}

	public DoubleParam stitchThreshold()
	{
		return stitchThreshold;
	}

	public IntParam flow3DSmooth()
	{
		return flow3DSmooth;
	}

	public BooleanParam useGpu()
	{
		return useGpu;
	}

	public ChoiceParam torchVersion()
	{
		return torchVersion;
	}

	public BooleanParam simplifyContour()
	{
		return simplifyContour;
	}

	public DoubleParam smoothingScale()
	{
		return smoothingScale;
	}
}

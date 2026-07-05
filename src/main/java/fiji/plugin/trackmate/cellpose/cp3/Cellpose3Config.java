package fiji.plugin.trackmate.cellpose.cp3;

import org.scijava.ui.config.Parameters.IntParam;

import fiji.plugin.trackmate.cellpose.CellposeBaseConfig;
import net.imglib2.cellpose.Cellpose3BuiltinModels;

public class Cellpose3Config extends CellposeBaseConfig< Cellpose3BuiltinModels >
{

	static final String BUILTIN_MODEL_KEY = "CELLPOSE_3_BUILTIN_MODEL";

	private final IntParam chan1;

	private final IntParam chan2;

	public Cellpose3Config( final int nChannels, final double pixelSize, final String units )
	{
		super(
				Cellpose3DetectorFactory.NAME,
				Cellpose3DetectorFactory.DOC_CELLPOSE_URL,
				BUILTIN_MODEL_KEY,
				Cellpose3BuiltinModels.class,
				pixelSize,
				units );

		// Channels, two int params.
		this.chan1 = addIntParameter()
				.key( "CHAN1" )
				.name( "Main channel" )
				.help( "The main channel to segment. Select 0 to use a grayscale blend of all channels." )
				.defaultValue( 1 )
				.min( 0 )
				.max( nChannels )
				.get();
		this.chan2 = addIntParameter()
				.key( "CHAN2" )
				.name( "Optional channel" )
				.help( "The second channel to segment. Select 0 to skip using a second channel." )
				.defaultValue( 0 )
				.min( 0 )
				.max( nChannels )
				.get();

		// Change their order.
		reorder( chan1, 2 );
		reorder( chan2, 3 );
	}

	public IntParam chan1()
	{
		return chan1;
	}

	public IntParam chan2()
	{
		return chan2;
	}
}
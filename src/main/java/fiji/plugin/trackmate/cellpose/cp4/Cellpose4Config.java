package fiji.plugin.trackmate.cellpose.cp4;

import org.scijava.ui.config.Parameters.IntParam;

import fiji.plugin.trackmate.cellpose.CellposeBaseConfig;
import net.imglib2.cellpose.Cellpose4BuiltinModels;

public class Cellpose4Config extends CellposeBaseConfig< Cellpose4BuiltinModels >
{

	static final String BUILTIN_MODEL_KEY = "CELLPOSE_SAM_BUILTIN_MODEL";

	private final IntParam chan1;

	private final IntParam chan2;

	private final IntParam chan3;

	public Cellpose4Config( final int nChannels, final double pixelSize, final String units )
	{
		super(
				Cellpose4DetectorFactory.NAME,
				Cellpose4DetectorFactory.DOC_CELLPOSE_URL,
				BUILTIN_MODEL_KEY,
				Cellpose4BuiltinModels.class,
				pixelSize, units );

		// Channels, two int params.
		this.chan1 = addIntParameter()
				.key( "CHAN1" )
				.name( "Channel 1" )
				.help( "First channel to include in the segmentation. Select 0 not use it." )
				.defaultValue( 1 )
				.min( 0 )
				.max( nChannels )
				.get();
		this.chan2 = addIntParameter()
				.key( "CHAN2" )
				.name( "Channel 2" )
				.help( "Second channel to include in the segmentation. Select 0 not use it." )
				.defaultValue( 0 )
				.min( 0 )
				.max( nChannels )
				.get();
		this.chan3 = addIntParameter()
				.key( "CHAN3" )
				.name( "Channel 3" )
				.help( "Third channel to include in the segmentation. Select 0 not use it." )
				.defaultValue( 0 )
				.min( 0 )
				.max( nChannels )
				.get();

		// Change their order.
		orderedElements.remove( chan1 );
		orderedElements.add( 2, chan1 );
		orderedElements.remove( chan2 );
		orderedElements.add( 3, chan2 );
		orderedElements.remove( chan3 );
		orderedElements.add( 4, chan3 );
	}

	public IntParam chan1()
	{
		return chan1;
	}

	public IntParam chan2()
	{
		return chan2;
	}

	public IntParam chan3()
	{
		return chan3;
	}
}
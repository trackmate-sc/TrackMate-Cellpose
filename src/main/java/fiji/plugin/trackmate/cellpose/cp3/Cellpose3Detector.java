package fiji.plugin.trackmate.cellpose.cp3;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import fiji.plugin.trackmate.cellpose.AbstractCellposeDetector;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.appose.ShmImg;
import net.imglib2.cellpose.ApposeTaskListener;
import net.imglib2.cellpose.AxisInfo;
import net.imglib2.cellpose.Cellpose;
import net.imglib2.cellpose.Cellpose3Parameters;
import net.imglib2.cellpose.CellposeParameters;
import net.imglib2.cellpose.CellposeRunner;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class Cellpose3Detector< T extends RealType< T > & NativeType< T > > extends AbstractCellposeDetector< T, Cellpose3Config >
{

	public Cellpose3Detector( final ImgPlus< T > img, final Interval interval, final Cellpose3Config config )
	{
		super( img, interval, config );
	}

	@Override
	protected CellposeRunner< T, UnsignedShortType > createRunner( final CellposeParameters params, final ApposeTaskListener listener, final ShmImg< T > inputShmImg, final AxisInfo axisInfo, final ShmImg< UnsignedShortType > outputShmImg ) throws BuildException, IOException, InterruptedException, TaskException
	{
		return Cellpose.cellposeRunner( ( Cellpose3Parameters ) params, listener, inputShmImg, axisInfo, outputShmImg, null );
	}

	@Override
	protected Cellpose3Parameters toParams( final Cellpose3Config config )
	{
		final List< Integer > channels = Arrays.asList(
				config.chan1().getValue(),
				config.chan2().getValue() );

		final String selection = config.builtinOrCustom().getSelection().getKey();
		final boolean isBuiltin = selection.equals( "BUILTIN_MODEL" );

		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( isBuiltin ? config.builtinModel().getValue() : null )
				.customModel( isBuiltin ? null : config.customModel().getValue() )
				.diameter( config.diameter().getValue() )
				.channels( channels )
				.minSize( config.minSize().getValue() )
				.resample( true ) // Must be true here, as we expect the output
									// to have the same size as the input.
				.cellProbThreshold( config.flowThreshold().getValue() )
				.flowThreshold( config.flowThreshold().getValue() )
				.do3D( config.mode3D().getValue() )
				.stitchThreshold( config.stitchThreshold().getValue() )
				.flow3dSmooth( config.flow3DSmooth().getValue() )
				.torchVersion( config.torchVersion().getValue() )
				.useGpu( config.useGpu().getValue() )
				.build();
		return params;
	}
}

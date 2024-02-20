package fiji.plugin.trackmate.cellpose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ij.IJ;

public abstract class AbstractCellposeSettings
{

	/**
	 * Interface for enums that represent a pretrained or a custom model in
	 * cellpose or omnipose.
	 */
	public interface PretrainedModel
	{

		/**
		 * Returns <code>true</code> if this model is a custom model.
		 * 
		 * @return <code>true</code> if this model is a custom model.
		 */
		boolean isCustom();

		/**
		 * Returns the name of this pretrained model, or its path if it is a
		 * custom model.
		 * 
		 * @return the model name or path.
		 */
		String getPath();

	}

	public final String executablePath;

	public final int chan;

	public final int chan2;

	public final String customModelPath;

	public final double diameter;

	public final boolean useGPU;

	public final boolean simplifyContours;

	private final PretrainedModel model;

	protected AbstractCellposeSettings(
			final String executablePath,
			final PretrainedModel model,
			final String customModelPath,
			final int chan,
			final int chan2,
			final double diameter,
			final boolean useGPU,
			final boolean simplifyContours )
	{
		this.executablePath = executablePath;
		this.model = model;
		this.chan = chan;
		this.chan2 = chan2;
		this.customModelPath = customModelPath;
		this.diameter = diameter;
		this.useGPU = useGPU;
		this.simplifyContours = simplifyContours;
	}

	/**
	 * Returns the executable name of the cellpose or omnipose command. For
	 * cellpose, it's simply 'cellpose'.
	 * 
	 * @return the executable name.
	 */
	public abstract String getExecutableName();

	public List< String > toCmdLine( final String imagesDir )
	{
		final List< String > cmd = new ArrayList<>();

		/*
		 * First decide whether we are calling Cellpose from python, or directly
		 * the Cellpose executable. We check the last part of the path to check
		 * whether this is python or cellpose.
		 */
		final String[] split = executablePath.replace( "\\", "/" ).split( "/" );
		final String lastItem = split[ split.length - 1 ];
		if ( lastItem.toLowerCase().startsWith( "python" ) )
		{
			// Activate conda env if it runs in Windows.
			if ( IJ.isWindows() )
			{
				final String envname = split[ split.length - 2 ];
				cmd.addAll( Arrays.asList( "cmd.exe", "/c", "conda", "activate", envname ) );
				cmd.add( "&" );
			}

			// Calling Cellpose from python.
			cmd.add( executablePath );
			cmd.add( "-m" );
			cmd.add( getExecutableName() );
		}
		else
		{
			// Calling Cellpose executable.
			cmd.add( executablePath );
		}

		/*
		 * Cellpose command line arguments.
		 */

		// Target dir.
		cmd.add( "--dir" );
		cmd.add( imagesDir );

		// First channel.
		cmd.add( "--chan" );
		cmd.add( "" + (chan) );

		// Second channel.
		if ( chan2 >= 0 )
		{
			cmd.add( "--chan2" );
			cmd.add( "" + chan2 );
		}

		// GPU.
		if ( useGPU )
			cmd.add( "--use_gpu" );

		// Diameter.
		cmd.add( "--diameter" );
		cmd.add( ( diameter > 0 ) ? "" + diameter : "0" );

		// Model.
		cmd.add( "--pretrained_model" );
		if ( model.isCustom() )
			cmd.add( customModelPath );
		else
			cmd.add( model.getPath() );

		// Export results as PNG.
		cmd.add( "--save_png" );

		// Do not save Numpy files.
		cmd.add( "--no_npy" );

		return Collections.unmodifiableList( cmd );
	}
}

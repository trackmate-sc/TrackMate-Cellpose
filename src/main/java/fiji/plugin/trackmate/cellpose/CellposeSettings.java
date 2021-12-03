package fiji.plugin.trackmate.cellpose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CellposeSettings
{

	public final String cellposePythonPath;
	
	public final int chan;

	public final int chan2;

	public final PretrainedModel model;

	public final double diameter;

	public final boolean simplifyContours;

	public CellposeSettings(
			final String cellposePythonPath,
			final PretrainedModel model,
			final int chan,
			final int chan2,
			final double diameter,
			final boolean simplifyContours )
	{
		this.cellposePythonPath = cellposePythonPath;
		this.model = model;
		this.chan = chan;
		this.chan2 = chan2;
		this.diameter = diameter;
		this.simplifyContours = simplifyContours;
	}

	public List< String > toCmdLine( final String imagesDir )
	{
		final List< String > cmd = new ArrayList<>();
		// Calling Cellpose from python.
		cmd.add( cellposePythonPath );
		cmd.add( "-m" );
		cmd.add( "cellpose" );
		// Cellpose command line arguments.
		cmd.add( "--dir" );
		cmd.add( imagesDir );
		cmd.add( "--chan" );
		cmd.add( "" + chan );
		if ( chan2 >= 0 )
		{
			cmd.add( "--chan2" );
			cmd.add( "" + chan2 );
		}
		cmd.add( "--diameter" );
		cmd.add( ( diameter > 0 ) ? "" + diameter : "0" );
		cmd.add( "--pretrained_model" );
		cmd.add( model.path );
		cmd.add( "--save_png" );
		return Collections.unmodifiableList( cmd );
	}

	public static Builder create()
	{
		return new Builder();
	}

	public static final class Builder
	{

		private String cellposePythonPath = "/opt/anaconda3/envs/cellpose/bin/python";

		private int chan = 0;

		private int chan2 = -1;

		private PretrainedModel model = PretrainedModel.CYTO;

		private double diameter = 30.;

		private boolean simplifyContours = true;

		public Builder channel1( final int ch )
		{
			this.chan = ch;
			return this;
		}

		public Builder channel2( final int ch )
		{
			this.chan2 = ch;
			return this;
		}

		public Builder cellposePythonPath( final String cellposePythonPath )
		{
			this.cellposePythonPath = cellposePythonPath;
			return this;
		}

		public Builder model( final PretrainedModel model )
		{
			this.model = model;
			return this;
		}

		public Builder diameter( final double diameter )
		{
			this.diameter = diameter;
			return this;
		}

		public Builder simplifyContours( final boolean simplifyContours )
		{
			this.simplifyContours = simplifyContours;
			return this;
		}

		public CellposeSettings get()
		{
			return new CellposeSettings( cellposePythonPath, model, chan, chan2, diameter, simplifyContours );
		}
	}

	public enum PretrainedModel
	{
		CYTO( "Cytoplasm", "cyto" ),
		NUCLEI( "Nucleus", "nuclei" );

		private final String name;

		private final String path;

		PretrainedModel( final String name, final String path )
		{
			this.name = name;
			this.path = path;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public String cellposeName()
		{
			return path;
		}
	}

	public static final CellposeSettings DEFAULT = new Builder().get();

}
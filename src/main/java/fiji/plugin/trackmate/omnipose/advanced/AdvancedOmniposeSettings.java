package fiji.plugin.trackmate.omnipose.advanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fiji.plugin.trackmate.omnipose.OmniposeSettings;

public class AdvancedOmniposeSettings extends OmniposeSettings
{
	private final double flowThreshold;

	private final double cellProbThreshold;

	public AdvancedOmniposeSettings(
			final String omniposePythonPath,
			final PretrainedModelOmnipose model,
			final String customModelPath,
			final int chan,
			final int chan2,
			final double diameter,
			final boolean useGPU,
			final boolean simplifyContours,
			final double flowThreshold,
			final double cellProbThreshold)
	{
		super( omniposePythonPath, model, customModelPath, chan, chan2, diameter, useGPU, simplifyContours );
		this.flowThreshold = flowThreshold;
		this.cellProbThreshold = cellProbThreshold;
	}

	@Override
	public List< String > toCmdLine( final String imagesDir )
	{
		final List< String > cmd = new ArrayList<>( super.toCmdLine( imagesDir ) );
		cmd.add( "--flow_threshold" );
		cmd.add( String.valueOf( flowThreshold ) );
		/*
		 * Careful! Because omnipose is still based on cellpose 1, the
		 * cellprob_threshold parameter is still called mask_threshold.
		 */
		cmd.add( "--mask_threshold" );
		cmd.add( String.valueOf( cellProbThreshold ) );
                               
                return Collections.unmodifiableList( cmd );
	}

	public static Builder create()
	{
		return new Builder();
	}

	public static final class Builder extends OmniposeSettings.Builder
	{
                
		private double flowThreshold = 0.4;

		private double cellProbThreshold = 0.0;

		public Builder flowThreshold( final double flowThreshold )
		{
			this.flowThreshold = flowThreshold;
			return this;
		}

		public Builder cellProbThreshold( final double cellProbThreshold )
		{
			this.cellProbThreshold = cellProbThreshold;
			return this;
		}

		@Override
		public Builder channel1( final int ch )
		{
			super.channel1( ch );
			return this;
		}

		@Override
		public Builder channel2( final int ch )
		{
			super.channel2( ch );
			return this;
		}

		@Override
		public Builder omniposePythonPath( final String cellposePythonPath )
		{
			super.omniposePythonPath( cellposePythonPath );
			return this;
		}

		@Override
		public Builder model( final PretrainedModelOmnipose model )
		{
			super.model( model );
			return this;
		}

		@Override
		public Builder diameter( final double diameter )
		{
			super.diameter( diameter );
			return this;
		}

		@Override
		public Builder useGPU( final boolean useGPU )
		{
			super.useGPU( useGPU );
			return this;
		}

		@Override
		public Builder simplifyContours( final boolean simplifyContours )
		{
			super.simplifyContours( simplifyContours );
			return this;
		}

		@Override
		public Builder customModel( final String customModelPath )
		{
			super.customModel( customModelPath );
			return this;
		}

		@Override
		public AdvancedOmniposeSettings get()
		{
			return new AdvancedOmniposeSettings(
					omniposePythonPath,
					model,
					customModelPath,
					chan,
					chan2,
					diameter,
					useGPU,
					simplifyContours,
					flowThreshold,
					cellProbThreshold);
		}
	}
}

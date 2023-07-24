package fiji.plugin.trackmate.cellpose.advanced;

import fiji.plugin.trackmate.cellpose.CellposeSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdvancedCellposeSettings extends CellposeSettings
{
	private final double flowThreshold;

	private final double cellProbThreshold;

	public AdvancedCellposeSettings(
			String cellposePythonPath,
			PretrainedModelCellpose model,
			String customModelPath,
			int chan,
			int chan2,
			double diameter,
			boolean useGPU,
			boolean simplifyContours,
			double flowThreshold,
			double cellProbThreshold )
	{
		super( cellposePythonPath, model, customModelPath, chan, chan2, diameter, useGPU, simplifyContours );
		this.flowThreshold = flowThreshold;
		this.cellProbThreshold = cellProbThreshold;
	}

	@Override
	public List< String > toCmdLine( String imagesDir )
	{
		List< String > cmd = new ArrayList<>(super.toCmdLine( imagesDir ));
		cmd.add( "--flow_threshold" );
		cmd.add( String.valueOf( flowThreshold ) );
		cmd.add( "--cellprob_threshold" );
		cmd.add( String.valueOf( cellProbThreshold ) );
		return Collections.unmodifiableList( cmd );
	}

	public static Builder create()
	{
		return new Builder();
	}

	public static final class Builder extends CellposeSettings.Builder
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

		public Builder channel1( final int ch )
		{
			super.channel1( ch );
			return this;
		}

		public Builder channel2( final int ch )
		{
			super.channel2( ch );
			return this;
		}

		public Builder cellposePythonPath( final String cellposePythonPath )
		{
			super.cellposePythonPath( cellposePythonPath );
			return this;
		}

		public Builder model( final PretrainedModelCellpose model )
		{
			super.model( model );
			return this;
		}

		public Builder diameter( final double diameter )
		{
			super.diameter( diameter );
			return this;
		}

		public Builder useGPU( final boolean useGPU )
		{
			super.useGPU( useGPU );
			return this;
		}

		public Builder simplifyContours( final boolean simplifyContours )
		{
			super.simplifyContours( simplifyContours );
			return this;
		}

		public Builder customModel( final String customModelPath )
		{
			super.customModel( customModelPath );
			return this;
		}

		@Override
		public AdvancedCellposeSettings get()
		{
			return new AdvancedCellposeSettings(
					cellposePythonPath,
					model,
					customModelPath,
					chan,
					chan2,
					diameter,
					useGPU,
					simplifyContours,
					flowThreshold,
					cellProbThreshold );
		}
	}
}

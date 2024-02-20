package fiji.plugin.trackmate.cellpose.advanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fiji.plugin.trackmate.cellpose.CellposeSettings;

public class AdvancedCellposeSettings extends CellposeSettings
{
	private final double flowThreshold;

	private final double cellProbThreshold;

	private final boolean resample; // compute dynamics at original image size
        
        private final double cellMinSize; // minimum size of masks to keep them in CP
        
	public AdvancedCellposeSettings(
			final String cellposePythonPath,
			final PretrainedModelCellpose model,
			final String customModelPath,
			final int chan,
			final int chan2,
			final double diameter,
			final boolean useGPU,
			final boolean simplifyContours,
			final double flowThreshold,
			final double cellProbThreshold,
                        final double cellMinSize,
//                        final boolean do2DZ,
//                        final double iouThreshold,
                        final boolean resample )
	{
		super( cellposePythonPath, model, customModelPath, chan, chan2, diameter, useGPU, simplifyContours );
		this.flowThreshold = flowThreshold;
		this.cellProbThreshold = cellProbThreshold;
                this.cellMinSize = cellMinSize;
//                this.do2DZ = do2DZ;
//                this.iouThreshold = iouThreshold;
                this.resample = resample;
	}

	@Override
	public List< String > toCmdLine( final String imagesDir )
	{
		final List< String > cmd = new ArrayList<>(super.toCmdLine( imagesDir ));
		cmd.add( "--flow_threshold" );
		cmd.add( String.valueOf( flowThreshold ) );
		cmd.add( "--cellprob_threshold" );
		cmd.add( String.valueOf( cellProbThreshold ) );
                cmd.add( "--min_size" );
		cmd.add( String.valueOf( (int) cellMinSize ) );
//                if ( is3D )
//                {
//                    if ( do2DZ )
//                    {
//                        cmd.remove("--do_3D");
//                        cmd.add("--stitch_threshold");
//                        cmd.add( String.valueOf( iouThreshold ) );
//                    }
//                    //else
//                   // {
//                   // cmd.add("--do_3D");
//                   // }
//                }
                if ( !resample )
                    cmd.add( "--no_resample");
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
                
                private double cellMinSize = 15;  // CellPose default min_size value
                
//                private boolean do2DZ = false; // cellPose 3D mode (xy, yz, zy or 2D+stitch)
//                
//                private double iouThreshold = 0.25;
                
                private boolean resample = true; // CellPose resample parameters: if dynamics are computed at original size (slower but more accurate)

                public Builder resample( final boolean resample )
		{
			this.resample = resample;
			return this;
		}

                
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
                
                public Builder cellMinSize( final double cMinSize )
		{
			this.cellMinSize = cMinSize;
			return this;
		}
                           
//		public Builder do2DZ( final boolean do2DZ )
//		{
//			this.do2DZ = do2DZ;
//                        return this;
//		}
//                
//                public Builder iouThreshold( final double iouThreshold )
//		{
//			this.iouThreshold = iouThreshold;
//                        return this;
//		}

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
		public Builder cellposePythonPath( final String cellposePythonPath )
		{
			super.cellposePythonPath( cellposePythonPath );
			return this;
		}

		@Override
		public Builder model( final PretrainedModelCellpose model )
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
					cellProbThreshold,
                                        cellMinSize,
//                                        do2DZ,
//                                        iouThreshold,
                                       resample );
		}
	}
}

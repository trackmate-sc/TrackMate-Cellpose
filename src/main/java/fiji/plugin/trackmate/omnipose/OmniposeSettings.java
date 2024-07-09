/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2023 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.omnipose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fiji.plugin.trackmate.cellpose.AbstractCellposeSettings;

public class OmniposeSettings extends AbstractCellposeSettings
{       

	public OmniposeSettings(
			final String omniposePythonPath,
			final PretrainedModelOmnipose model,
			final String customModelPath,
			final int chan,
			final int chan2,
			final double diameter,
			final boolean useGPU,
			final boolean simplifyContours )
	{
		super( omniposePythonPath, model, customModelPath, chan, chan2, diameter, useGPU, simplifyContours );
	}

	@Override
	public List< String > toCmdLine( final String imagesDir )
	{
		final List< String > cmd = new ArrayList<>( super.toCmdLine( imagesDir ) );
		// omnipose executable adds it anyway, but let's make sure.
                cmd.add( "--omni" );
                
                cmd.add( "--nchan" );
		cmd.add( String.valueOf( 1 ) ); // Only segment with --nchan to 1, and save only the channel to segment in temp directory
		
		return Collections.unmodifiableList( cmd );
	}

	@Override
	public String getExecutableName()
	{
		return "omnipose";
	}

	public static Builder create()
	{
		return new Builder();
	}

	public static class Builder
	{

		protected String omniposePythonPath = "/opt/anaconda3/envs/omnipose/bin/python";

		protected int chan = 0;

		protected int chan2 = -1;

		protected PretrainedModelOmnipose model = PretrainedModelOmnipose.BACT_PHASE;

		protected double diameter = 30.;
		
		protected boolean useGPU = true;
		
		protected boolean simplifyContours = true;

		protected String customModelPath = "";

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

		public Builder omniposePythonPath( final String omniposePythonPath )
		{
			this.omniposePythonPath = omniposePythonPath;
			return this;
		}

		public Builder model( final PretrainedModelOmnipose model )
		{
			this.model = model;
			return this;
		}

		public Builder diameter( final double diameter )
		{
			this.diameter = diameter;
			return this;
		}

		public Builder useGPU( final boolean useGPU )
		{
			this.useGPU = useGPU;
			return this;
		}

		public Builder simplifyContours( final boolean simplifyContours )
		{
			this.simplifyContours = simplifyContours;
			return this;
		}

		public Builder customModel( final String customModelPath )
		{
			this.customModelPath = customModelPath;
			return this;
		}

		public OmniposeSettings get()
		{
			return new OmniposeSettings(
					omniposePythonPath,
					model,
					customModelPath,
					chan,
					chan2,
					diameter,
					useGPU,
					simplifyContours );
		}

	}

	public enum PretrainedModelOmnipose implements PretrainedModel
	{
		BACT_PHASE( "Bacteria phase contrast", "bact_phase_omni", false ),
		BACT_FLUO( "Bacteria fluorescence", "bact_fluor_omni", false ),
		CUSTOM( "Custom", "", true );

		private final String name;

		private final String path;

		private final boolean isCustom;

		PretrainedModelOmnipose( final String name, final String path, final boolean isCustom )
		{
			this.name = name;
			this.path = path;
			this.isCustom = isCustom;
		}

		@Override
		public String toString()
		{
			return name;
		}

		@Override
		public boolean isCustom()
		{
			return isCustom;
		}

		@Override
		public String getPath()
		{
			return path;
		}

	}

	public static final OmniposeSettings DEFAULT = new Builder().get();
}

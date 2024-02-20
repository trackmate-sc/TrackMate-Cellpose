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
package fiji.plugin.trackmate.cellpose;

public class CellposeSettings extends AbstractCellposeSettings
{

	public CellposeSettings(
			final String cellposePythonPath,
			final PretrainedModelCellpose model,
			final String customModelPath,
			final int chan,
			final int chan2,
			final double diameter,
			final boolean useGPU,
			final boolean simplifyContours )
	{
		super( cellposePythonPath, model, customModelPath, chan, chan2, diameter, useGPU, simplifyContours );
	}

	@Override
	public String getExecutableName()
	{
		return "cellpose";
	}

	public static Builder create()
	{
		return new Builder();
	}

	public static class Builder
	{

		protected String cellposePythonPath = "/opt/anaconda3/envs/cellpose/bin/python";

		protected int chan = 1;

		protected int chan2 = 0;

		protected PretrainedModelCellpose model = PretrainedModelCellpose.CYTO;

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

		public Builder cellposePythonPath( final String cellposePythonPath )
		{
			this.cellposePythonPath = cellposePythonPath;
			return this;
		}

		public Builder model( final PretrainedModelCellpose model )
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

		public CellposeSettings get()
		{
			return new CellposeSettings(
					cellposePythonPath,
					model,
					customModelPath,
					chan,
					chan2,
					diameter,
					useGPU,
					simplifyContours );
		}
	}

	public enum PretrainedModelCellpose implements PretrainedModel
	{
		CYTO( "Cytoplasm", "cyto", false ),
		NUCLEI( "Nucleus", "nuclei", false ),
		CYTO2( "Cytoplasm 2.0", "cyto2", false ),
                LIVECELL( "Live cell", "livecell", false ),
                TISSUNET( "TissueNet", "tissuenet", false ),
                CPX( "CPx", "CPx", false ),
		CUSTOM( "Custom", "", true );

		private final String name;

		final String path;

		private final boolean isCustom;

		PretrainedModelCellpose( final String name, final String path, final boolean isCustom )
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

	public static final CellposeSettings DEFAULT = new Builder().get();
}

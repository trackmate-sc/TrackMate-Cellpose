/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2026 TrackMate developers.
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

import fiji.plugin.trackmate.util.cli.CommonTrackMateArguments;
import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator;

public abstract class CellposeCLIBase extends CondaCLIConfigurator
{

	/**
	 * The key to the parameter that stores the path to the custom model file to
	 * use with Cellpose. It must be an absolute file path.
	 */
	public static final String KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH = "CELLPOSE_MODEL_FILEPATH";

	public static final String DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH = "";

	/**
	 * They key to the parameter that configures whether cellpose will try to
	 * use GPU acceleration. For this to work, a working cellpose with working
	 * GPU support must be present on the system. If not, cellpose will default
	 * to using the CPU.
	 */
	public static final String KEY_USE_GPU = "USE_GPU";

	public static final Boolean DEFAULT_USE_GPU = Boolean.valueOf( true );

	private final PathArgument customModelPath;

	private final Flag useGPU;

	private final PathArgument imageFolder;

	private final Flag simplifyContours;

	protected final int nChannels;

	protected final String units;

	protected final double pixelSize;

	public CellposeCLIBase( final int nChannels, final String units, final double pixelSize )
	{
		this.nChannels = nChannels;
		this.units = units;
		this.pixelSize = pixelSize;

		this.customModelPath = addPathArgument()
				.name( "Path to a custom model" )
				.argument( "--pretrained_model" )
				.required( true )
				.help( "Path to a custom cellpose model file." )
				.defaultValue( DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.key( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.get();

		// Use GPU?
		this.useGPU = addFlag()
				.name( "Use GPU" )
				.help( "Whether to use GPU acceleration, if installed." )
				.argument( "--use_gpu" )
				.key( KEY_USE_GPU )
				.defaultValue( DEFAULT_USE_GPU )
				.get();

		// Simplify contours
		this.simplifyContours = CommonTrackMateArguments.addSimplifyContour( this );

		// Folder to store input images.
		this.imageFolder = addPathArgument()
				.name( "Input image folder path" )
				.help( "Directory with series of .tif files." )
				.argument( "--dir" )
				.visible( false )
				.required( true )
				.get();

		// Configure cellpose outputs
		addFlag()
				.name( "Save as PNGs" )
				.argument( "--save_png" )
				.defaultValue( true )
				.required( true )
				.inCLI( true )
				.visible( false )
				.get();
		addFlag()
				.name( "Do not save Numpy files" )
				.argument( "--no_npy" )
				.defaultValue( true )
				.required( true )
				.inCLI( true )
				.visible( false )
				.get();
		addFlag()
				.name( "Verbose" )
				.argument( "--verbose" )
				.defaultValue( true )
				.required( true )
				.inCLI( true )
				.visible( false )
				.get();
	}

	@Override
	public abstract String getCommand();

	public PathArgument customModelPath()
	{
		return customModelPath;
	}

	public PathArgument imageFolder()
	{
		return imageFolder;
	}

	public Flag useGPU()
	{
		return useGPU;
	}

	public Flag simplifyContours()
	{
		return simplifyContours;
	}
}

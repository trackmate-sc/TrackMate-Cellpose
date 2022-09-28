/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2021 - 2022 The Institut Pasteur.
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
package fiji.plugin.trackmate;

import java.io.IOException;

import fiji.plugin.trackmate.cellpose.CellposeDetector;
import fiji.plugin.trackmate.cellpose.CellposeSettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;

/**
 * Inspired by the BIOP approach.
 */
public class CellPoseAttempt2
{

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static void main( final String[] args ) throws IOException, InterruptedException
	{
		ImageJ.main( args );

//		final ImagePlus imp = IJ.openImage( "samples/P31-crop-2.tif" );
		final ImagePlus imp = IJ.openImage( "D:/IAH/Projects/MLenormand_YeastCellsTracking/Data/Dataset-20220909/pos0_gfp_maxz_c2-t1-t3.tif" );
		imp.show();
		
		// Cellpose command line options.
//		final CellposeSettings cp = CellposeSettings.DEFAULT;
		
		CellposeSettings cp = CellposeSettings.create()					
//			.cellposePythonPath("C:/Users/mphan/Desktop/testcellpose/cellpose.exe")
			.cellposePythonPath("C:/Users/mphan/Anaconda3/envs/cellpose/python.exe")
			.channel1(0)
			.channel2(0)
			.diameter(5.)
			.model(CellposeSettings.PretrainedModel.NUCLEI)
			.useGPU(true)
			.simplifyContours(true)
			.get();

		
		final ImgPlus img = TMUtils.rawWraps( imp );
		final CellposeDetector detector = new CellposeDetector( img, img, cp, Logger.DEFAULT_LOGGER );
		if ( !detector.checkInput() )
		{
			System.err.println( detector.getErrorMessage() );
			return;
		}
		
		if ( !detector.process() )
		{
			System.err.println( detector.getErrorMessage() );
			return;
		}
		
		System.out.println( String.format( "Done in %.1f s.", detector.getProcessingTime() / 1000. ) );
		final SpotCollection spots = detector.getResult();
		spots.setVisible( true );
		System.out.println( spots );

		final Model model = new Model();
		model.setSpots( spots, false );
		final SelectionModel selectionModel = new SelectionModel( model );
		
		final HyperStackDisplayer displayer = new HyperStackDisplayer( model, selectionModel, imp, DisplaySettingsIO.readUserDefault() );
		displayer.render();
	}
}

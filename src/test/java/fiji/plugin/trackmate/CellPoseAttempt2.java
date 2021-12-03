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

		final ImagePlus imp = IJ.openImage( "samples/P31-crop-2.tif" );
		imp.show();
		
		// Cellpose command line options.
		final CellposeSettings cp = CellposeSettings.DEFAULT;
		
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

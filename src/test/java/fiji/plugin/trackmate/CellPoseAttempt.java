package fiji.plugin.trackmate;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ij.ImageJ;

/**
 * Inspired by the BIOP approach.
 */
public class CellPoseAttempt
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );

		new TrackMatePlugIn().run( "samples/P31-crop-2.tif" );
	}
}

package fiji.plugin.trackmate;

import ij.ImageJ;

public class LoadCellposeResultsDemo
{

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new LoadTrackMatePlugIn().run( "" );
	}
}

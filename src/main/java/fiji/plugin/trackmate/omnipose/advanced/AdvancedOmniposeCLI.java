/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2025 TrackMate developers.
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
package fiji.plugin.trackmate.omnipose.advanced;

import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeCLI.DEFAULT_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeCLI.DEFAULT_FLOW_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeCLI.DEFAULT_NO_RESAMPLE;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeCLI.KEY_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeCLI.KEY_FLOW_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeCLI.KEY_NO_RESAMPLE;

import javax.swing.JFrame;

import fiji.plugin.trackmate.omnipose.OmniposeCLI;
import fiji.plugin.trackmate.util.cli.CommandBuilder;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder.ConfigPanel;

public class AdvancedOmniposeCLI extends OmniposeCLI
{

	private final DoubleArgument flowThreshold;

	private final DoubleArgument cellProbThreshold;

	private final Flag noResample;

	public AdvancedOmniposeCLI( final int nChannels, final String units, final double pixelSize )
	{
		super( nChannels, units, pixelSize );

		// Flow threshold
		this.flowThreshold = addDoubleArgument()
				.name( "Flow threshold" )
				.argument( "--flow_threshold" )
				.help( "<html>"
						+ "Increase this threshold if omnipose is not returning as many ROIs as you’d expect. "
						+ "<p>"
						+ "Similarly, decrease this threshold if omnipose is returning too many ill-shaped ROIs."
						+ "</html>" )
				.key( KEY_FLOW_THRESHOLD )
				.defaultValue( DEFAULT_FLOW_THRESHOLD )
				.min( 0. )
				.max( 3. )
				.get();

		/*
		 * Careful! Because omnipose is still based on cellpose 1, the
		 * cellprob_threshold parameter is still called mask_threshold.
		 */
		// Cell probability threshold
		this.cellProbThreshold = addDoubleArgument()
				.name( "Mask threshold" )
				.argument( "--mask_threshold" )
				.help( "<html>"
						+ "Decrease this threshold if omnipose is not returning as many ROIs as you’d expect. "
						+ "<p>"
						+ "Similarly, increase this threshold if omnipose is returning too many ROIs particularly from dim areas."
						+ "</html>" )
				.key( KEY_CELL_PROB_THRESHOLD )
				.defaultValue( DEFAULT_CELL_PROB_THRESHOLD )
				.min( -6. )
				.max( 6. )
				.get();

		// Resample
		this.noResample = addFlag()
				.name( "Do not resample" )
				.argument( "--no_resample" )
				.help( "<html>"
						+ "Disables flows/cellprob resampling to original image size before computing masks."
						+ "<p>"
						+ "Using this flag will make more masks more jagged with larger diameter settings but will be faster on large images."
						+ "</html>" )
				.key( KEY_NO_RESAMPLE )
				.defaultValue( DEFAULT_NO_RESAMPLE )
				.get();

		// Re-add it the arguments at the desired position.
		arguments.remove( flowThreshold );
		arguments.remove( cellProbThreshold );
		arguments.remove( noResample );
		arguments.add( 4, flowThreshold );
		arguments.add( 5, cellProbThreshold );
		arguments.add( 7, noResample );
	}

	public DoubleArgument flowThreshold()
	{
		return flowThreshold;
	}

	public DoubleArgument cellProbThreshold()
	{
		return cellProbThreshold;
	}

	public Flag noResample()
	{
		return noResample;
	}

	public static void main( final String[] args )
	{
		final AdvancedOmniposeCLI cli = new AdvancedOmniposeCLI( 4, "µm", 0.2 );
		System.out.println( cli );

		// Configure the CLI.
		cli.imageFolder().set( "/Users/tinevez/Desktop" );
		cli.modelPretrained().set( 0 );
		cli.segmentationChannel().set( 2 );
		cli.diameter().set( 2.0 );

		// Output command line.
		System.out.println( "Command line: " );
		System.out.println( CommandBuilder.build( cli ) );

		// Show config panel.
		final ConfigPanel panel = ConfigGuiBuilder.build( cli );
		final JFrame frame = new JFrame( "Advanced " + cli.getCommand() + " CLI" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}
}

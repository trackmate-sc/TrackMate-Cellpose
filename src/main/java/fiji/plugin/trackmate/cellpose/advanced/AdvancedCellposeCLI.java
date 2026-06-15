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
package fiji.plugin.trackmate.cellpose.advanced;

import javax.swing.JFrame;

import fiji.plugin.trackmate.cellpose.CellposeCLI;
import fiji.plugin.trackmate.util.cli.CommandBuilder;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder.ConfigPanel;

public class AdvancedCellposeCLI extends CellposeCLI
{


	/**
	 * The key to the parameter that store the flow threshold value. From
	 * cellpose docs:
	 * <p>
	 * Note there is nothing keeping the neural network from predicting
	 * horizontal and vertical flows that do not correspond to any real shapes
	 * at all. In practice, most predicted flows are consistent with real
	 * shapes, because the network was only trained on image flows that are
	 * consistent with real shapes, but sometimes when the network is uncertain
	 * it may output inconsistent flows. To check that the recovered shapes
	 * after the flow dynamics step are consistent with real ROIs, we recompute
	 * the flow gradients for these putative predicted ROIs, and compute the
	 * mean squared error between them and the flows predicted by the network.
	 * <p>
	 * The flow_threshold parameter is the maximum allowed error of the flows
	 * for each mask. The default is flow_threshold=0.4. Increase this threshold
	 * if cellpose is not returning as many ROIs as you’d expect. Similarly,
	 * decrease this threshold if cellpose is returning too many ill-shaped
	 * ROIs.
	 */
	public static final String KEY_FLOW_THRESHOLD = "FLOW_THRESHOLD";

	public static final Double DEFAULT_FLOW_THRESHOLD = Double.valueOf( 0.4 );

	/**
	 * The key to the parameter that store the cell probability threshold value.
	 * From cellpose docs:
	 * <p>
	 * The network predicts 3 outputs: flows in X, flows in Y, and cell
	 * “probability”. The predictions the network makes of the probability are
	 * the inputs to a sigmoid centered at zero (1 / (1 + e^-x)), so they vary
	 * from around -6 to +6. The pixels greater than the cellprob_threshold are
	 * used to run dynamics and determine ROIs. The default is
	 * cellprob_threshold=0.0. Decrease this threshold if cellpose is not
	 * returning as many ROIs as you’d expect. Similarly, increase this
	 * threshold if cellpose is returning too ROIs particularly from dim areas.
	 */
	public static final String KEY_CELL_PROB_THRESHOLD = "CELL_PROB_THRESHOLD";

	public static final Double DEFAULT_CELL_PROB_THRESHOLD = Double.valueOf( 0. );

	/**
	 * The key to the parameter that store the resampling option. From cellpose
	 * docs:
	 * <p>
	 * The cellpose network is run on your rescaled image – where the rescaling
	 * factor is determined by the diameter you input (or determined
	 * automatically as above). For instance, if you have an image with 60 pixel
	 * diameter cells, the rescaling factor is 30./60. = 0.5. After determining
	 * the flows (dX, dY, cellprob), the model runs the dynamics. The dynamics
	 * can be run at the rescaled size (resample=False), or the dynamics can be
	 * run on the resampled, interpolated flows at the true image size
	 * (resample=True). resample=True will create smoother ROIs when the cells
	 * are large but will be slower in case; resample=False will find more ROIs
	 * when the cells are small but will be slower in this case. By default in
	 * versions &ge; 1.0 resample=True.
	 */
	public static final String KEY_NO_RESAMPLE = "NO_RESAMPLE";

	public static final Boolean DEFAULT_NO_RESAMPLE = false;

	/**
	 * The key to the parameter that store the minimum size to keep masks. Used
	 * only if do_3D mode or 2D+Z and stitch_threshold &gt; 0 From cellpose
	 * docs:
	 * <p>
	 * Minimum number of pixels per mask, can turn off with -1.
	 */
	public static final String KEY_CELL_MIN_SIZE = "CELL_MIN_SIZE";

	public static final Double DEFAULT_CELL_MIN_SIZE = Double.valueOf( 15. );

	/**
	 * Parameters for CellPose 3D mode: either do_3D (do xy, yz, zx) or
	 * 2D+stitch_threshold to reconstruct in 3D from cellpose docs:
	 * <p>
	 * There may be additional differences in YZ and XZ slices that make them
	 * unable to be used for 3D segmentation. I’d recommend viewing the volume
	 * in those dimensions if the segmentation is failing. In those instances,
	 * you may want to turn off 3D segmentation (do_3D=False) and run instead
	 * with stitch_threshold &gt; 0. Cellpose will create ROIs in 2D on each XY
	 * slice and then stitch them across slices if the IoU between the mask on
	 * the current slice and the next slice is greater than or equal to the
	 * stitch_threshold.
	 */
	public static final Boolean DEFAULT_DO2DZ = false;

	public static final String KEY_DO2DZ = "DO2DZ";

	/** Default value of iou threshold for 2D+z stitching */
	public static final Double DEFAULT_IOU_THRESHOLD = Double.valueOf( 0.25 );

	public static final String KEY_IOU_THRESHOLD = "IOUTHRESHOLD";

	private final DoubleArgument flowThreshold;

	private final DoubleArgument cellProbThreshold;

	private final Flag noResample;

	public AdvancedCellposeCLI( final int nChannels, final String units, final double pixelSize )
	{
		super( nChannels, units, pixelSize );

		// Flow threshold
		this.flowThreshold = addDoubleArgument()
				.name( "Flow threshold" )
				.argument( "--flow_threshold" )
				.help( "<html>"
						+ "Increase this threshold if cellpose is not returning as many ROIs as you’d expect. "
						+ "<p>"
						+ "Similarly, decrease this threshold if cellpose is returning too many ill-shaped ROIs."
						+ "</html>" )
				.key( KEY_FLOW_THRESHOLD )
				.defaultValue( DEFAULT_FLOW_THRESHOLD )
				.min( 0. )
				.max( 3. )
				.get();

		// Cell probability threshold
		this.cellProbThreshold = addDoubleArgument()
				.name( "Cell probability threshold" )
				.argument( "--cellprob_threshold" )
				.help( "<html>"
						+ "Decrease this threshold if cellpose is not returning as many ROIs as you’d expect. "
						+ "<p>"
						+ "Similarly, increase this threshold if cellpose is returning too many ROIs particularly from dim areas."
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
		arguments.add( 5, flowThreshold );
		arguments.add( 6, cellProbThreshold );
		arguments.add( 8, noResample );
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
		final AdvancedCellposeCLI cli = new AdvancedCellposeCLI( 4, "µm", 0.2 );
		System.out.println( cli );

		// Configure the CLI.
		cli.imageFolder().set( "/Users/tinevez/Desktop" );
		cli.modelPretrained().set( "cyto3" );
		cli.chan1().set( 2 );
		cli.diameter().set( 20.0 );

		// Output command line.
		System.out.println( "Command line: " );
		System.out.println( CommandBuilder.build( cli ) );

		// Show config panel.
		final ConfigPanel panel = ConfigGuiBuilder.build( cli );
		final JFrame frame = new JFrame( "Advanced cellpose CLI" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}
}

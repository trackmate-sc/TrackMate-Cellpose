package fiji.plugin.trackmate.cellpose.advanced;

import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.DEFAULT_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.DEFAULT_FLOW_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_CELL_PROB_THRESHOLD;
import static fiji.plugin.trackmate.cellpose.advanced.AdvancedCellposeDetectorFactory.KEY_FLOW_THRESHOLD;

import javax.swing.JFrame;

import fiji.plugin.trackmate.cellpose.CellposeCLI;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder;
import fiji.plugin.trackmate.util.cli.CliGuiBuilder.CliConfigPanel;
import fiji.plugin.trackmate.util.cli.CommandBuilder;

public class AdvancedCellposeCLI extends CellposeCLI
{

	public static final String KEY_NO_RESAMPLE = "NO_RESAMPLE";

	public static final Boolean DEFAULT_NO_RESAMPLE = false;

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
		arguments.add( 4, flowThreshold );
		arguments.add( 5, cellProbThreshold );
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
		final CliConfigPanel panel = CliGuiBuilder.build( cli );
		final JFrame frame = new JFrame( "Advanced cellpose CLI" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}

}

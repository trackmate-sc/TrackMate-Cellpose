package fiji.plugin.trackmate;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Window;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

import org.apposed.appose.Builder.ProgressConsumer;
import org.apposed.appose.TaskEvent;

import ij.IJ;
import net.imglib2.cellpose.ApposeTaskListener;

/**
 * An implementation of ApposeTaskListener that uses TrackMate to report task
 * progress, and env installation via a custom progress dialog. The dialog is
 * only shown if the installation takes more than 1 second.
 */
public class TrackMateApposeProgressListener implements ApposeTaskListener
{

	private volatile JDialog progressDialog;

	private volatile JProgressBar progressBar;

	private volatile ScheduledFuture< ? > delayedShowTask;

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );

	private final Logger logger;

	public TrackMateApposeProgressListener( final String title, final Logger logger )
	{
		this.logger = logger;
	}

	/*
	 * Normal task messages -> TrackMate logger.
	 */

	public void error( final String msg )
	{
		logger.error( msg );
	}

	@Override
	public void message( final String msg )
	{
		logger.log( msg );
	}

	public void progress( final double progress )
	{
		logger.setProgress( progress );
	}

	@Override
	public Consumer< TaskEvent > taskListener()
	{
		/*
		 *  A hack: When this method is called, it means that the env build process
		 *  has finished successfully. We can close the progress dialog.
		 */
		close();
		return e -> {
			if ( e.message != null )
				message( e.message );
			if ( e.current >= 0 && e.maximum > 0 )
				progress( ( double ) e.current / e.maximum );
		};
	}

	/*
	 * Installation messages -> Custom progress dialog.
	 */

	@Override
	public Consumer< String > outputListener()
	{
		return str -> log( str );
	}

	@Override
	public Consumer< String > errorListener()
	{
		return str -> {
			if ( str != null && str.contains( "The" ) && str.contains( "environment has been installed." ) )
			{
				final String envName = str.substring( str.indexOf( "The" ) + 3, str.indexOf( "environment" ) );
				message( "Python environment " + envName + " is ready." );
			}
			else
			{
				// Actual error.
				error( "ERROR: " + str );
			}
		};
	}

	@Override
	public ProgressConsumer progressListener()
	{
		return ( msg, cur, max ) -> log( msg, cur, max );
	}

	public void close()
	{
		EventQueue.invokeLater( () -> {
			// Cancel the delayed show if it hasn't run yet
			if ( delayedShowTask != null )
			{
				delayedShowTask.cancel( false );
				delayedShowTask = null;
			}

			if ( progressDialog != null )
				progressDialog.dispose();
			progressDialog = null;
		} );
	}

	private final AtomicBoolean dialogHasBeenUsed = new AtomicBoolean( false );

	private void log( final String msg, final Long cur, final Long max )
	{
		dialogHasBeenUsed.set( true );
		EventQueue.invokeLater( () -> {
			if ( progressDialog == null )
			{
				// Schedule the dialog to appear after 1 second
				if ( delayedShowTask == null )
				{
					delayedShowTask = scheduler.schedule( () -> {
						EventQueue.invokeLater( () -> {
							if ( progressDialog == null && dialogHasBeenUsed.get() )
								createAndShowDialog();
						} );
					}, 1, TimeUnit.SECONDS );
				}
				return; // Don't update yet, dialog not visible
			}

			// Update existing dialog
			updateProgressBar( msg, cur, max );
		} );
	}

	private void log( final String msg )
	{
		log( msg, null, null );
	}

	private void createAndShowDialog()
	{
		final Window owner = IJ.getInstance();
		progressDialog = new JDialog( owner, "Fiji ♥ Appose" );
		progressDialog.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		progressBar = new JProgressBar();
		progressDialog.getContentPane().add( progressBar );
		progressBar.setFont( new Font( "Courier", Font.PLAIN, 14 ) );
		progressBar.setString("   Installing Python environment   "  );
		progressBar.setStringPainted( true );
		progressBar.setIndeterminate( true );
		progressDialog.pack();
		progressDialog.setLocationRelativeTo( owner );
		progressDialog.setVisible( true );
		delayedShowTask = null;
	}

	private void updateProgressBar( final String msg, final Long cur, final Long max )
	{
		if ( msg != null && !msg.trim().isEmpty() )
			progressBar.setString( "Building Python environment: " + msg.trim() );
		if ( cur != null || max != null )
			progressBar.setIndeterminate( false );
		if ( max != null )
			progressBar.setMaximum( max.intValue() );
		if ( cur != null )
			progressBar.setValue( cur.intValue() );
	}
}

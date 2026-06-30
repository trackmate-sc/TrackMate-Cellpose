package fiji.plugin.trackmate.cellpose;

import java.util.function.Consumer;

import org.apposed.appose.Builder.ProgressConsumer;
import org.apposed.appose.TaskEvent;

import net.imglib2.cellpose.ApposeTaskListener;

/**
 * Adapts a {@link org.scijava.ui.config.listeners.ApposeTaskListener} to the
 * one used in imglib2-cellpose.
 */
public class CellposeApposeListener implements ApposeTaskListener
{

	private final org.scijava.ui.config.listeners.ApposeTaskListener listener;

	CellposeApposeListener( final org.scijava.ui.config.listeners.ApposeTaskListener listener )
	{
		this.listener = listener;
	}

	@Override
	public Consumer< TaskEvent > taskListener()
	{
		return listener.taskListener();
	}

	@Override
	public Consumer< String > outputListener()
	{
		return listener.outputListener();
	}

	@Override
	public Consumer< String > errorListener()
	{
		return listener.errorListener();
	}

	@Override
	public ProgressConsumer progressListener()
	{
		return listener.progressListener();
	}

	@Override
	public void message( final String msg )
	{
		listener.message( msg );
	}

	public static final CellposeApposeListener of( final org.scijava.ui.config.listeners.ApposeTaskListener listener )
	{
		return new CellposeApposeListener( listener );
	}
}
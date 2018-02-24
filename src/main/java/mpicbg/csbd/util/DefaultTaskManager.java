package mpicbg.csbd.util;

import java.util.ArrayList;
import java.util.List;

public class DefaultTaskManager implements TaskManager {

	private final List< Task > tasks;
	private final TaskPresenter taskPresenter;

	public DefaultTaskManager() {
		taskPresenter = new DefaultTaskPresenter( this );
		tasks = new ArrayList<>();
	}

	@Override
	public void initialize() {
		taskPresenter.initialize();
	}

	@Override
	public void add( final Task task ) {
		tasks.add( task );
		task.setManager( this );
		taskPresenter.addStep( task.getTitle() );
	}

	@Override
	public void cancel() {
//		pool.shutdownNow();
//		failMsg = PROGRESS_CANCELED;
//		commandFailed = true;
		for ( final Task task : tasks ) {
			task.cancel();
		}
	}

	@Override
	public void log( final String msg ) {
		taskPresenter.log( msg );
	}

	@Override
	public void logError( final String msg ) {
		taskPresenter.logError( msg );
	}

	@Override
	public void update( final Task task ) {
		final int index = tasks.indexOf( task );
		if ( task.isStarted() ) {
			taskPresenter.setStepStarted( index );
		}
		if ( task.isFailed() ) {
			taskPresenter.setStepFailed( index );
		}
		if ( task.isFinished() ) {
			taskPresenter.setStepDone( index );
		}
	}

}

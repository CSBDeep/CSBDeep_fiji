package mpicbg.csbd.task;

import java.util.ArrayList;
import java.util.List;

public class TaskForceManager implements TaskManager {

	private final List< TaskForce > taskForces;
	private final TaskPresenter taskPresenter;

	public TaskForceManager() {
		taskPresenter = new DefaultTaskPresenter( this );
		taskForces = new ArrayList<>();
	}

	@Override
	public void initialize() {
		taskPresenter.initialize();
	}

	@Override
	public void add( final Task task ) {}

	@Override
	public void cancel() {
		for ( final Task task : taskForces ) {
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

		int index = taskForces.indexOf( task );
		if ( index == -1 ) {
			TaskForce taskForce = null;
			for ( int i = 0; i < taskForces.size() && index < 0; i++ ) {
				for ( final Task subtask : taskForces.get( i ).getTasks() ) {
					if ( task.equals( subtask ) ) {
						index = i;
						taskForce = taskForces.get( i );
						break;
					}
				}
			}
			if ( index < 0 || taskForce == null ) return;
			taskForce.update();
		} else {
			final TaskForce taskForce = taskForces.get( index );
			if ( taskForce.isStarted() ) {
				taskPresenter.setStepStarted( index );
			}
			if ( taskForce.isFailed() ) {
				taskPresenter.setStepFailed( index );
			}
			if ( taskForce.isFinished() ) {
				taskPresenter.setStepDone( index );
			}
		}
	}

	@Override
	public void close() {
		taskPresenter.close();
	}

	public <T extends Task> void createTaskForce( final String codeName, final T... tasks ) {
		final TaskForce taskForce = new TaskForce( codeName, tasks );
		taskForces.add( taskForce );
		taskForce.setManager( this );
		taskPresenter.addStep( taskForce.getTitle() );
		for ( final Task task : tasks ) {
			task.setManager( this );
		}
	}

	private class TaskForce extends DefaultTask {

		private final Task[] tasks;

		public TaskForce( final String codeName, final Task[] tasks ) {
			setTitle( codeName );
			this.tasks = tasks;
		}

		public void update() {
			boolean allFinished = true;
			setIdle();
			for ( final Task task : tasks ) {
				if ( !task.isFinished() ) {
					allFinished = false;
				}
				if ( task.isStarted() ) {
					setStarted();
				}
				if ( task.isFailed() ) {
					setFailed();
					break;
				}
			}
			if ( !isFailed() && allFinished ) {
				setFinished();
			}
		}

		public Task[] getTasks() {
			return tasks;
		}

		@Override
		public void cancel() {
			for ( final Task task : tasks ) {
				task.cancel();
			}
		}
	}

}

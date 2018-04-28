package mpicbg.csbd.task;

import org.scijava.log.Logger;

import java.util.ArrayList;
import java.util.List;

public class TaskForceManager implements TaskManager {

	private final List< TaskForce > taskForces;
	private final TaskPresenter taskPresenter;

	public TaskForceManager(boolean headless, Logger logger) {
		taskPresenter = new DefaultTaskPresenter( this, headless, logger );
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
	public void finalizeSetup() {
		taskPresenter.show();
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
			taskForce.update(index);
		} else {
			final TaskForce taskForce = taskForces.get( index );
			if ( taskForce.isStarted() ) {
				taskPresenter.setTaskStarted( index );
				if(task.numSteps() > 1) {
					taskPresenter.setTaskNumSteps(index, task.numSteps());
					taskPresenter.setTaskStep(index, task.getCurrentStep());
				}
			}
			if ( taskForce.isFailed() ) {
				taskPresenter.setTaskFailed( index );
			}
			if ( taskForce.isFinished() ) {
				taskPresenter.setTaskDone( index );
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
		taskPresenter.addTask( taskForce.getTitle() );
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

		public void update(int index) {
			boolean allFinished = true;
			setIdle();
			int numSteps = 0;
			int currentStep = 0;
			for ( final Task task : tasks ) {
				if ( !task.isFinished() ) {
					allFinished = false;
				}else {
					numSteps += task.numSteps();
					currentStep += task.numSteps();
				}
				if ( task.isStarted() ) {
					setStarted();
					numSteps += task.numSteps();
					currentStep += task.getCurrentStep();
				}
				if ( task.isFailed() ) {
					setFailed();
					break;
				}
			}
			if ( !isFailed() && allFinished ) {
				setFinished();
			}
			setNumSteps(numSteps);
			setCurrentStep(currentStep);
			if(numSteps() > 1) {
				taskPresenter.setTaskNumSteps(index, numSteps());
				taskPresenter.setTaskStep(index, getCurrentStep());
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

		@Override
		public void setStarted() {
			started = true;
			failed = false;
			updateManager();
		}

		@Override
		public void setIdle() {
			started = false;
			failed = false;
			updateManager();
		}

		@Override
		public void setFailed() {
			updateManager();
		}

		@Override
		public void setFinished() {
			setFinished( true );
		}
	}

}

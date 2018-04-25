package mpicbg.csbd.task;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mpicbg.csbd.ui.CSBDeepProgress;
import org.scijava.Initializable;
import org.scijava.log.Logger;

public class DefaultTaskPresenter implements TaskPresenter, ActionListener {

	CSBDeepProgress progressWindow;
	TaskManager taskManager;
	Logger logger;
	final boolean headless;
	boolean initialized = false;

	public DefaultTaskPresenter(final TaskManager taskManager, boolean headless, Logger logger) {
		this.headless = headless;
		this.taskManager = taskManager;
		this.logger = logger;
	}

	@Override
	public void initialize() {
		if(!headless) {
			progressWindow = CSBDeepProgress.create();
			progressWindow.getCancelBtn().addActionListener( this );
			initialized = true;
		}
	}

	@Override
	public void initializeWithGPUWarning() {
		if(!headless) {
			progressWindow = CSBDeepProgress.createWithGPUWarning();
			progressWindow.getCancelBtn().addActionListener(this);
			initialized = true;
		}
	}

	private boolean inUse() {
		return !headless && initialized;
	}
	
	@Override
	public void close() {
		if(inUse()) {
			progressWindow.getCancelBtn().removeActionListener(this);
			progressWindow.dispose();
		}
	}

	@Override
	public void show() {
		if(inUse()) {
			progressWindow.display();
		}
	}

	@Override
	public void addTask(final String title ) {
		if(inUse()) {
			progressWindow.addTask( title );
		}
	}

	@Override
	public void setTaskDone(final int index ) {
		if(inUse()) {
			progressWindow.setTaskDone( index );
		}
	}

	@Override
	public void setTaskFailed(final int index ) {
		if(inUse()) {
			progressWindow.setTaskFail( index );
		}
	}

	@Override
	public void setTaskStarted(final int index ) {
		if(inUse()) {
			progressWindow.setTaskStart( index );
		}
	}

	@Override
	public void setTaskInProgress(final int index ) {
		if(inUse()) {
			progressWindow.setTaskStart( index );
		}
	}

	@Override
	public void setTaskNumSteps(final int index, final int numSteps) {
		if(inUse()) {
			progressWindow.setTaskNumSteps(index, numSteps);
		}
	}

	@Override
	public void setTaskStep(final int index, final int step) {
		if(inUse()) {
			progressWindow.setTaskCurrentStep(index, step);
		}
	}

	@Override
	public void log( final String msg ) {
		if(inUse()) {
			progressWindow.addLog( msg );
		}
		logger.info( msg );
	}

	@Override
	public void logError( final String msg ) {
		if(inUse()) {
			progressWindow.addError( msg );
		}
		logger.error( msg );
	}

	@Override
	public void actionPerformed( final ActionEvent e ) {
		if(inUse()) {
			if ( e.getSource().equals( progressWindow.getCancelBtn() ) ) {
				taskManager.cancel();
			}
		}
	}

}

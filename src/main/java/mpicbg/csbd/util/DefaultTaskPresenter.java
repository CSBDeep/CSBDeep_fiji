package mpicbg.csbd.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mpicbg.csbd.ui.CSBDeepProgress;

public class DefaultTaskPresenter implements TaskPresenter, ActionListener {

	CSBDeepProgress progressWindow;
	TaskManager taskManager;

	public DefaultTaskPresenter( final TaskManager taskManager ) {
		this.taskManager = taskManager;
	}

	@Override
	public void initialize() {
		progressWindow = CSBDeepProgress.create();
		progressWindow.getCancelBtn().addActionListener( this );
	}

	@Override
	public void initializeWithGPUWarning() {
		progressWindow = CSBDeepProgress.createWithGPUWarning();
		progressWindow.getCancelBtn().addActionListener( this );
	}
	
	@Override
	public void close() {
		progressWindow.getCancelBtn().removeActionListener( this );
	}

	@Override
	public void addStep( final String title ) {
		progressWindow.addStep( title );
	}

	@Override
	public void setStepDone( final int index ) {
		progressWindow.setStepDone( index );
	}

	@Override
	public void setStepFailed( final int index ) {
		progressWindow.setStepFail( index );
	}

	@Override
	public void setStepStarted( final int index ) {
		progressWindow.setStepStart( index );
	}

	@Override
	public void setStepInProgress( final int index ) {
		progressWindow.setStepStart( index );
	}

	@Override
	public void log( final String msg ) {
		progressWindow.addLog( msg );
		System.out.println( msg );
	}

	@Override
	public void logError( final String msg ) {
		progressWindow.addError( msg );
		System.out.println( "ERROR: " + msg );
	}

	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( progressWindow.getCancelBtn() ) ) {
			taskManager.cancel();
		}
	}

}

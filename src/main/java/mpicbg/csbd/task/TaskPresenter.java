package mpicbg.csbd.task;

public interface TaskPresenter {

	public void initialize();

	public void initializeWithGPUWarning();

	public void addStep( String title );

	public void setStepDone( int index );

	public void setStepFailed( int index );

	public void setStepStarted( int index );

	public void setStepInProgress( int index );

	public void log( String msg );

	public void logError( String msg );

	void close();

}

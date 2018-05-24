package mpicbg.csbd.task;

public interface TaskPresenter {

	public void initialize();

	public void initializeWithGPUWarning();

	public void addTask(String title );

	public void setTaskDone(int index );

	public void setTaskFailed(int index );

	public void setTaskStarted(int index );

	public void setTaskInProgress(int index );

	public void setTaskNumSteps(final int index, final int numSteps);

	public void setTaskStep(final int index, final int step);

	public void debug( String msg );

	public void log( String msg );

	public void logError( String msg );

	public void close();

	public void show();
}

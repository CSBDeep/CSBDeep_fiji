
package mpicbg.csbd.task;

public interface TaskPresenter {

	void initialize();

	void addTask(String title);

	void setTaskDone(int index);

	void setTaskFailed(int index);

	void setTaskStarted(int index);

	void setTaskInProgress(int index);

	void setTaskNumSteps(final int index, final int numSteps);

	void setTaskStep(final int index, final int step);

	void debug(String msg);

	void log(String msg);

	void logError(String msg);

	void close();

	void show();

	void showGPUWarning();
}

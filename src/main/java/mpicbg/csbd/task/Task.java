
package mpicbg.csbd.task;

import org.scijava.Cancelable;

public interface Task extends Cancelable {

	void setManager(TaskManager manager);

	boolean isStarted();

	boolean isFinished();

	boolean isFailed();

	void setStarted();

	void setIdle();

	void setFailed();

	void setFinished();

	void setFinished(boolean finished);

	boolean hasMultipleSteps();

	int numSteps();

	void setNumSteps(int steps);

	void nextStep();

	boolean hasIterations();

	int numIterations();

	void addIteration();

	void startNewIteration();

	void setIterations(int iterations);

	void nextIteration();

	void debug(String msg);

	void log(String msg);

	void logError(String msg);

	int getCurrentIteration();

	void setCurrentIteration(int currentIteration);

	int getCurrentStep();

	void setCurrentStep(int currentStep);

	String getTitle();

	void setTitle(String title);

}

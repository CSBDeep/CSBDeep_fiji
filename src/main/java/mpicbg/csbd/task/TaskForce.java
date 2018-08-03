
package mpicbg.csbd.task;

public class TaskForce extends DefaultTask {

	private final Task[] tasks;
	private final TaskPresenter taskPresenter;

	public TaskForce(final String codeName, final Task[] tasks,
		final TaskPresenter taskPresenter)
	{
		setTitle(codeName);
		this.tasks = tasks;
		this.taskPresenter = taskPresenter;
	}

	public void update(int index) {
		boolean allFinished = true;
		setIdle();
		int numSteps = 0;
		int currentStep = 0;
		for (final Task task : tasks) {
			if (!task.isFinished()) {
				allFinished = false;
			}
			else {
				numSteps += task.numSteps();
				currentStep += task.numSteps();
			}
			if (task.isStarted()) {
				setStarted();
				numSteps += task.numSteps();
				currentStep += task.getCurrentStep();
			}
			if (task.isFailed()) {
				setFailed();
				break;
			}
		}
		if (!isFailed() && allFinished) {
			setFinished();
		}
		setNumSteps(numSteps);
		setCurrentStep(currentStep);
		if (numSteps() > 1) {
			taskPresenter.setTaskNumSteps(index, numSteps());
			taskPresenter.setTaskStep(index, getCurrentStep());
		}
	}

	public Task[] getTasks() {
		return tasks;
	}

	@Override
	public void cancel(String reason) {
		for (final Task task : tasks) {
			task.cancel(reason);
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
		setFinished(true);
	}
}

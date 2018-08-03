
package mpicbg.csbd.task;

import java.util.ArrayList;
import java.util.List;

import org.scijava.log.Logger;

public class TaskForceManager extends DefaultTaskManager {

	private final List<TaskForce> taskForces;

	public TaskForceManager(boolean headless, Logger logger) {
		super(headless, logger);
		taskForces = new ArrayList<>();
	}

	@Override
	public void add(final Task task) {}

	@Override
	public void cancel(String reason) {
		for (final Task task : taskForces) {
			task.cancel(reason);
		}
	}

	@Override
	public void update(final Task task) {

		int index = taskForces.indexOf(task);
		if (index == -1) {
			TaskForce taskForce = null;
			for (int i = 0; i < taskForces.size() && index < 0; i++) {
				for (final Task subtask : taskForces.get(i).getTasks()) {
					if (task.equals(subtask)) {
						index = i;
						taskForce = taskForces.get(i);
						break;
					}
				}
			}
			if (index < 0 || taskForce == null) return;
			taskForce.update(index);
		}
		else {
			final TaskForce taskForce = taskForces.get(index);
			if (taskForce.isStarted()) {
				taskPresenter.setTaskStarted(index);
				if (task.numSteps() > 1) {
					taskPresenter.setTaskNumSteps(index, task.numSteps());
					taskPresenter.setTaskStep(index, task.getCurrentStep());
				}
			}
			if (taskForce.isFailed()) {
				taskPresenter.setTaskFailed(index);
			}
			if (taskForce.isFinished()) {
				taskPresenter.setTaskDone(index);
			}
		}
	}

	public <T extends Task> void createTaskForce(final String codeName,
		final T... tasks)
	{
		final TaskForce taskForce = new TaskForce(codeName, tasks, taskPresenter);
		taskForces.add(taskForce);
		taskForce.setManager(this);
		taskPresenter.addTask(taskForce.getTitle());
		for (final Task task : tasks) {
			task.setManager(this);
		}
	}

}

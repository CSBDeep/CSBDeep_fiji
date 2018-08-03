
package mpicbg.csbd.task;

import java.util.ArrayList;
import java.util.List;

import org.scijava.log.Logger;

public class DefaultTaskManager implements TaskManager {

	private final List<Task> tasks;
	protected final TaskPresenter taskPresenter;

	public DefaultTaskManager(boolean headless, Logger logger) {
		taskPresenter = new DefaultTaskPresenter(this, headless, logger);
		tasks = new ArrayList<>();
	}

	@Override
	public void initialize() {
		taskPresenter.initialize();
	}

	@Override
	public void noGPUFound() {
		taskPresenter.showGPUWarning();
	}

	@Override
	public void add(final Task task) {
		tasks.add(task);
		task.setManager(this);
		taskPresenter.addTask(task.getTitle());
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void cancel(String reason) {
		for (final Task task : tasks) {
			task.cancel(reason);
		}
	}

	@Override
	public String getCancelReason() {
		return null;
	}

	@Override
	public void debug(final String msg) {
		taskPresenter.debug(msg);
	}

	@Override
	public void log(final String msg) {
		taskPresenter.log(msg);
	}

	@Override
	public void logError(final String msg) {
		taskPresenter.logError(msg);
	}

	@Override
	public void finalizeSetup() {
		taskPresenter.show();
	}

	@Override
	public void update(final Task task) {
		final int index = tasks.indexOf(task);
		if (task.isStarted()) {
			taskPresenter.setTaskStarted(index);
		}
		if (task.isFailed()) {
			taskPresenter.setTaskFailed(index);
		}
		if (task.isFinished()) {
			taskPresenter.setTaskDone(index);
		}
	}

	@Override
	public void close() {
		taskPresenter.close();
	}

}


package de.csbdresden.csbdeep.task;

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.util.ArrayList;
import java.util.List;

public class DefaultTaskManager implements TaskManager {

	protected final List<Task> tasks;
	protected final TaskPresenter taskPresenter;

	@Parameter
	protected LogService logger;

	@Parameter
	private Context context;

	public DefaultTaskManager(boolean headless) {
		tasks = new ArrayList<>();
		taskPresenter = new DefaultTaskPresenter(this, headless);
	}

	@Override
	public void initialize() {
		context.inject(taskPresenter);
		taskPresenter.initialize();
	}

	@Override
	public void noGPUFound() {
		taskPresenter.showGPUWarning();
	}

	@Override
	public void logWarning(String msg) {
		taskPresenter.logWarning(msg);
		logger.warn(msg);
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
		logger.debug(msg);
	}

	@Override
	public void log(final String msg) {
		taskPresenter.log(msg);
		logger.info(msg);
	}

	@Override
	public void logError(final String msg) {
		taskPresenter.logError(msg);
		logger.error(msg);
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

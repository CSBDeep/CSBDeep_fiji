/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2020 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

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

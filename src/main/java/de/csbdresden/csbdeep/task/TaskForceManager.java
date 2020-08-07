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

import java.util.ArrayList;
import java.util.List;

public class TaskForceManager extends DefaultTaskManager {

	private final List<TaskForce> taskForces;

	public TaskForceManager(boolean headless) {
		super(headless);
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

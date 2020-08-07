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

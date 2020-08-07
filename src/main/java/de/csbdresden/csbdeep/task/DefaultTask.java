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

public class DefaultTask implements Task {

	protected boolean started = false;
	protected boolean finished = false;
	protected boolean failed = false;
	protected TaskManager manager;
	protected int steps = 1;
	protected int iterations = 1;
	protected int currentIteration = 0;
	protected int currentStep = 0;
	protected String title;

	@Override
	public void setManager(final TaskManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public boolean isFailed() {
		return failed;
	}

	void updateManager() {
		if (manager != null) {
			manager.update(this);
		}
	}

	@Override
	public void setStarted() {
		started = true;
		failed = false;
		debug(getClassName() + " started");
		updateManager();
	}

	@Override
	public void setIdle() {
		started = false;
		failed = false;
		debug(getClassName() + " idle");

		updateManager();
	}

	@Override
	public void setFailed() {
		failed = true;
		debug(getClassName() + " failed");
		updateManager();
	}

	@Override
	public void setFinished() {
		debug(getClassName() + " finished");
		setFinished(true);
	}

	@Override
	public void setFinished(final boolean finished) {
		this.finished = finished;
		updateManager();
	}

	@Override
	public boolean hasMultipleSteps() {
		return numSteps() > 1;
	}

	@Override
	public int numSteps() {
		return steps;
	}

	@Override
	public void setNumSteps(final int steps) {
		this.steps = steps;
		updateManager();
	}

	@Override
	public void nextStep() {
		currentStep++;
	}

	@Override
	public boolean hasIterations() {
		return numIterations() > 0;
	}

	@Override
	public int numIterations() {
		return iterations;
	}

	@Override
	public void addIteration() {
		iterations++;
	}

	@Override
	public void startNewIteration() {
		iterations++;
		currentIteration++;
	}

	@Override
	public void setIterations(final int iterations) {
		this.iterations = iterations;
		updateManager();
	}

	@Override
	public void nextIteration() {
		currentIteration++;
		setFinished(false);
	}

	@Override
	public void debug(final String msg) {
		if (manager != null) {
			manager.debug(msg);
		}
		else {
			System.out.println("[DEBUG] " + msg);
		}
	}

	@Override
	public void log(final String msg) {
		if (manager != null) {
			manager.log(msg);
		}
		else {
			System.out.println("[INFO] " + msg);
		}
	}

	@Override
	public void logError(final String msg) {
		if (manager != null) {
			manager.logError(msg);
		}
		else {
			System.out.println("[ERROR] " + msg);
		}
	}

	@Override
	public int getCurrentIteration() {
		return currentIteration;
	}

	@Override
	public void setCurrentIteration(final int currentIteration) {
		this.currentIteration = currentIteration;
		updateManager();
	}

	@Override
	public int getCurrentStep() {
		return currentStep;
	}

	@Override
	public void setCurrentStep(final int currentStep) {
		this.currentStep = currentStep;
		updateManager();
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void cancel(String reason) {

	}

	@Override
	public String getCancelReason() {
		return null;
	}

	@Override
	public String getTitle() {
		if (title == null) return this.getClass().getSimpleName();
		return title;
	}

	private String getClassName() {
		return this.getClass().toString();
	}

	@Override
	public void setTitle(final String title) {
		this.title = title;
	}

	@Override
	public void logWarning(String msg) {
		if (manager != null) {
			manager.logWarning(msg);
		}
		else {
			System.out.println("WARNING: " + msg);
		}
	}
}

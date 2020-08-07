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

import de.csbdresden.csbdeep.ui.CSBDeepProgress;
import net.imagej.tensorflow.TensorFlowService;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DefaultTaskPresenter extends WindowAdapter implements TaskPresenter, ActionListener {

	private CSBDeepProgress progressWindow;
	private final TaskManager taskManager;
	@Parameter
	private ThreadService threadService;
	@Parameter
	private StatusService status;
	@Parameter
	private TensorFlowService tensorFlowService;
	private final boolean headless;
	private boolean initialized = false;
	private boolean closing = false;

	public DefaultTaskPresenter(final TaskManager taskManager, boolean headless)
	{
		this.headless = headless;
		this.taskManager = taskManager;
	}

	@Override
	public void initialize() {
		if (!headless) {
			progressWindow = CSBDeepProgress.create(status, threadService);
			progressWindow.updateTensorFlowStatus(tensorFlowService.getTensorFlowVersion());
			progressWindow.getCancelBtn().addActionListener(this);
			progressWindow.getFrame().addWindowListener(this);
			initialized = true;
		}
	}

	private boolean inUse() {
		return !headless && initialized;
	}

	@Override
	public void close() {
		if (inUse()) {
			closing = true;
			progressWindow.getCancelBtn().removeActionListener(this);
			progressWindow.dispose();
		}
	}

	@Override
	public void show() {
		if (inUse()) {
			progressWindow.display();
		}
	}

	@Override
	public void logWarning(String msg) {
		if (inUse()) {
			progressWindow.addError(msg);
		}
	}

	@Override
	public void addTask(final String title) {
		if (inUse()) {
			progressWindow.addTask(title);
		}
	}

	@Override
	public void setTaskDone(final int index) {
		if (inUse()) {
			progressWindow.setTaskDone(index);
		}
	}

	@Override
	public void setTaskFailed(final int index) {
		if (inUse()) {
			progressWindow.setTaskFail(index);
		}
	}

	@Override
	public void setTaskStarted(final int index) {
		if (inUse()) {
			progressWindow.setTaskStart(index);
		}
	}

	@Override
	public void setTaskInProgress(final int index) {
		if (inUse()) {
			progressWindow.setTaskStart(index);
		}
	}

	@Override
	public void setTaskNumSteps(final int index, final int numSteps) {
		if (inUse()) {
			progressWindow.setTaskNumSteps(index, numSteps);
		}
	}

	@Override
	public void setTaskStep(final int index, final int step) {
		if (inUse()) {
			progressWindow.setTaskCurrentStep(index, step);
		}
	}

	@Override
	public void debug(final String msg) {
		if (inUse()) {
			progressWindow.addLog(msg);
		}
	}

	@Override
	public void log(final String msg) {
		if (inUse()) {
			progressWindow.addLog(msg);
		}
	}

	@Override
	public void logError(final String msg) {
		if (inUse()) {
			progressWindow.addError(msg);
			createErrorPopup(msg);
		}
	}

	private void createErrorPopup(String msg) {
		JOptionPane.showMessageDialog(null,
				"<html><p style='width: 333px;'>"+msg.replace("\n", "<br/>")+"</p>",
				"CSBDeep error",
				JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (inUse()) {
			if (e.getSource().equals(progressWindow.getCancelBtn())) {
				taskManager.cancel("Canceled");
			}
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
		if (inUse()) {
			if(!closing) taskManager.cancel("Canceled");
		}
	}
}

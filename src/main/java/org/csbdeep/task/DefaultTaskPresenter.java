
package org.csbdeep.task;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.csbdeep.ui.CSBDeepProgress;
import org.scijava.app.StatusService;
import org.scijava.thread.ThreadService;

public class DefaultTaskPresenter implements TaskPresenter, ActionListener {

	private CSBDeepProgress progressWindow;
	private final TaskManager taskManager;
	private final ThreadService threadService;
	private final StatusService status;
	private final boolean headless;
	private boolean initialized = false;

	public DefaultTaskPresenter(final TaskManager taskManager, boolean headless, StatusService status, ThreadService threadService)
	{
		this.headless = headless;
		this.taskManager = taskManager;
		this.status = status;
		this.threadService = threadService;
	}

	@Override
	public void initialize() {
		if (!headless) {
			progressWindow = CSBDeepProgress.create(status, threadService);
			progressWindow.getCancelBtn().addActionListener(this);
			initialized = true;
		}
	}

	private boolean inUse() {
		return !headless && initialized;
	}

	@Override
	public void close() {
		if (inUse()) {
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
	public void showGPUWarning() {
		if (inUse()) {
			progressWindow.showGPUWarning();
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

}

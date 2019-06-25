
package de.csbdresden.csbdeep.task;

import java.awt.event.*;

import javax.swing.*;

import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;

import de.csbdresden.csbdeep.network.model.tensorflow.TensorFlowInstallationService;
import de.csbdresden.csbdeep.ui.CSBDeepProgress;

public class DefaultTaskPresenter extends WindowAdapter implements TaskPresenter, ActionListener {

	private CSBDeepProgress progressWindow;
	private final TaskManager taskManager;
	@Parameter
	private ThreadService threadService;
	@Parameter
	private StatusService status;
	@Parameter
	private TensorFlowInstallationService tensorFlowService;
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
			progressWindow.updateTensorFlowStatus(tensorFlowService.getCurrentVersion());
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

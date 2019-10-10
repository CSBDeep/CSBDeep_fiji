/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2018 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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

package de.csbdresden.csbdeep.ui;

import net.imagej.tensorflow.TensorFlowVersion;
import net.imagej.updater.util.UpdaterUtil;
import org.scijava.app.StatusService;
import org.scijava.thread.ThreadService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CSBDeepProgress extends JPanel {

	private class GuiTask {

		public JLabel status;
		public JLabel title;
		public int numIterations;
		public int iteration;
		public int numSteps;
		public int step;
		public boolean taskDone;
	}

	private final StatusService status;
	private final ThreadService threadService;

	private final JButton okButton, cancelButton;
	private final JProgressBar progressBar;
	private final Component progressBarSpace;
	private final JTextPane taskOutput;

	public static final int STATUS_IDLE = -1;
	public static final int STATUS_RUNNING = 0;
	public static final int STATUS_DONE = 1;
	public static final int STATUS_FAIL = 2;

	private final List<GuiTask> tasks = new ArrayList<>();
	private int currentTask;
	private boolean currentTaskFailing;

	private final JPanel taskContainer;
	private final JFrame frame;

	private JPanel note1;

	private JLabel tensorFlowStatus = new JLabel("", SwingConstants.RIGHT);

	private final SimpleAttributeSet red = new SimpleAttributeSet();

	public JFrame getFrame() {
		return frame;
	}

	public JButton getCancelBtn() {
		return cancelButton;
	}

	public JButton getOkBtn() {
		return okButton;
	}

	public CSBDeepProgress(JFrame frame, StatusService status, ThreadService threadService) {

		super(new BorderLayout());

		this.status = status;
		this.frame = frame;
		this.threadService = threadService;

		StyleConstants.setForeground(red, Color.red);

		taskContainer = new JPanel();
		taskContainer.setLayout(new BoxLayout(taskContainer, BoxLayout.Y_AXIS));

		taskContainer.setBorder(new EmptyBorder(0, 0, 0, 123));

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);

		taskOutput = new JTextPane();
		taskOutput.setAutoscrolls(true);
		taskOutput.setMinimumSize(new Dimension(200, 80));
		taskOutput.setPreferredSize(new Dimension(200, 80));
		taskOutput.setMargin(new Insets(5, 5, 5, 5));
		taskOutput.setEditable(false);

		// WARNINGS
		final JPanel notePanel = new JPanel();
		notePanel.setLayout(new BoxLayout(notePanel, BoxLayout.Y_AXIS));
		notePanel.setMinimumSize(new Dimension(350, 0));
		notePanel.setPreferredSize(new Dimension(350, 0));
		final Border borderline = BorderFactory.createLineBorder(Color.red);
		final TitledBorder warningborder = BorderFactory.createTitledBorder(
			borderline, "Warning");
		warningborder.setTitleColor(Color.red);
		note1 = new JPanel();
		note1.setBorder(warningborder);
		tensorFlowStatus.setBorder(new EmptyBorder(2, 5, 5, 5));
		note1.add(tensorFlowStatus);
		notePanel.add(note1);
		note1.setVisible(false);

		notePanel.add(Box.createVerticalGlue());

		final JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(taskContainer, BorderLayout.WEST);
		topPanel.add(notePanel, BorderLayout.EAST);

		add(topPanel, BorderLayout.PAGE_START);

		final JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		progressBarSpace = Box.createRigidArea(new Dimension(0, 20));
		centerPanel.add(progressBarSpace);
		centerPanel.add(progressBar);
		centerPanel.add(new JScrollPane(taskOutput));
		add(centerPanel, BorderLayout.CENTER);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		okButton = new JButton("Ok");
		okButton.setEnabled(false);
		cancelButton = new JButton("Cancel");

		resetProgress();

		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(15, 0, -5, -3));
		footer.setAlignmentX(Component.RIGHT_ALIGNMENT);
		footer.add(cancelButton);
		footer.add(okButton);

		add(footer, BorderLayout.SOUTH);

		invalidate();
		repaint();

		getOkBtn().addActionListener(e -> frame.dispose());
		setOpaque(true); // content panes must be opaque
		frame.setContentPane(this);

	}

	public void display() {
		// Display the window.
		try {
			threadService.invoke(() -> {
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			});
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void addTask(final String title) {
		final JPanel taskrow = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JLabel statusLabel = new JLabel("\u2013", SwingConstants.CENTER);
		final Font font = statusLabel.getFont();
		statusLabel.setFont(new Font(font.getName(), Font.BOLD, font.getSize() *
			2));
		statusLabel.setPreferredSize(new Dimension(50, 30));
		statusLabel.setMinimumSize(new Dimension(50, 30));
		statusLabel.setMaximumSize(new Dimension(50, 30));
		final GuiTask task = new GuiTask();
		task.status = statusLabel;
		task.title = new JLabel(title);
		task.taskDone = false;
		tasks.add(task);
		taskrow.add(task.status);
		taskrow.add(task.title);
		taskContainer.add(taskrow);
	}

	private void resetProgress() {
		for (final GuiTask step : tasks) {
			step.taskDone = false;
		}
		currentTask = -1;
		currentTaskFailing = false;
		try {
			threadService.invoke(() -> progressBar.setValue(0));
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void setProgressBarMax(final int value) {
		try {
			threadService.invoke(() -> progressBar.setMaximum(value));
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void setProgressBarValue(final int value) {
		try {
			threadService.invoke(() -> {
				progressBar.setValue(value);
				status.showProgress(value, progressBar.getMaximum());
			});
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private void updateGUI() {

		try {
			threadService.invoke(() -> {
				boolean alldone = true;
				for (final GuiTask task : tasks) {
					if (!task.taskDone) alldone = false;
				}
				okButton.setEnabled(alldone || currentTaskFailing);
				cancelButton.setEnabled(!alldone && !currentTaskFailing);

				invalidate();
			});
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}

	}

	public void setTaskStart(final int task) {
		currentTask = task;
		currentTaskFailing = false;
		setCurrentStepStatus(STATUS_RUNNING);
		updateGUI();
	}

	public void setTaskDone(final int task) {
		tasks.get(currentTask).taskDone = true;
		setStepStatus(task, STATUS_DONE);
		status.clearStatus();
		updateGUI();
	}

	public void setTaskFail(final int task) {
		currentTaskFailing = true;
		setStepStatus(task, STATUS_FAIL);
		status.clearStatus();
		updateGUI();
	}

	public void setTaskNumSteps(final int task, final int steps) {
		tasks.get(currentTask).numSteps = steps;
		setProgressBarMax(steps);
		updateGUI();
	}

	public void setTaskCurrentStep(final int task, final int step) {
		tasks.get(currentTask).step = step;
		setProgressBarValue(step);
		updateGUI();
	}

	private void setCurrentStepStatus(final int status) {
		setStepStatus(currentTask, status);
	}

	private void setStepStatus(final int task, final int status) {

		if (status < tasks.size() && task >= 0) {
			final JLabel statuslabel = tasks.get(task).status;
			switch (status) {
				case STATUS_IDLE:
					statuslabel.setText("\u2013");
					statuslabel.setForeground(Color.getHSBColor(0.6f, 0.f, 0.3f));
					break;
				case STATUS_RUNNING:
					statuslabel.setText("\u2794");
					statuslabel.setForeground(Color.getHSBColor(0.6f, 0.f, 0.3f));
					break;
				case STATUS_DONE:
					statuslabel.setText("\u2713");
					statuslabel.setForeground(Color.getHSBColor(0.3f, 1, 0.6f));
					break;
				case STATUS_FAIL:
					statuslabel.setText("\u2013");
					statuslabel.setForeground(Color.red);
					break;
			}
		}
	}

	public void addLog(final String data) {
		addLog(data, null);
	}

	public void addLog(final String data, final SimpleAttributeSet style) {
		final Date date = new Date();
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			threadService.invoke(() -> {
				try {
					taskOutput.getDocument().insertString(taskOutput.getDocument()
							.getLength(), df.format(date) + " | " + data + "\n", style);
					taskOutput.setCaretPosition(taskOutput.getDocument().getLength());
					taskOutput.invalidate();
					this.invalidate();
				}
				catch (final BadLocationException exc) {
					exc.printStackTrace();
				}
			});
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private void showGPUWarning() {
		try {
			threadService.invoke(() -> note1.setVisible(true));
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void updateTensorFlowStatus(TensorFlowVersion version) {
		if(version == null) return;
		if(!version.usesGPU().isPresent() || !version.usesGPU().get()) {
			if(UpdaterUtil.getPlatform().equals("macosx")) {
				tensorFlowStatus.setText("<html>Using CPU TensorFlow version.<br />" +
						"This will affect performance.<br /></html>");
			} else {
				tensorFlowStatus.setText("<html>Using CPU TensorFlow version. This will<br />" +
						"affect performance. For GPU support<br />" +
						"please make sure CUDA and CuDNN are<br />" +
						"linked and run:<br/>" +
						"<i>Edit > Options > TensorFlow...</i>.</html>");
			}
			showGPUWarning();
		}
	}

	public void addError(final String data) {
		addLog(data, red);
	}

	public static CSBDeepProgress create(StatusService status, ThreadService threadService) {

		// Create and set up the window.
		final JFrame frame = new JFrame("CSBDeep progress");

		// Create and set up the content pane.
		final CSBDeepProgress newContentPane = new CSBDeepProgress(frame, status, threadService);

		return newContentPane;
	}

	public void dispose() {
		status.showProgress(1,1);
		if(frame.isVisible()) frame.dispose();
	}

}

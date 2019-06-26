package de.csbdresden.csbdeep.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

import javax.swing.*;

import de.csbdresden.csbdeep.network.model.tensorflow.LibraryVersion;
import de.csbdresden.csbdeep.network.model.tensorflow.TensorFlowInstallationService;
import net.miginfocom.swing.MigLayout;

public class TensorFlowLibraryManagementFrame extends JFrame {

	TensorFlowInstallationService tensorFlowInstallationService;

	private JComboBox<String> gpuChoiceBox;
	private JComboBox<String> cudaChoiceBox;
	private JComboBox<String> tfChoiceBox;
	private List<LibraryVersion> availableVersions = new ArrayList<>();
	ButtonGroup versionGroup = new ButtonGroup();
	private List<JRadioButton> buttons = new ArrayList<>();
	private JPanel installPanel;
	private static String NOFILTER = "-";
	private JLabel status;
	private static Color listBackgroundColor = new Color(250,250,250);

	public TensorFlowLibraryManagementFrame(TensorFlowInstallationService tensorFlowInstallationService) {
		super("TensorFlow library version management");
		this.tensorFlowInstallationService = tensorFlowInstallationService;
	}

	public void init() {
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("height 400"));
		panel.add(new JLabel("Please select the TensorFlow version you would like to install."), "wrap");
		panel.add(createFilterPanel(), "wrap, span, align right");
		panel.add(createInstallPanel(), "wrap, span, grow");
		panel.add(createStatusPanel(), "span, grow");
		setContentPane(panel);
	}

	private Component createFilterPanel() {
		gpuChoiceBox = new JComboBox<>();
		gpuChoiceBox.addItem(NOFILTER);
		gpuChoiceBox.addItem("GPU");
		gpuChoiceBox.addItem("CPU");
		gpuChoiceBox.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateFilter();
			}
		});
		cudaChoiceBox = new JComboBox<>();
		cudaChoiceBox.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateFilter();
			}
		});
		tfChoiceBox = new JComboBox<>();
		tfChoiceBox.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateFilter();
			}
		});
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.add(makeLabel("Filter by.."));
		panel.add(makeLabel("Mode: "));
		panel.add(gpuChoiceBox);
		panel.add(makeLabel("CUDA: "));
		panel.add(cudaChoiceBox);
		panel.add(makeLabel("TensorFlow: "));
		panel.add(tfChoiceBox);
//		panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.darkGray));
		return panel;
	}

	private Component createStatusPanel() {
		JPanel statusPanel = new JPanel(new MigLayout());
		status = new JLabel();
		status.setFont(status.getFont().deriveFont(Font.PLAIN));
		statusPanel.add(status);
		return statusPanel;
	}

	private void updateFilter() {
		installPanel.removeAll();
		buttons.forEach(btn -> {
			if(filter("", gpuChoiceBox, btn)) return;
			if(filter("CUDA ", cudaChoiceBox, btn)) return;
			if(filter("TF ", tfChoiceBox, btn)) return;
			installPanel.add(btn);
		});
		installPanel.revalidate();
		installPanel.repaint();
	}

	private boolean filter(String title, JComboBox<String> choiceBox, JRadioButton btn) {
		if(choiceBox.getSelectedItem().toString().equals(NOFILTER)) return false;
		return !btn.getText().contains(title + choiceBox.getSelectedItem().toString());
	}

	private JLabel makeLabel(String s) {
		JLabel label = new JLabel(s);
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		label.setHorizontalTextPosition(SwingConstants.RIGHT);
		return label;
	}

	private Component createInstallPanel() {
		installPanel = new JPanel(new MigLayout("flowy"));
		JScrollPane scroll = new JScrollPane(installPanel);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		installPanel.setBackground(listBackgroundColor);
		return scroll;
	}

	public void updateChoices(List<LibraryVersion> availableVersions) {
		availableVersions.sort(Comparator.comparing(LibraryVersion::getOrderableTFVersion));
		this.availableVersions = availableVersions;
		updateCUDAChoices();
		updateTFChoices();
		versionGroup = new ButtonGroup();
		installPanel.removeAll();
		for( LibraryVersion version : availableVersions) {
			JRadioButton btn = new JRadioButton(version.toString());
			btn.setToolTipText(version.getNote());
			btn.setSelected(version.active);
			btn.setOpaque(false);
			versionGroup.add(btn);
			buttons.add(btn);
			btn.addActionListener(e -> {
				if(btn.isSelected()) {
					new Thread(() -> activateVersion(version)).start();
				}
			});
		}
		updateFilter();
		updateStatus();
	}

	private void updateStatus() {
		status.setText(tensorFlowInstallationService.getStatus());
		if(status.getText().toLowerCase().contains("error")) {
			status.setForeground(Color.red);
		} else {
			status.setForeground(Color.black);
		}
	}

	private void activateVersion(LibraryVersion version) {
		if(version.active) {
			System.out.println("[WARNING] Cannot activate version, already active: " + version);
			return;
		}
		showWaitMessage();
		if(!version.downloaded) {
			try {
				tensorFlowInstallationService.downloadLib(new URL(version.url));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		tensorFlowInstallationService.checkStatus(version);
		if(!version.active) {
			tensorFlowInstallationService.removeAllFromLib();
			try {
				tensorFlowInstallationService.installLib(version);
			} catch (IOException e) {
				Object[] options = {"Yes",
						"No",
						"Cancel"};
				int choice = JOptionPane.showOptionDialog(this,
						"Error while unpacking library file " + version.localPath + ":\n"
								+ e.getMessage() + "\nShould it be downloaded again?",
						"Unpacking library error",
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						options,
						options[0]);
				if(choice == 0) {
					version.downloaded = false;
					activateVersion(version);
				}
			}
			JOptionPane.showMessageDialog(null,
					"Installed selected TensorFlow version. Please restart Fiji to load it.",
					"Please restart",
					JOptionPane.PLAIN_MESSAGE);
			dispose();
		}
	}

	private void showWaitMessage() {
		status.setText("Please wait..");
	}

	public void updateCUDAChoices() {
		Set<String> choices = new LinkedHashSet<>();
		for(LibraryVersion version :availableVersions) {
			if(version.cuda != null)
				choices.add(version.cuda);
		}
		cudaChoiceBox.removeAllItems();
		cudaChoiceBox.addItem(NOFILTER);
		for(String choice : choices) {
			cudaChoiceBox.addItem(choice);
		}
	}

	public void updateTFChoices() {
		Set<String> choices = new LinkedHashSet<>();
		for(LibraryVersion version :availableVersions) {
			if(version.tfVersion != null) choices.add(version.tfVersion);
		}
		tfChoiceBox.removeAllItems();
		tfChoiceBox.addItem(NOFILTER);
		for(String choice : choices) {
			tfChoiceBox.addItem(choice);
		}
	}

}

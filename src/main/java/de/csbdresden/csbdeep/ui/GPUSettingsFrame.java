package de.csbdresden.csbdeep.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

import javax.swing.*;

import de.csbdresden.csbdeep.network.model.tensorflow.LibraryVersion;
import javafx.scene.control.ChoiceBox;
import net.miginfocom.swing.MigLayout;

public class GPUSettingsFrame extends JFrame {

	TensorFlowInstallationService tensorFlowInstallationService;

	private JComboBox<String> gpuChoiceBox;
	private JComboBox<String> cudaChoiceBox;
	private JComboBox<String> tfChoiceBox;
	private List<LibraryVersion> availableVersions = new ArrayList<>();
	ButtonGroup versionGroup = new ButtonGroup();
	private List<JRadioButton> buttons = new ArrayList<>();
	private JPanel installPanel;
	private static String NOFILTER = "-";

	public GPUSettingsFrame(TensorFlowInstallationService tensorFlowInstallationService) {
		super("TensorFlow library version management");
		this.tensorFlowInstallationService = tensorFlowInstallationService;
	}

	public void init() {
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.add(createFilterPanel(), "wrap");
		panel.add(createInstallPanel(), "wrap, span, grow, h 200:500:");
		panel.add(createActionPanel(), "");
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
		panel.add(new JLabel("Filter by.."));
		panel.add(makeLabel("Mode: "));
		panel.add(gpuChoiceBox);
		panel.add(makeLabel("CUDA: "));
		panel.add(cudaChoiceBox);
		panel.add(makeLabel("TensorFlow: "));
		panel.add(tfChoiceBox);
		return panel;
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
		label.setBackground(Color.red);
		return label;
	}

	private Component createInstallPanel() {
		installPanel = new JPanel(new MigLayout("flowy"));
		return new JScrollPane(installPanel);
	}

	private Component createActionPanel() {
		JPanel panel = new JPanel();
		return panel;
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
			versionGroup.add(btn);
			buttons.add(btn);
			btn.addActionListener(e -> {
				if(btn.isSelected()) {
					activateVersion(version);
				}
			});
		}
		updateFilter();
	}

	private void activateVersion(LibraryVersion version) {
		if(version.active) {
			System.out.println("[WARNING] Cannot activate version, already active: " + version);
			return;
		}
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
			tensorFlowInstallationService.installLib(version);
		}
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
			System.out.println(version);
			if(version.tfVersion != null) choices.add(version.tfVersion);
		}
		tfChoiceBox.removeAllItems();
		tfChoiceBox.addItem(NOFILTER);
		for(String choice : choices) {
			tfChoiceBox.addItem(choice);
		}
	}

}

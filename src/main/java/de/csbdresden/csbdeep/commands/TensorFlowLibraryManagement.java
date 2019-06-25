package de.csbdresden.csbdeep.commands;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import de.csbdresden.csbdeep.network.model.tensorflow.LibraryVersion;
import de.csbdresden.csbdeep.ui.TensorFlowLibraryManagementFrame;
import de.csbdresden.csbdeep.network.model.tensorflow.TensorFlowInstallationService;
import net.imagej.ImageJ;
import net.imagej.updater.util.UpdaterUtil;

@Plugin(type = Command.class, menuPath = "Plugins>CSBDeep>TF library management tool")
public class TensorFlowLibraryManagement implements Command {

	@Parameter(required = false)
	String errorMessage = "";

	@Parameter
	TensorFlowInstallationService tensorFlowInstallationService;

	LibraryVersion currentVersion;
	List<LibraryVersion> availableVersions = new ArrayList<>();

	String platform = UpdaterUtil.getPlatform();

	@Override
	public void run() {
		tensorFlowInstallationService.loadNativeLibrary();
		currentVersion = getCurrentVersion();
		TensorFlowLibraryManagementFrame frame = new TensorFlowLibraryManagementFrame(tensorFlowInstallationService);
		frame.init();
		initAvailableVersions();
		frame.updateChoices(availableVersions);
		frame.pack();
		frame.setMinimumSize(new Dimension(0, 200));
		frame.setVisible(true);

	}

	public List<LibraryVersion> initAvailableVersions() {
		addAvailableVersion(currentVersion);
		//linux64
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.2.0.tar.gz",
				"CPU", "linux64", "1.2.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.2.0.tar.gz",
				"GPU", "linux64", "1.2.0", "8.0", "5.1");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.3.0.tar.gz",
				"CPU", "linux64", "1.3.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.3.0.tar.gz",
				"GPU", "linux64", "1.3.0", "8.0", "6");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.4.1.tar.gz",
				"CPU", "linux64", "1.4.1");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.4.1.tar.gz",
				"GPU", "linux64", "1.4.1", "8.0", "6");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.5.0.tar.gz",
				"CPU", "linux64", "1.5.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.5.0.tar.gz",
				"GPU", "linux64", "1.5.0", "9.0", "7");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.6.0.tar.gz",
				"CPU", "linux64", "1.6.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.6.0.tar.gz",
				"GPU", "linux64", "1.6.0", "9.0", "7");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.7.0.tar.gz",
				"CPU", "linux64", "1.7.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.7.0.tar.gz",
				"GPU", "linux64", "1.7.0", "9.0", "7");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.8.0.tar.gz",
				"CPU", "linux64", "1.8.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.8.0.tar.gz",
				"GPU", "linux64", "1.8.0", "9.0", "7");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.9.0.tar.gz",
				"CPU", "linux64", "1.9.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.9.0.tar.gz",
				"GPU", "linux64", "1.9.0", "9.0", "7");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.10.0.tar.gz",
				"CPU", "linux64", "1.10.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.10.0.tar.gz",
				"GPU", "linux64", "1.10.0", "9.0", "7.?");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.11.0.tar.gz",
				"CPU", "linux64", "1.11.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.11.0.tar.gz",
				"GPU", "linux64", "1.11.0", "9.0", ">= 7.2");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.12.0.tar.gz",
				"CPU", "linux64", "1.12.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.12.0.tar.gz",
				"GPU", "linux64", "1.12.0", "9.0", ">= 7.2");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-1.13.1.tar.gz",
				"CPU", "linux64", "1.13.1");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.13.1.tar.gz",
				"GPU", "linux64", "1.13.1", "10.0", "7.4");
		//win64
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.2.0.zip",
				"CPU", "win64", "1.2.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.3.0.zip",
				"CPU", "win64", "1.3.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.4.1.zip",
				"CPU", "win64", "1.4.1");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.5.0.zip",
				"CPU", "win64", "1.5.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.6.0.zip",
				"CPU", "win64", "1.6.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.7.0.zip",
				"CPU", "win64", "1.7.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.8.0.zip",
				"CPU", "win64", "1.8.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.9.0.zip",
				"CPU", "win64", "1.9.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.10.0.zip",
				"CPU", "win64", "1.10.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.11.0.zip",
				"CPU", "win64", "1.11.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-windows-x86_64-1.12.0.zip",
				"GPU", "win64", "1.12.0", "9.0", ">= 7.2");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.12.0.zip",
				"CPU", "win64", "1.12.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-windows-x86_64-1.13.1.zip",
				"GPU", "win64", "1.13.1", "10.0", "7.4");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-windows-x86_64-1.13.1.zip",
				"CPU", "win64", "1.13.1");
		//macosx
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.2.0.tar.gz",
				"CPU", "macosx", "1.2.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.3.0.tar.gz",
				"CPU", "macosx", "1.3.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.4.1.tar.gz",
				"CPU", "macosx", "1.4.1");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.5.0.tar.gz",
				"CPU", "macosx", "1.5.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.6.0.tar.gz",
				"CPU", "macosx", "1.6.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.7.0.tar.gz",
				"CPU", "macosx", "1.7.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.8.0.tar.gz",
				"CPU", "macosx", "1.8.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.9.0.tar.gz",
				"CPU", "macosx", "1.9.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.10.0.tar.gz",
				"CPU", "macosx", "1.10.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.11.0.tar.gz",
				"CPU", "macosx", "1.11.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.12.0.tar.gz",
				"CPU", "macosx", "1.12.0");
		addAvailableVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-darwin-x86_64-1.13.1.tar.gz",
				"CPU", "macosx", "1.13.1");
		availableVersions.forEach(lib -> tensorFlowInstallationService.checkStatus(lib));
		return availableVersions;
	}

	private void addAvailableVersion(LibraryVersion version) {
		if(!version.platform.equals(platform)) return;
		boolean exists = false;
		for (LibraryVersion other : availableVersions) {
			if (other.equals(version)) {
				if (other.cudnn == null) {
					other.cudnn = version.cudnn;
				}
				if (other.cuda == null) {
					other.cuda = version.cuda;
				}
				exists = true;
				continue;
			}
		}
		if(!exists) availableVersions.add(version);
	}

	private void addAvailableVersion(String url, String mode, String os, String tfVersion) {
		addAvailableVersion(new LibraryVersion(url, mode, os, tfVersion, null, null));
	}

	private void addAvailableVersion(String url, String mode, String os, String tfVersion, String cudaVersion, String cudnnVersion) {
		addAvailableVersion(new LibraryVersion(url, mode, os, tfVersion, cudaVersion, cudnnVersion));
	}

	public LibraryVersion getCurrentVersion() {
		return tensorFlowInstallationService.getCurrentVersion();
	}

	public static void main(String... args) {
		ImageJ ij = new ImageJ();
		ij.command().run(TensorFlowLibraryManagement.class, true);
	}

}

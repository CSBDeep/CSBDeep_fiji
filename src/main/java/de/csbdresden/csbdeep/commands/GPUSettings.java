package de.csbdresden.csbdeep.commands;

import java.util.ArrayList;
import java.util.List;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import de.csbdresden.csbdeep.network.model.tensorflow.LibraryVersion;

@Plugin(type = Command.class, menuPath = "Plugins>CSBDeep>GPU library management tool")
public class GPUSettings implements Command {

	static List<LibraryVersion> availableVersions = getAvailableVersions();

	@Override
	public void run() {

	}

	public static List<LibraryVersion> getAvailableVersions() {
		List<LibraryVersion> res = new ArrayList<>();
		res.add(new LibraryVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.12.0.tar.gz",
				"linux64", "1.12.0", "9.0", ">= 7.2"));
		res.add(new LibraryVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-windows-x86_64-1.12.0.zip",
				"win64", "1.12.0", "9.0", ">= 7.2"));
		res.add(new LibraryVersion("https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-1.11.0.tar.gz",
				"linux64", "1.11.0", "9.0", ">= 7.2"));
		return res;
	}

}

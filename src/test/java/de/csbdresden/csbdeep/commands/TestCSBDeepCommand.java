package de.csbdresden.csbdeep.commands;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import net.imagej.Dataset;
import net.imagej.ImageJ;

public class TestCSBDeepCommand implements Command {

	@Parameter
	Dataset input;

	@Parameter
	protected boolean normalizeInput = true;
	@Parameter
	protected float percentileBottom = 3.0f;
	@Parameter
	protected float percentileTop = 99.8f;

	@Parameter
	CommandService command;

	@Parameter
	UIService ui;

	@Override
	public void run() {
		try {
			Module module = command.run(GenericNetwork.class, false, "input", input,
					"normalizeInput", normalizeInput,
					"percentileBottom", percentileBottom,
					"percentileTop", percentileTop,
					"modelUrl", "http://csbdeep.bioimagecomputing.com/model-project.zip").get();
			ui.show(module.getOutput("output"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) throws IOException {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch(args);

		// ask the user for a file to open
		final File file = ij.ui().chooseFile(null, "open");

		if (file != null && file.exists()) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open(file
					.getAbsolutePath());

			// show the image
			ij.ui().show(dataset);

			// invoke the plugin
			ij.command().run(TestCSBDeepCommand.class, true);
		}
	}

}

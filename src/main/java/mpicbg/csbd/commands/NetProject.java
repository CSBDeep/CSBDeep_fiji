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

package mpicbg.csbd.commands;

import java.io.File;
import java.io.IOException;
import java.util.OptionalLong;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;

/**
 */
@Plugin(type = Command.class,
	menuPath = "Plugins>CSBDeep>Demo>Surface Projection - Flywing", headless = true)
public class NetProject extends CSBDeepCommand implements Command {

	@Override
	public void initialize() {

		super.initialize();

		modelFileUrl = "http://csbdeep.bioimagecomputing.com/model-project.zip";
		modelName = "net_project";

	}

	@Override
	public void run() {
		tryToInitialize();
		network.setDoDimensionReduction(true, Axes.Z);
		try {
			validateInput(getInput(), "3D grayscale image with dimension order X-Y-Z",
				OptionalLong.empty(), OptionalLong.empty(), OptionalLong.empty());
			super.run();
		}
		catch (final IOException e) {
			showError(e.getMessage());
		}
	}

	public static void main(final String... args) throws Exception {

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
			if (!ij.ui().isHeadless()) {
				ij.ui().show(dataset);
			}

			// invoke the plugin
			ij.command().run(NetProject.class, true);
		}

	}
}

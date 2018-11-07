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

package org.csbdeep.commands;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.csbdeep.util.DatasetHelper;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;

/**
 */
@Plugin(type = Command.class,
	menuPath = "Plugins>CSBDeep>Demo>Deconvolution - Microtubules", headless = true)
public class NetTubulin implements Command {

	@Parameter(type = ItemIO.INPUT)
	public Dataset input;

	@Parameter(type = ItemIO.OUTPUT)
	protected List<Dataset> output = new ArrayList<>();

	@Parameter(label = "Number of tiles", min = "1")
	protected int nTiles = 8;

	@Parameter(label="Show process dialog")
	protected boolean showProcessDialog = true;

	@Parameter
	CommandService commandService;

	@Parameter
	UIService uiService;

	@Parameter(label = "Batch size", min = "1")
	protected int batchSize = 10;
	private String url = "http://csbdeep.bioimagecomputing.com/model-tubulin.zip";

//	@Override
//	protected Tiling.TilingAction[] getTilingActions() {
//		Tiling.TilingAction[] actions = super.getTilingActions();
//		if (getInput().numDimensions() == 3) {
//			int batchDim = network.getInputNode().getDatasetDimIndexByTFIndex(0);
//			if (batchDim >= 0) {
//				actions[batchDim] = Tiling.TilingAction.TILE_WITHOUT_PADDING;
//			}
//		}
//		return actions;
//	}

	@Override
	public void run() {
		boolean validInput = DatasetHelper.validate(input, "3D image with size order X-Y-T, checking if it is a 2D image (also valid)", uiService.isHeadless(),
			OptionalLong.empty(), OptionalLong.empty(), OptionalLong.empty());
		if(!validInput) {
			validInput = DatasetHelper.validate(input, "2D image with size order X-Y", uiService.isHeadless(),
				OptionalLong.empty(), OptionalLong.empty());
		}
		if(!validInput) return;
//			final AxisType[] mapping = { Axes.TIME, Axes.Y, Axes.X, Axes.CHANNEL };
//			setMapping(mapping);
		try {
			final CommandModule module = commandService.run(
					GenericNetwork.class, false,
					"input", input,
					"modelUrl", url,
	//					"batchSize", batchSize,
	//					"batchAxis", Axes.TIME.getLabel(),
					"blockMultiple", 8,
					"nTiles", nTiles,
					"showProcessDialog", showProcessDialog).get();
			output.addAll((Collection) module.getOutput("output"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
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
			ij.ui().show(dataset);

			// invoke the plugin
			ij.command().run(NetTubulin.class, true);
		}

	}
}

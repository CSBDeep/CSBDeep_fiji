/*
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.widget.Button;

import mpicbg.csbd.normalize.task.DefaultInputNormalizer;
import mpicbg.csbd.ui.MappingDialog;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;

/**
 */
@Plugin(type = Command.class, menuPath = "Plugins>CSBDeep>Run your network")
public class GenericNetwork extends CSBDeepCommand implements Command {

	@Parameter(visibility = ItemVisibility.MESSAGE)
	protected String normtext = "Normalization";
	@Parameter
	protected boolean normalizeInput = true;
	@Parameter
	protected float percentileBottom = 3.0f;
	@Parameter
	protected float percentileTop = 99.8f;

	protected float min = 0;
	protected float max = 1;

	@Parameter(label = "Clip normalization")
	protected boolean clip = false;

	@Parameter(label = "Import model (.zip)", callback = "modelChanged",
		initializer = "modelInitialized", persist = false)
	private File modelFile;
	private final String modelFileKey = "modelfile-anynetwork";

	private String cacheName = "generic";

	@Parameter(label = "Adjust image <-> tensorflow mapping",
		callback = "openTFMappingDialog")
	private Button changeTFMapping;

	@Parameter
	private PrefService prefService;

	private boolean modelChangeCallbackCalled = false;

	private ExecutorService pool = null;
	private Future<?> modelLoadingFuture = null;

	/** Executed whenever the {@link #modelFile} parameter is initialized. */
	protected void modelInitialized() {
		System.out.println("modelInitialized");
		final String p_modelfile = prefService.get(String.class, modelFileKey, "");
		if (p_modelfile != "") {
			modelFile = new File(p_modelfile);
			if(modelFile != null) {
				updateCacheName();
				savePreferences();
				tryToInitialize();
				prepareInputAndNetwork();
			}
		}
	}

	private void updateCacheName() {
		if(modelFile != null) {
			if(modelFile.exists()){
				try {
					FileInputStream fis = new FileInputStream(modelFile);
					String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
					cacheName = "generic_" + md5;
					savePreferences();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/** Executed whenever the {@link #modelFile} parameter changes. */
	protected void modelChanged() {
		if(!modelChangeCallbackCalled) {
			modelChangeCallbackCalled = true;
			System.out.println("modelChanged");
			restartPool();
			modelLoadingFuture = pool.submit(() -> {
				updateCacheName();
				savePreferences();
				if (initialized) {
					network.dispose();
				} else {
					tryToInitialize();
				}
				prepareInputAndNetwork();
				modelChangeCallbackCalled = false;
			});
		}
	}

	private void restartPool() {
		if(pool != null) {
			pool.shutdownNow();
		}
		pool = Executors.newSingleThreadExecutor();
	}

	private void finishModelLoading() {
		if(modelLoadingFuture != null) {
			try {
				modelLoadingFuture.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		if(pool != null) {
			pool.shutdown();
			pool = null;
		}
	}

	protected void openTFMappingDialog() {
		System.out.println("openTFMappingDialog");
		finishModelLoading();
		MappingDialog.create(network.getInputNode(), network.getOutputNode());
	}

	@Override
	public void run() {
		finishModelLoading();
		updateCacheName();
		savePreferences();
		tryToInitialize();
		prepareInputAndNetwork();
		checkAndResolveDimensionReduction();
		super.run();
	}

	@Override
	protected void setupNormalizer() {
		((DefaultInputNormalizer) inputNormalizer).getNormalizer().setup(
			new float[] { percentileBottom, percentileTop }, new float[] { min,
				max }, clip);
	}

	@Override
	protected boolean doInputNormalization() {
		return normalizeInput;
	}

	@Override
	protected void prepareInputAndNetwork() {
		if(modelFile != null) {
			modelFileUrl = modelFile.getAbsolutePath();
		}
		modelName = cacheName;
		super.prepareInputAndNetwork();
		checkAndResolveDimensionReduction();
	}

	private void checkAndResolveDimensionReduction() {
		for (int i = 0; i < getInput().numDimensions(); i++) {
			AxisType axis = getInput().axis(i).type();
			if (!network.getOutputNode().getNodeAxes().contains(axis)) {
				// log("Network input node axis " + axis.getLabel() + " not present in
				// output node, will be reduced");
				network.setDoDimensionReduction(true, axis);
			}
		}
		network.doDimensionReduction();
	}

	private void savePreferences() {
		if(modelFile != null) {
			prefService.put(String.class, modelFileKey, modelFile.getAbsolutePath());
		}
	}

	/**
	 * This main function serves for development purposes. It allows you to run
	 * the plugin immediately out of your integrated development environment
	 * (IDE).
	 *
	 * @param args whatever, it's ignored
	 * @throws Exception
	 */
	public static void main(final String... args) throws Exception {

		final ImageJ ij = new ImageJ();

		ij.launch(args);

		// ask the user for a file to open
		 final File file = ij.ui().chooseFile( null, "open" );

		if (file != null && file.exists()) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open(file
				.getAbsolutePath());

			// show the image
			ij.ui().show(dataset);

			// invoke the plugin
			ij.command().run(GenericNetwork.class, true);
		}

	}

}

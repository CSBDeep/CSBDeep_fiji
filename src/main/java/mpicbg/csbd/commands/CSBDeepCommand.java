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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.*;

import org.scijava.*;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import mpicbg.csbd.network.ImageTensor;
import mpicbg.csbd.network.Network;
import mpicbg.csbd.network.task.*;
import mpicbg.csbd.network.tensorflow.TensorFlowNetwork;
import mpicbg.csbd.normalize.task.DefaultInputNormalizer;
import mpicbg.csbd.normalize.task.InputNormalizer;
import mpicbg.csbd.task.Task;
import mpicbg.csbd.task.TaskForceManager;
import mpicbg.csbd.task.TaskManager;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.DefaultTiling;
import mpicbg.csbd.tiling.Tiling;
import mpicbg.csbd.tiling.task.DefaultInputTiler;
import mpicbg.csbd.tiling.task.DefaultOutputTiler;
import mpicbg.csbd.tiling.task.InputTiler;
import mpicbg.csbd.tiling.task.OutputTiler;
import mpicbg.csbd.util.DatasetHelper;
import mpicbg.csbd.util.task.DefaultInputProcessor;
import mpicbg.csbd.util.task.DefaultOutputProcessor;
import mpicbg.csbd.util.task.InputProcessor;
import mpicbg.csbd.util.task.OutputProcessor;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

public abstract class CSBDeepCommand<T extends RealType<T>> implements
	Cancelable, Initializable, Disposable
{

	@Parameter(type = ItemIO.INPUT, initializer = "processDataset")
	public Dataset input;

	@Parameter
	protected LogService log;

	@Parameter
	protected TensorFlowService tensorFlowService;

	@Parameter
	protected DatasetService datasetService;

	@Parameter
	protected UIService uiService;

	@Parameter
	protected OpService opService;

	@Parameter(label = "Number of tiles", min = "1")
	protected int nTiles = 8;

	@Parameter(label = "Overlap between tiles", min = "0", stepSize = "16")
	protected int overlap = 32;

	@Parameter(type = ItemIO.OUTPUT)
	protected List<Dataset> output = new ArrayList<>();

	protected String modelFileUrl;
	protected String modelName;
	protected String inputNodeName = "input";
	protected String outputNodeName = "output";
	protected int blockMultiple = 8;

	protected TaskManager taskManager;

	protected Network network;
	protected Tiling tiling;

	protected InputProcessor inputProcessor;
	protected InputMapper inputMapper;
	protected InputNormalizer inputNormalizer;
	protected InputTiler inputTiler;
	protected ModelLoader modelLoader;
	protected ModelExecutor modelExecutor;
	protected OutputTiler outputTiler;
	protected OutputProcessor outputProcessor;

	protected boolean initialized = false;

	private ExecutorService pool = null;
	private Future<?> future;

	@Override
	public void initialize() {
		initialized = true;
		initTasks();
		initTaskManager();
		initNetwork();
	}

	protected void tryToInitialize() {
		if (!initialized) {
			initialize();
		}
	}

	protected boolean initNetwork() {
		network = new TensorFlowNetwork(tensorFlowService, datasetService,
			modelExecutor);
		if(network.libraryLoaded()) {
			network.testGPUSupport();
			if(!network.supportsGPU()) taskManager.noGPUFound();
		}else {
			return false;
		}
		return true;
	}

	protected void initTasks() {
		inputMapper = initInputMapper();
		inputProcessor = initInputProcessor();
		inputNormalizer = initInputNormalizer();
		inputTiler = initInputTiler();
		modelLoader = initModelLoader();
		modelExecutor = initModelExecutor();
		outputTiler = initOutputTiler();
		outputProcessor = initOutputProcessor();
	}

	protected void initTaskManager() {
		final TaskForceManager tfm = new TaskForceManager(isHeadless(), log);
		tfm.initialize();
		tfm.createTaskForce("Preprocessing", modelLoader, inputMapper,
			inputProcessor, inputNormalizer);
		tfm.createTaskForce("Tiling", inputTiler);
		tfm.createTaskForce("Execution", modelExecutor);
		tfm.createTaskForce("Postprocessing", outputTiler, outputProcessor);
		taskManager = tfm;
	}

	protected InputMapper initInputMapper() {
		return new DefaultInputMapper();
	}

	protected InputProcessor initInputProcessor() {
		return new DefaultInputProcessor();
	}

	protected InputNormalizer initInputNormalizer() {
		return new DefaultInputNormalizer();
	}

	protected InputTiler initInputTiler() {
		return new DefaultInputTiler();
	}

	protected ModelLoader initModelLoader() {
		return new DefaultModelLoader();
	}

	protected ModelExecutor initModelExecutor() {
		return new DefaultModelExecutor();
	}

	protected OutputTiler initOutputTiler() {
		return new DefaultOutputTiler();
	}

	protected OutputProcessor initOutputProcessor() {
		return new DefaultOutputProcessor();
	}

	public void run() {

		if (noInputData()) return;

		pool = Executors.newSingleThreadExecutor();

		future = pool.submit(() -> {
			tryToInitialize();

			taskManager.finalizeSetup();

			prepareInputAndNetwork();

			final Dataset normalizedInput;
			if (doInputNormalization()) {
				setupNormalizer();
				normalizedInput = inputNormalizer.run(getInput(), opService,
						datasetService);
			} else {
				normalizedInput = getInput();
			}

			final List<RandomAccessibleInterval<T>> processedInput = inputProcessor.run(
					normalizedInput);

			log("INPUT NODE: ");
			network.getInputNode().printMapping();
			log("OUTPUT NODE: ");
			network.getOutputNode().printMapping();

			initTiling();
			try {
				final List<AdvancedTiledView<FloatType>> tiledOutput =
						tryToTileAndRunNetwork(processedInput);
				if(tiledOutput != null) {
					final List<RandomAccessibleInterval<FloatType>> output = outputTiler.run(
							tiledOutput, tiling, getAxesArray(network.getOutputNode()));
					for (AdvancedTiledView obj : tiledOutput) {
						obj.dispose();
					}
					this.output.clear();
					this.output.addAll(outputProcessor.run(output, getInput(), network,
							datasetService));
				}
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			}

		});

		try {
			if(future != null) future.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		dispose();

	}

	protected void setupNormalizer() {
		// do nothing, use normalizer default values
	}

	protected boolean doInputNormalization() {
		return true;
	}

	protected void prepareInputAndNetwork() {
		modelLoader.run(modelName, network, modelFileUrl, inputNodeName,
			outputNodeName, getInput());
		inputMapper.run(getInput(), network);
	}

	@Override
	public void dispose() {
		if (taskManager != null) {
			taskManager.close();
		}
		if (network != null) {
			network.dispose();
		}
		if(pool != null) {
			pool.shutdown();
		}
		pool = null;
	}

	private AxisType[] getAxesArray(final ImageTensor outputNode) {
		int numDim = outputNode.numDimensions();
		boolean addChannel = false;
		if (numDim < outputNode.getNodeShape().length && outputNode
			.getNodeShape()[outputNode.getNodeShape().length - 1] > 1)
		{
			addChannel = true;
			numDim++;
		}
		final AxisType[] res = new AxisType[numDim];
		for (int i = 0; i < outputNode.numDimensions(); i++) {
			res[i] = outputNode.getAxisByDatasetDim(i);
		}
		if (addChannel) {
			res[res.length - 1] = Axes.CHANNEL;
		}
		return res;
	}

	protected void initTiling() {
		tiling = new DefaultTiling(nTiles, 1, blockMultiple, overlap);
	}

	private List<AdvancedTiledView<FloatType>> tryToTileAndRunNetwork(
		final List<RandomAccessibleInterval<T>> normalizedInput)
		throws OutOfMemoryError
	{
		List<AdvancedTiledView<FloatType>> tiledOutput = null;

		boolean isOutOfMemory = true;
		boolean canHandleOutOfMemory = true;

		while (isOutOfMemory && canHandleOutOfMemory) {
			try {
				final List<AdvancedTiledView<T>> tiledInput = inputTiler.run(
					normalizedInput, getAxesArray(getInput()), tiling, getTilingActions());
				if(tiledInput != null) {
					tiledOutput = modelExecutor.run(tiledInput, network);
				}
				isOutOfMemory = false;

			}
			catch (final OutOfMemoryError e) {
				isOutOfMemory = true;
				canHandleOutOfMemory = tryHandleOutOfMemoryError();
			}
		}

		if (isOutOfMemory) throw new OutOfMemoryError(
			"Out of memory exception occurred. Plugin exit.");

		return tiledOutput;
	}

	private AxisType[] getAxesArray(Dataset input) {
		AxisType[] res = new AxisType[input.numDimensions()];
		for (int i = 0; i < input.numDimensions(); i++) {
			res[i] = input.axis(i).type();
		}
		return res;
	}

	protected Tiling.TilingAction[] getTilingActions() {
		Tiling.TilingAction[] actions = new Tiling.TilingAction[getInput()
			.numDimensions()];
		Arrays.fill(actions, Tiling.TilingAction.NO_TILING);
		for (int i = 0; i < actions.length; i++) {
			AxisType type = getInput().axis(i).type();
			if (type.isSpatial()) {
				actions[i] = Tiling.TilingAction.TILE_WITH_PADDING;
			}
		}
		return actions;
	}

	public void setMapping(final AxisType[] mapping) {
		inputMapper.setMapping(mapping);
	}

	private boolean noInputData() {
		return getInput() == null;
	}

	private boolean tryHandleOutOfMemoryError() {
		// We expect it to be an out of memory exception and
		// try it again with more tiles.
		final Task modelExecutorTask = modelExecutor;
		if (!handleOutOfMemoryError()) {
			modelExecutorTask.setFailed();
			return false;
		}
		modelExecutorTask.logError(
			"Out of memory exception occurred. Trying with " + nTiles +
				" tiles and overlap " + overlap + "...");
		initTiling();
		modelExecutorTask.startNewIteration();
		((Task) inputTiler).addIteration();
		return true;
	}

	protected boolean handleOutOfMemoryError() {
		nTiles *= 2;
		// Check if the number of tiles is too large already
		if (Arrays.stream(Intervals.dimensionsAsLongArray(getInput())).max()
			.getAsLong() / nTiles < blockMultiple)
		{
			if (overlap == 0) return false;
			overlap *= 0.5;
			if (overlap < 2) overlap = 0;
		}
		return true;
	}

	protected static void showError(final String errorMsg) {
		JOptionPane.showMessageDialog(null, errorMsg, "Error",
			JOptionPane.ERROR_MESSAGE);
	}

	public Dataset getInput() {
		return input;
	}

	public void validateInput(final Dataset dataset, final String formatDesc,
		final OptionalLong... expectedDims) throws IOException
	{
		DatasetHelper.validate(dataset, formatDesc, expectedDims);
	}

	@Override
	public String getCancelReason() {
		return null;
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void cancel(final String reason) {
		if(future != null) {
			future.cancel(true);
		}
		if(pool != null) {
			pool.shutdownNow();
		}
		dispose();
	}

	protected void log(final String msg) {
		if (taskManager != null) {
			taskManager.log(msg);
		}
		else {
			System.out.println(msg);
		}
	}

	protected boolean isHeadless() {
		return uiService.isHeadless();
	}

}

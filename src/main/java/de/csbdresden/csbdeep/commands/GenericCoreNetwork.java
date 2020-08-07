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

package de.csbdresden.csbdeep.commands;

import de.csbdresden.csbdeep.io.DefaultInputProcessor;
import de.csbdresden.csbdeep.io.InputProcessor;
import de.csbdresden.csbdeep.io.OutputProcessor;
import de.csbdresden.csbdeep.network.DefaultInputMapper;
import de.csbdresden.csbdeep.network.DefaultInputValidator;
import de.csbdresden.csbdeep.network.DefaultModelExecutor;
import de.csbdresden.csbdeep.network.DefaultModelLoader;
import de.csbdresden.csbdeep.network.InputMapper;
import de.csbdresden.csbdeep.network.InputValidator;
import de.csbdresden.csbdeep.network.ModelExecutor;
import de.csbdresden.csbdeep.network.ModelLoader;
import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.network.model.tensorflow.TensorFlowNetwork;
import de.csbdresden.csbdeep.normalize.DefaultInputNormalizer;
import de.csbdresden.csbdeep.normalize.InputNormalizer;
import de.csbdresden.csbdeep.task.Task;
import de.csbdresden.csbdeep.task.TaskForceManager;
import de.csbdresden.csbdeep.task.TaskManager;
import de.csbdresden.csbdeep.tiling.AdvancedTiledView;
import de.csbdresden.csbdeep.tiling.DefaultInputTiler;
import de.csbdresden.csbdeep.tiling.DefaultOutputTiler;
import de.csbdresden.csbdeep.tiling.DefaultTiling;
import de.csbdresden.csbdeep.tiling.InputTiler;
import de.csbdresden.csbdeep.tiling.OutputTiler;
import de.csbdresden.csbdeep.tiling.Tiling;
import de.csbdresden.csbdeep.ui.MappingDialog;
import de.csbdresden.csbdeep.util.IOHelper;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Cancelable;
import org.scijava.Context;
import org.scijava.Disposable;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Plugin(type = Command.class)
public abstract class GenericCoreNetwork implements
		Command, Cancelable, Initializable, Disposable
{

	@Parameter(type = ItemIO.INPUT, initializer = "input")
	public Dataset input;

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

	@Parameter(label = "Number of tiles", min = "1")
	protected int nTiles = 8;

	@Parameter(label = "Tile size has to be multiple of", min = "1")
	protected int blockMultiple = 32;

	@Parameter(label = "Overlap between tiles", min = "0", stepSize = "16")
	protected int overlap = 32;

	@Parameter(label = "Batch size", min = "1")
	protected int batchSize = 1;

	@Parameter
	private Context context;

	private boolean canceled = false;
	private boolean modelNeedsInitialization = false;
	private boolean networkInitialized;
	private boolean networkAndInputCompatible;

	public enum NetworkInputSourceType { UNSET, FILE, URL }
	
	private NetworkInputSourceType networkInputSourceType = NetworkInputSourceType.UNSET;

	@Parameter(label = "Import model (.zip)", callback = "modelFileChanged",
			initializer = "modelFileInitialized", persist = false, required = false)
	private File modelFile;

	@Parameter(label = "Import model (.zip) from URL", callback = "modelUrlChanged",
			initializer = "modelUrlChanged", persist = false, required = false)
	protected String modelUrl;

	@Parameter(label = "Adjust mapping of TF network input",
			callback = "openTFMappingDialog")
	private Button changeTFMapping;

	@Parameter(label="Show progress dialog")
	protected boolean showProgressDialog = true;

	@Parameter
	protected LogService log;

	@Parameter
	protected StatusService status;

	@Parameter
	protected TensorFlowService tensorFlowService;

	@Parameter
	protected DatasetService datasetService;

	@Parameter
	protected UIService uiService;

	@Parameter
	protected OpService opService;

	@Parameter
	private PrefService prefService;

	@Parameter
	private ThreadService threadService;

	protected String modelName;

	protected TaskManager taskManager;

	protected Network network;
	protected Tiling tiling;

	protected InputProcessor inputProcessor;
	protected InputValidator inputValidator;
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

	protected String cacheName;
	protected String modelFileKey;
	private String modelFileUrl = "";

	private int oldNTiles;
	private int oldBatchesSize;

	protected void openTFMappingDialog() {
		threadService.run(() -> {
			tryToInitialize();
			solveModelSource();
			initiateModelIfNeeded();
			try {
				threadService.invoke(() -> MappingDialog.create(network.getInputNode(), network.getOutputNode()));
			} catch (InterruptedException | InvocationTargetException e) {
				e.printStackTrace();
			}
		});
	}

	/** Executed whenever the {@link #modelFile} parameter is initialized. */
	protected void modelFileInitialized() {
		final String p_modelfile = prefService.get(this.getClass(), modelFileKey, "");
		if (!p_modelfile.isEmpty()) {
			modelFile = new File(p_modelfile);
			if(modelFile.exists()) {
				modelFileChanged();
			}
		}
	}

	private void updateCacheName() {
		switch(networkInputSourceType) {
			case UNSET:
			default:
				break;
			case FILE:
				cacheName = getFileCacheName(this.getClass(), modelFile);
				if(cacheName != null) savePreferences();
				break;
			case URL:
				cacheName = getUrlCacheName(this.getClass(), modelUrl);
				if(cacheName != null) savePreferences();
				break;
		}
	}

	public static String getUrlCacheName(Class commandClass, String modelUrl) {
		try {
			return IOHelper.getUrlCacheName(commandClass, modelUrl);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getFileCacheName(Class commandClass, File modelFile) {
		try {
			return IOHelper.getFileCacheName(commandClass, modelFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected void modelFileChanged() {
		if (modelFile != null) {
			if(modelFile.exists()) {
				modelUrl = null;
				networkInputSourceType = NetworkInputSourceType.FILE;
				modelFileUrl = modelFile.getAbsolutePath();
				modelChanged();
			}else {
				error("Model file " + modelFile.getAbsolutePath() + " does not exist.");
			}
		}
	}

	protected void modelUrlChanged() {
		if(modelUrl != null && modelUrl.length() > new String("https://").length()) {
			if (IOHelper.urlExists(modelUrl)) {
				modelFile = null;
				networkInputSourceType = NetworkInputSourceType.URL;
				modelFileUrl = modelUrl;
				modelChanged();
				return;
			}
		}
	}

	protected void modelChanged() {
		updateCacheName();
		modelNeedsInitialization = true;
		savePreferences();
		if (networkInitialized) {
			network.clear();
		}
	}

	protected void initiateModelIfNeeded() {
		if(modelNeedsInitialization) {
			try {
				tryToPrepareInputAndNetwork();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void initialize() {
		initialized = true;
		cacheName = this.getClass().getSimpleName();
		modelFileKey = getModelFileKey();
		initTasks();
		initNetwork();
		if (!network.libraryLoaded()) {
			this.cancel("TensorFlow library could not be loaded");
			return;
		}
		initTaskManager();
	}

	public String getModelFileKey() {
		return this.getClass().getSimpleName() + "_modelfile";
	}

	protected void tryToInitialize() {
		if (!initialized) {
			initialize();
		}
	}

	protected boolean initNetwork() {
		networkInitialized = true;
		network = new TensorFlowNetwork(modelExecutor);
		context.inject(network);
		network.loadLibrary();
		if(!network.libraryLoaded()) {
			return false;
		}
		return true;
	}

	protected void initTasks() {
		inputValidator = initInputValidator();
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
		final TaskForceManager tfm = new TaskForceManager(isHeadless() || !showProgressDialog);
		context.inject(tfm);
		tfm.initialize();
		tfm.createTaskForce("Preprocessing", modelLoader, inputValidator, inputMapper,
			inputProcessor, inputNormalizer);
		tfm.createTaskForce("Tiling", inputTiler);
		tfm.createTaskForce("Execution", modelExecutor);
		tfm.createTaskForce("Postprocessing", outputTiler, outputProcessor);
		taskManager = tfm;
	}

	protected InputValidator initInputValidator() {
		return new DefaultInputValidator();
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

	protected abstract OutputProcessor initOutputProcessor();

	protected void initTiling() {
		tiling = new DefaultTiling(nTiles, batchSize, blockMultiple, overlap);
	}

	public void run() {

		if(isCanceled()) return;

		final long startTime = System.currentTimeMillis();

		if (noInputData()) return;

		pool = Executors.newSingleThreadExecutor();

		try {

			future = pool.submit(this::mainThread);
			if(future != null) future.get();

		} catch(CancellationException e) {
			log.warn("CSBDeep canceled.");
			dispose();
			return;
		} catch(OutOfMemoryError | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		dispose();

		log("CSBDeep plugin exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");

	}

	protected void mainThread() throws OutOfMemoryError {

		tryToInitialize();
		taskManager.finalizeSetup();
		solveModelSource();

		updateCacheName();
		savePreferences();

		initiateModelIfNeeded();
		if(!networkAndInputCompatible) return;

		final Dataset normalizedInput;
		if (doInputNormalization()) {
			setupNormalizer();
			normalizedInput = inputNormalizer.run(getInput(), opService,
					datasetService);
		} else {
			normalizedInput = getInput();
			inputNormalizer.setFinished();
		}

		final List<RandomAccessibleInterval> processedInput = inputProcessor.run(
				normalizedInput, network);

		log("INPUT NODE: ");
		network.getInputNode().printMapping(inputProcessor);
		log("OUTPUT NODE: ");
		network.getOutputNode().printMapping(inputProcessor);

		initTiling();
		List<AdvancedTiledView<FloatType>> tiledOutput = null;
		try {
			tiledOutput = tryToTileAndRunNetwork(processedInput);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		if(tiledOutput != null) {
			final List<RandomAccessibleInterval<FloatType>> output;
			if(network.getOutputNode().getTilingAllowed()) {
				output = outputTiler.run(
						tiledOutput, tiling, network.getOutputNode().getFinalAxesArray());
			} else {
				output = tiledOutput.stream().map(tile -> getSingleTile(tile)).collect(Collectors.toList());
			}
			for (AdvancedTiledView obj : tiledOutput) {
				obj.dispose();
			}
			computeOutput(output);
		}

	}

	protected abstract void computeOutput(List<RandomAccessibleInterval<FloatType>> output);

	private RandomAccessibleInterval<FloatType> getSingleTile(AdvancedTiledView<FloatType> tile) {
		return tile.getProcessedTiles().get(0);
	}

	private void solveModelSource() {
		if(modelFileUrl.isEmpty()) modelFileChanged();
		if(modelFileUrl.isEmpty()) modelUrlChanged();
	}

	protected void setupNormalizer() {
		((DefaultInputNormalizer) inputNormalizer).getNormalizer().setup(
				new float[] { percentileBottom, percentileTop }, new float[] { min,
						max }, clip);
	}

	protected boolean doInputNormalization() {
		return normalizeInput;
	}

	protected void tryToPrepareInputAndNetwork() throws FileNotFoundException {

		networkAndInputCompatible = false;

		modelName = cacheName;

		if(!networkInitialized) {
			initNetwork();
		}
		if(!network.libraryLoaded()) return;

		if(modelFileUrl.isEmpty()) {
			taskManager.logError("Trained model file / URL is missing or unavailable");
		}
		modelLoader.run(modelName, network, modelFileUrl, getInput());

		try {
			inputValidator.run(getInput(), network);
		}
		catch(IncompatibleTypeException e) {
			taskManager.logError(e.getMessage());
			return;
		}
		inputMapper.run(getInput(), network);
		networkAndInputCompatible = !inputMapper.isFailed();
	}

	private void savePreferences() {
		if(modelFile != null) {
			prefService.put(this.getClass(), modelFileKey, modelFile.getAbsolutePath());
		}
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

	protected List<AdvancedTiledView<FloatType>> tryToTileAndRunNetwork(
		final List<RandomAccessibleInterval> normalizedInput)
			throws OutOfMemoryError, ExecutionException {
		List<AdvancedTiledView<FloatType>> tiledOutput = null;

		boolean isOutOfMemory = true;
		boolean canHandleOutOfMemory = true;

		while (isOutOfMemory && canHandleOutOfMemory) {
			try {
				tiledOutput = tileAndRunNetwork(normalizedInput);
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

	protected List tileAndRunNetwork(List<RandomAccessibleInterval> input) throws ExecutionException {
		AxisType[] finalInputAxes = network.getInputNode().getFinalAxesArray();
		Tiling.TilingAction[] tilingActions = network.getInputNode().getTilingActions();
		final List<AdvancedTiledView> tiledInput;
		if(network.getInputNode().getTilingAllowed()) {
			tiledInput = inputTiler.run(
					input, finalInputAxes, tiling, tilingActions);
			nTiles = tiling.getTilesNum();
		} else {
			tiledInput = input.stream().map(image -> getSingleTileView(image, finalInputAxes)).collect(Collectors.toList());
		}
		if(tiledInput == null) return null;
		return modelExecutor.run(tiledInput, network);
	}

	private AdvancedTiledView getSingleTileView(RandomAccessibleInterval image, AxisType[] finalInputAxes) {
		long[] blockSize = new long[image.numDimensions()];
		long[] overlap = new long[image.numDimensions()];
		for (int i = 0; i < blockSize.length; i++) {
			blockSize[i] = image.dimension(i);
			overlap[i] = 0;
		}
		return new AdvancedTiledView(image, blockSize, overlap, finalInputAxes);
	}

	public void setMapping(final AxisType[] mapping) {
		inputMapper.setMapping(mapping);
	}

	public AxisType[] getMapping() {
		return inputMapper.getMapping();
	}

	private boolean noInputData() {
		boolean noInput = getInput() == null;
		if(noInput) {
			if(isHeadless()) {
				log("Please open an image first");
			}else {
				showError("Please open an image first");
			}
		}
		return noInput;
	}

	private boolean tryHandleOutOfMemoryError() {
		// We expect it to be an out of memory exception and
		// try it again with more tiles or smaller batches.
		final Task modelExecutorTask = modelExecutor;
		nTiles = tiling.getTilesNum();
		if(oldNTiles == nTiles && oldBatchesSize == batchSize) {
			modelExecutorTask.setFailed();
			return false;
		}
		oldNTiles = nTiles;
		oldBatchesSize = batchSize;

		handleOutOfMemoryError();
		initTiling();
		nTiles = tiling.getTilesNum();
		modelExecutorTask.logWarning(
			"Out of memory exception occurred. Trying with " + nTiles +
				" tiles, batch size " + batchSize + " and overlap " + overlap + "...");

		modelExecutorTask.startNewIteration();
		inputTiler.addIteration();
		return true;
	}

	protected void handleOutOfMemoryError() {
		batchSize /= 2;
		if (batchSize < 1) {
			batchSize = 1;
			nTiles *= 2;
		}
	}

	protected static void showError(final String errorMsg) {
		JOptionPane.showMessageDialog(null, errorMsg, "Error",
			JOptionPane.ERROR_MESSAGE);
	}

	public Dataset getInput() {
		return input;
	}

	@Override
	public String getCancelReason() {
		return null;
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void cancel(final String reason) {
		canceled = true;
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

	protected void error(final String msg) {
		if (taskManager != null) {
			taskManager.logError(msg);
		}
		else {
			System.out.println("[ERROR] " + msg);
		}
	}

	protected boolean isHeadless() {
		return uiService.isHeadless();
	}

}

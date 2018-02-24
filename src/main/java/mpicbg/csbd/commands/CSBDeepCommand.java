/*-
 * #%L
 * CSBDeep Fiji Plugin: Use deep neural networks for image restoration for fluorescence microscopy.
 * %%
 * Copyright (C) 2017 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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

import javax.swing.JOptionPane;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

import org.scijava.Cancelable;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.network.ImageTensor;
import mpicbg.csbd.network.Network;
import mpicbg.csbd.network.tensorflow.TensorFlowNetwork;
import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.tasks.DefaultInputMapper;
import mpicbg.csbd.tasks.DefaultInputNormalizer;
import mpicbg.csbd.tasks.DefaultInputProcessor;
import mpicbg.csbd.tasks.DefaultInputTiler;
import mpicbg.csbd.tasks.DefaultModelExecutor;
import mpicbg.csbd.tasks.DefaultModelLoader;
import mpicbg.csbd.tasks.DefaultOutputProcessor;
import mpicbg.csbd.tasks.DefaultOutputTiler;
import mpicbg.csbd.tasks.InputMapper;
import mpicbg.csbd.tasks.InputNormalizer;
import mpicbg.csbd.tasks.InputProcessor;
import mpicbg.csbd.tasks.InputTiler;
import mpicbg.csbd.tasks.ModelExecutor;
import mpicbg.csbd.tasks.ModelLoader;
import mpicbg.csbd.tasks.OutputProcessor;
import mpicbg.csbd.tasks.OutputTiler;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.DefaultTiling;
import mpicbg.csbd.tiling.Tiling;
import mpicbg.csbd.util.DatasetHelper;
import mpicbg.csbd.util.DefaultTaskManager;
import mpicbg.csbd.util.Task;
import mpicbg.csbd.util.TaskManager;

public class CSBDeepCommand< T extends RealType< T > > extends PercentileNormalizer< T >
		implements
		Cancelable,
		Initializable {

	protected static String[] OUTPUT_NAMES = { "result" };

	@Parameter( label = "input data", type = ItemIO.INPUT, initializer = "processDataset" )
	public DatasetView datasetView;

	@Parameter
	protected LogService log;

	@Parameter( label = "Number of tiles", min = "1" )
	public int nTiles = 8;

	@Parameter( label = "Overlap between tiles", min = "0", stepSize = "16" )
	public int overlap = 32;

	@Parameter( type = ItemIO.BOTH, label = "result" )
	protected List< DatasetView > resultDatasets = new ArrayList<>();

	protected TiledView< FloatType > tiledView;
	List< RandomAccessibleInterval< FloatType > > rawresults = null;

	public String modelFileUrl;
	public String modelName;
	public String inputNodeName = "input";
	public String outputNodeName = "output";
	public int blockMultiple = 32;

	protected boolean processedDataset = false;

	TaskManager taskManager;

	protected Network network;
	protected Tiling tiling;

	InputProcessor inputProcessor;
	InputMapper inputMapper;
	InputNormalizer inputNormalizer;
	InputTiler inputTiler;
	ModelLoader modelLoader;
	ModelExecutor modelExecutor;
	OutputTiler outputTiler;
	OutputProcessor outputProcessor;

	@Override
	public void initialize() {
		initNetwork();
		initTasks();
	}

	protected void initNetwork() {
		network = new TensorFlowNetwork();
		network.loadLibrary();
	}

	private void initTasks() {
		inputMapper = initInputMapper();
		inputProcessor = initInputProcessor();
		inputNormalizer = initInputNormalizer();
		inputTiler = initInputTiler();
		modelLoader = initModelLoader();
		modelExecutor = initModelExecutor();
		outputTiler = initOutputTiler();
		outputProcessor = initOutputProcessor();

		taskManager = new DefaultTaskManager();
		taskManager.initialize();
//		taskManager.createTaskForce("Preprocessing", modelLoader, inputMapper, inputProcessor, inputNormalizer);
//		taskManager.createTaskForce("Tiling", inputTiler);
//		taskManager.createTaskForce("Execution", modelExecutor);
//		taskManager.createTaskForce("Postprocessing", outputTiler, outputProcessor);
		taskManager.add( ( Task ) modelLoader );
		taskManager.add( ( Task ) inputMapper );
		taskManager.add( ( Task ) inputProcessor );
		taskManager.add( ( Task ) inputNormalizer );
		taskManager.add( ( Task ) inputTiler );
		taskManager.add( ( Task ) modelExecutor );
		taskManager.add( ( Task ) outputTiler );
		taskManager.add( ( Task ) outputProcessor );
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

		if ( noInputData() )
			return;

		modelLoader.run(
				modelName,
				network,
				modelFileUrl,
				inputNodeName,
				outputNodeName,
				datasetView );
		inputMapper.run( getInput(), network );
		final List< RandomAccessibleInterval< FloatType > > processedInput =
				inputProcessor.run( getInput() );
		final List< RandomAccessibleInterval< FloatType > > normalizedInput =
				inputNormalizer.run( processedInput );
		initTiling();
		final List< AdvancedTiledView< FloatType > > tiledOutput =
				tryToTileAndRunNetwork( normalizedInput );
		final List< RandomAccessibleInterval< FloatType > > output =
				outputTiler.run( tiledOutput, tiling, getAxesArray( network.getOutputNode() ) );
		resultDatasets.clear();
		resultDatasets.addAll( outputProcessor.run( output, datasetView, network ) );
	}

	private AxisType[] getAxesArray( final ImageTensor outputNode ) {
		final AxisType[] res = new AxisType[ outputNode.numDimensions() + 1 ];
		for ( int i = 0; i < outputNode.numDimensions(); i++ ) {
			res[ i ] = outputNode.getAxisByDatasetDim( i );
		}
		res[ res.length - 1 ] = Axes.CHANNEL;
		return res;
	}

	protected void initTiling() {
		tiling = new DefaultTiling( nTiles, blockMultiple, overlap );
	}

	private List< AdvancedTiledView< FloatType > > tryToTileAndRunNetwork(
			final List< RandomAccessibleInterval< FloatType > > normalizedInput ) {
		List< AdvancedTiledView< FloatType > > tiledOutput = null;
		boolean isOutOfMemory = true;
		boolean canHandleOutOfMemory = true;
		while ( isOutOfMemory && canHandleOutOfMemory ) {
			try {
				final List< AdvancedTiledView< FloatType > > tiledInput =
						inputTiler.run( normalizedInput, getInput(), tiling );
				tiledOutput = modelExecutor.run( tiledInput, network );
				isOutOfMemory = false;
			} catch ( final OutOfMemoryError e ) {
				isOutOfMemory = true;
				canHandleOutOfMemory = tryHandleOutOfMemoryError();
			}
		}
		return tiledOutput;
	}

	public void setMapping( final AxisType[] mapping ) {
		inputMapper.setMapping( mapping );
	}

	private boolean noInputData() {
		return getInput() == null;
	}

	private boolean tryHandleOutOfMemoryError() {
		// We expect it to be an out of memory exception and
		// try it again with more tiles.
		final Task modelExecutorTask = ( Task ) modelExecutor;
		if ( !handleOutOfMemoryError() ) {
			modelExecutorTask.setFailed();
			return false;
		}
		modelExecutorTask.logError(
				"Out of memory exception occurred. Trying with " + nTiles + " tiles..." );
		modelExecutorTask.startNewIteration();
		( ( Task ) inputTiler ).addIteration();
		return true;
	}

	protected boolean handleOutOfMemoryError() {
		nTiles *= 2;
		// Check if the number of tiles is too large already
		if ( Arrays.stream(
				Intervals.dimensionsAsLongArray(
						getInput() ) ).max().getAsLong() / nTiles < blockMultiple ) { return false; }
		return true;
	}

	protected static void showError( final String errorMsg ) {
		JOptionPane.showMessageDialog(
				null,
				errorMsg,
				"Error",
				JOptionPane.ERROR_MESSAGE );
	}

	public Dataset getInput() {
		return datasetView.getData();
	}

	public void validateInput(
			final Dataset dataset,
			final String formatDesc,
			final OptionalLong... expectedDims ) throws IOException {
		DatasetHelper.validate( dataset, formatDesc, expectedDims );
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
	public void cancel( final String reason ) {
		modelExecutor.cancel( reason );
	}

	protected static void
			printDim( final String title, final RandomAccessibleInterval< FloatType > img ) {
		final long[] dims = new long[ img.numDimensions() ];
		img.dimensions( dims );
		System.out.println( title + ": " + Arrays.toString( dims ) );
	}

}

/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2020 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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
// package mpicbg.csbd.tasks;
//
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
//
// import net.imagej.Dataset;
// import net.imagej.ImageJ;
// import net.imagej.axis.Axes;
// import net.imagej.axis.AxisType;
// import net.imagej.display.DatasetView;
// import net.imglib2.RandomAccessibleInterval;
// import net.imglib2.type.numeric.real.FloatType;
// import net.imglib2.util.Intervals;
//
// import org.junit.Test;
//
// import CSBDeepTest;
// import mpicbg.csbd.network.Network;
// import mpicbg.csbd.prediction.AdvancedTiledView;
// import mpicbg.csbd.prediction.DefaultTiling;
// import mpicbg.csbd.prediction.Tiling;
// import mpicbg.csbd.util.Task;
//
// public class AllTasksTest extends CSBDeepTest {
//
// Network network = new PseudoNetwork();
//
// InputProcessor inputProcessor = new DefaultInputProcessor();
// InputMapper inputMapper = new DefaultInputMapper();
// InputNormalizer inputNormalizer = new DefaultInputNormalizer();
// InputTiler inputTiler = new DefaultInputTiler();
// ModelLoader modelLoader = new DefaultModelLoader();
// ModelExecutor modelExecutor = new DefaultModelExecutor();
// OutputTiler outputTiler = new DefaultOutputTiler();
// OutputProcessor outputProcessor = new DefaultOutputProcessor();
//
// Tiling tiling;
//
// String modelName = "";
// String modelFileUrl = "";
// String inputNodeName = "";
// String outputNodeName = "";
//
// int nTiles = 8;
// int blockMultiple = 32;
// int overlap = 32;
//
// Dataset dataset = null;
// List<DatasetView> output = new ArrayList<>();
//
// @Test
// public void run() {
//
// long[] datasetSize = {10, 50, 100};
// AxisType[] axes = {Axes.Z, Axes.X, Axes.Y};
//
// ImageJ ij = new ImageJ();
//
// dataset = ij.dataset().create(new FloatType(), datasetSize, "", axes);
// DatasetView datasetView = wrapInDatasetView( dataset );
//
// network.getInputNode().setNodeShape( datasetSize );
// network.getOutputNode().setNodeShape( datasetSize );
// modelLoader.run(modelName, network, modelFileUrl, inputNodeName,
// outputNodeName, datasetView);
// inputMapper.run(dataset, network);
// List< RandomAccessibleInterval< FloatType > > processedInput =
// inputProcessor.run(dataset);
// List< RandomAccessibleInterval< FloatType > > normalizedInput =
// inputNormalizer.run( processedInput );
// tiling = new DefaultTiling( nTiles, blockMultiple, overlap );
// List< AdvancedTiledView< FloatType > > tiledOutput = tryToTileAndRunNetwork(
// normalizedInput );
// List< RandomAccessibleInterval< FloatType > > output = outputTiler.run(
// tiledOutput, tiling, axes );
// output.clear();
// output.addAll( outputProcessor.run(output, datasetView, network) );
// }
//
// private List< AdvancedTiledView< FloatType > > tryToTileAndRunNetwork(List<
// RandomAccessibleInterval< FloatType > > normalizedInput) {
// List< AdvancedTiledView< FloatType > > tiledOutput = null;
// boolean isOutOfMemory = true;
// boolean canHandleOutOfMemory = true;
// while(isOutOfMemory && canHandleOutOfMemory) {
// try {
// List<AdvancedTiledView<FloatType>> tiledInput = inputTiler.run(
// normalizedInput, dataset, tiling );
// tiledOutput = modelExecutor.run( tiledInput, network );
// isOutOfMemory = false;
// } catch(OutOfMemoryError e) {
// isOutOfMemory = true;
// canHandleOutOfMemory = tryHandleOutOfMemoryError();
// }
// }
// return tiledOutput;
// }
//
// private boolean tryHandleOutOfMemoryError() {
// // We expect it to be an out of memory exception and
// // try it again with more tiles.
// Task modelExecutorTask = (Task)modelExecutor;
// if(!handleOutOfMemoryError()) {
// modelExecutorTask.setFailed();
// return false;
// }
// modelExecutorTask.logError( "Out of memory exception occurred. Trying with "
// + nTiles + " tiles..." );
// modelExecutorTask.startNewIteration();
// ((Task)inputTiler).addIteration();
// return true;
// }
//
// protected boolean handleOutOfMemoryError() {
// nTiles *= 2;
// // Check if the number of tiles is too large already
// if ( Arrays.stream( Intervals.dimensionsAsLongArray( dataset )
// ).max().getAsLong() / nTiles < blockMultiple ) {
// return false;
// }
// return true;
// }
//
// }

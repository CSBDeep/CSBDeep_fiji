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

package de.csbdresden.csbdeep.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import de.csbdresden.csbdeep.imglib2.TiledView;
import de.csbdresden.csbdeep.io.DatasetOutputProcessor;
import de.csbdresden.csbdeep.io.DefaultInputProcessor;
import de.csbdresden.csbdeep.io.InputProcessor;
import de.csbdresden.csbdeep.io.OutputProcessor;
import de.csbdresden.csbdeep.network.model.ImageTensor;
import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.tiling.DefaultTiling;
import de.csbdresden.csbdeep.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

@Plugin(type = Command.class,
	menuPath = "Plugins>CSBDeep>Run your IsoNet",
	headless = true)
public class GenericIsotropicNetwork<T extends RealType<T>> extends GenericNetwork implements
	Command
{

	@Parameter(label = "Scale factor of Z-Axis", min = "1")
	protected float scale = 10.2f;

	final ExecutorService pool = Executors.newWorkStealingPool();

	@Override
	protected void initTiling() {
		int batchMultiple = 4;
		batchSize = (int) Math.ceil((float) batchSize / (float) batchMultiple) *
			batchMultiple;
		tiling = new DefaultTiling(nTiles, batchSize, blockMultiple, overlap);
	}

	@Override
	protected InputProcessor initInputProcessor() {
		return new IsoInputProcessor();
	}

	@Override
	protected OutputProcessor initOutputProcessor() {
		return new IsoOutputProcessor(datasetService, input);
	}

	private class IsoInputProcessor extends DefaultInputProcessor
	{

		@Override
		public List<RandomAccessibleInterval<FloatType>> run(final Dataset input, Network network) {

			setStarted();

			RandomAccessibleInterval<FloatType> inputRai = Converters.convert(
				(RandomAccessibleInterval) input.getImgPlus(),
				new RealFloatConverter<T>(), new FloatType());

			List droppedDims = network.dropSingletonDims();

			inputRai = dropSingletonDimensions(inputRai, droppedDims);

			// Add dimensions until it fits the input tensor
			while (inputRai.numDimensions() < network.getInputNode().getNodeShape().length) {
				inputRai = Views.addDimension(inputRai, 0, 0);
			}

			log("Dataset type: " + input.getTypeLabelLong());
			DatasetHelper.logDim(this, "Dataset dimensions", inputRai);

			final int dimX = input.dimensionIndex(Axes.X);
			final int dimY = input.dimensionIndex(Axes.Y);
			final int dimZ = input.dimensionIndex(Axes.Z);

			final RandomAccessibleInterval<FloatType> upsampled = upsample(inputRai,
				dimZ, scale);

			DatasetHelper.logDim(this, "Upsampled", upsampled);

			final RandomAccessibleInterval<FloatType> rotated0 = Views.permute(
				upsampled, dimX, dimZ);

			final RandomAccessibleInterval<FloatType> rotated1 = Views.permute(
				rotated0, dimY, dimZ);

			final List<RandomAccessibleInterval<FloatType>> output =
				new ArrayList<>();

			output.add(rotated0);
			output.add(rotated1);

			DatasetHelper.logDim(this, "Input #1 (Z-X rotated)", rotated0);
			DatasetHelper.logDim(this, "Input #2 (Z-X and Z-Y rotated)", rotated1);

			setFinished();

			return output;

		}

	}

	@Override
	public void dispose() {
		super.dispose();
		pool.shutdown();
	}

	private class IsoOutputProcessor<T extends RealType<T> & NativeType<T>>
		extends DatasetOutputProcessor<T>
	{

		private final Dataset dataset;

		public IsoOutputProcessor(DatasetService datasetService, Dataset input) {
			super(datasetService);
			this.dataset = input;
		}

		@Override
		public Dataset run(final List<RandomAccessibleInterval<T>> result, final ImageTensor node)
		{
			setStarted();

			final RandomAccessibleInterval<T> _result0 = result.get(0);
			final RandomAccessibleInterval<T> _result1 = result.get(1);

			DatasetHelper.logDim(this, "_result0", _result0);
			DatasetHelper.logDim(this, "_result1", _result1);

			DatasetHelper.logDim(this, "result0.get(0)", _result0);
			DatasetHelper.logDim(this, "result1.get(0)", _result1);

			// prediction for ZY rotation
			RandomAccessibleInterval<T> res0_pred = _result0;

			// prediction for ZX rotation
			RandomAccessibleInterval<T> res1_pred = _result1;

			DatasetHelper.logDim(this, "Output #1", res0_pred);
			DatasetHelper.logDim(this, "Output #2", res1_pred);

			final int dimX = dataset.dimensionIndex(Axes.X);
			final int dimY = dataset.dimensionIndex(Axes.Y);
			final int dimZ = dataset.dimensionIndex(Axes.Z);

			// rotate output stacks back
			res0_pred = Views.permute(res0_pred, dimX, dimZ);
			res1_pred = Views.permute(res1_pred, dimY, dimZ);
			res1_pred = Views.permute(res1_pred, dimX, dimZ);

			DatasetHelper.logDim(this, "Output #1 (original rotation)", res0_pred);
			DatasetHelper.logDim(this, "Output #2 (original rotation)", res1_pred);

			log("Merge output stacks..");

			// Calculate the geometric mean of the two predictions
			// TODO check if this is right
			final RandomAccessibleInterval<T> prediction = new CellImgFactory<>(
				res0_pred.randomAccess().get()).create(Intervals
					.dimensionsAsLongArray(res0_pred));
			pointwiseGeometricMean(res0_pred, res1_pred, prediction);
			DatasetHelper.logDim(this, "Merged output", prediction);

			Dataset output = wrapIntoDataset(OUTPUT_NAMES[0], prediction, node.getAxesArray(),
				datasetService);

			setFinished();

			return output;
		}
	}

	/**
	 * Scales the given dimension by the given scale and uses linear interpolation
	 * for the missing values. NOTE: This method will return very fast because the
	 * scaling is not applied in this method. The scaling is only applied on
	 * access of pixel values. NOTE: The resulting dimension length will not match
	 * old_dimension_length * scale. But the min and max of the image are mapped
	 * to the scaled positions and used to define the new interval.
	 *
	 * @param input Input image
	 * @param dim Dimension number to scale
	 * @param scale Scale of the dimension
	 * @return The scaled image
	 */
	private <U extends NumericType<U>> RandomAccessibleInterval<U> upsample(
		final RandomAccessibleInterval<U> input, final int dim,
		final float scale)
	{
		final int n = input.numDimensions();

		// Interpolate
		final RealRandomAccessible<U> interpolated = Views.interpolate(Views
			.extendBorder(input), new NLinearInterpolatorFactory<>());

		// Affine transformation to scale the Z axis
		final double[] scales = IntStream.range(0, n).mapToDouble(i -> i == dim
			? scale : 1).toArray();
		final AffineGet scaling = new Scale(scales);

		// Scale min and max to create an interval afterwards
		final double[] targetMin = new double[n];
		final double[] targetMax = new double[n];
		scaling.apply(Intervals.minAsDoubleArray(input), targetMin);
		scaling.apply(Intervals.maxAsDoubleArray(input), targetMax);

		// Apply the transformation
		final RandomAccessible<U> scaled = RealViews.affine(interpolated, scaling);
		return Views.interval(scaled, Arrays.stream(targetMin).mapToLong(
			d -> (long) Math.ceil(d)).toArray(), Arrays.stream(targetMax).mapToLong(
				d -> (long) Math.floor(d)).toArray());
	}

	private <U extends RealType<U>, V extends RealType<V>, W extends RealType<W>>
		void pointwiseGeometricMean(final RandomAccessibleInterval<U> in1,
			final RandomAccessibleInterval<V> in2,
			final RandomAccessibleInterval<W> out)
	{
		final long[] blockSize = computeBlockSize(in1);

		final TiledView<U> tiledViewIn1 = new TiledView<>(in1, blockSize);
		final TiledView<V> tiledViewIn2 = new TiledView<>(in2, blockSize);
		final TiledView<W> tiledViewOut = new TiledView<>(out, blockSize);

		// final ExecutorService pool = Executors.newWorkStealingPool();
		final List<Future<?>> futures = new ArrayList<>();

		futures.clear();

		LoopBuilder.setImages(tiledViewIn1, tiledViewIn2, tiledViewOut)
			.forEachPixel((in1Tile, in2Tile, outTile) -> {
				final LoopBuilder<LoopBuilder.TriConsumer<U, V, W>> loop = LoopBuilder
					.setImages(in1Tile, in2Tile, outTile);
				futures.add(pool.submit(() -> loop.forEachPixel((i1, i2, o) -> o
					.setReal(Math.sqrt(i1.getRealFloat() * i2.getRealFloat())))));
			});

		for (final Future<?> f : futures) {
			try {
				f.get();
			}
			catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		// pool.shutdown();
	}

	private long[] computeBlockSize(final RandomAccessibleInterval<?> in) {
		final int threads = Runtime.getRuntime().availableProcessors();

		final long[] blockSize = Intervals.dimensionsAsLongArray(in);
		int tmpMax = 0;
		for (int i = 0; i < blockSize.length; i++) {
			tmpMax = blockSize[i] > blockSize[tmpMax] ? i : tmpMax;
		}
		final int max = tmpMax;
		IntStream.range(0, blockSize.length).forEach((i) -> blockSize[i] = i == max
			? (long) Math.ceil(blockSize[i] * 1.0 / threads) : blockSize[i]);
		return blockSize;
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
			ij.command().run(GenericIsotropicNetwork.class, true);
		}
	}
}

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

package de.csbdresden.csbdeep.tiling;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.csbdresden.csbdeep.imglib2.GridView;
import de.csbdresden.csbdeep.task.Task;
import de.csbdresden.csbdeep.util.DatasetHelper;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class DefaultTiling<T extends RealType<T>> implements Tiling<T> {

	protected int tilesNum;
	protected int batchSize;
	protected int blockMultiple;
	protected int overlap;
	protected Task status;
	protected TilingAction[] tilingActions;

	public DefaultTiling(final int tilesNum, final int batchSize,
		final int blockMultiple, final int overlap)
	{

		this.tilesNum = tilesNum;
		this.batchSize = batchSize;
		this.blockMultiple = blockMultiple;
		this.overlap = overlap;

	}

	@Override
	public AdvancedTiledView<T> preprocess(RandomAccessibleInterval<T> input, AxisType[] axes, TilingAction[] tilingActions, Task parent)
	{

		this.status = parent;
		this.tilingActions = tilingActions;

		if (input != null) {

			long[] tiling = new long[input.numDimensions()];
			Arrays.fill(tiling, 1);
			computeTiling(input, tiling, tilingActions);
			tilesNum = (int) arrayProduct(tiling);
			long[] padding = getPadding(tiling);
			computeBatching(input, tiling, tilingActions);
			parent.log("Complete input axes: " + Arrays.toString(axes));
			parent.log("Tiling actions: " + Arrays.toString(tilingActions));
			parent.log("Dividing image into " + arrayProduct(tiling) + " tile(s)..");

			RandomAccessibleInterval<T> expandedInput = expandToFitBatchSize(input,
				tiling);
			expandedInput = expandToFitBlockSize(expandedInput, tiling);
			long[] tileSize = calculateTileSize(expandedInput, tiling);

			parent.log("Size of single image tile: " + Arrays.toString(tileSize));

			final AdvancedTiledView<T> tiledView = createTiledView(expandedInput, tileSize, padding, axes);
			for (int i = 0; i < input.numDimensions(); i++) {
				tiledView.getOriginalDims().put(axes[i], input.dimension(
					i));
			}

			DatasetHelper.logDim(parent, "Final image tiling", tiledView);
			parent.debug("Final tile padding: " + Arrays.toString(padding));

			int steps = 1;
			for (int i = 0; i < tiledView.numDimensions(); i++) {
				steps *= tiledView.dimension(i);
			}

			return tiledView;

		}

		return null;

	}

	private void computeBatching(RandomAccessibleInterval<T> input, long[] tiling,
		TilingAction[] tilingActions)
	{

		for (int i = 0; i < input.numDimensions(); i++) {
			if (tilingActions[i] == TilingAction.TILE_WITHOUT_PADDING) {

				long batchDimSize = input.dimension(i);

				// parent.debug( "batchDimSize: " + batchDimSize );

				long batchesNum = (int) Math.ceil((float) batchDimSize /
					(float) batchSize);

				// If a smaller batch size is sufficient for the same amount of batches,
				// we can use it
				batchSize = (int) Math.ceil((float) batchDimSize / (float) batchesNum);

				tiling[i] = batchesNum;

			}
		}

	}

	public static long arrayProduct(long[] array) {
		long rtn = 1;
		for (long i : array) {
			rtn *= i;
		}
		return rtn;
	}

	protected long[] computeTiling(RandomAccessibleInterval<T> input,
		long[] tiling, TilingAction[] tilingActions)
	{
		int currentTiles = 1;
		for (long tiles : tiling) {
			currentTiles *= tiles;
		}
		if (currentTiles >= tilesNum) {
			return tiling;
		}
		else {
			long[] singleTile = new long[input.numDimensions()];
			int maxDim = -1;
			for (int i = 0; i < singleTile.length; i++) {
				if (tilingActions[i] == TilingAction.TILE_WITH_PADDING) {
					singleTile[i] = getTileSize(input, i, tiling, blockMultiple);
					if (singleTile[i] > blockMultiple && (maxDim < 0 ||
						singleTile[i] > singleTile[maxDim]))
					{
						maxDim = i;
					}
				}
			}
			if (maxDim >= 0) {
				tiling[maxDim] += 1;
				return computeTiling(input, tiling, tilingActions);
			}
			else {
				return tiling;
			}
		}
	}

	private long getTileSize(RandomAccessibleInterval<T> dataset, int dimension, long[] tiling, int tileMultiple) {
		return (long) (Math.ceil(dataset.dimension(dimension) / tiling[dimension] / (double) tileMultiple) * tileMultiple);
	}

	protected long[] getPadding(long[] tiling) {
		long[] padding = new long[tiling.length];
		for (int i = 0; i < padding.length; i++) {
			if (tiling[i] > 1) padding[i] = overlap;
		}
		return padding;
	}

	protected RandomAccessibleInterval<T> expandToFitBlockSize(
		RandomAccessibleInterval<T> dataset, long[] tiling)
	{
		for (int i = 0; i < dataset.numDimensions(); i++) {
			if (tilingActions[i] == TilingAction.TILE_WITH_PADDING) {
				dataset = expandDimToSize(dataset, i, getTileSize(dataset, i, tiling, blockMultiple) * tiling[i]);
			}
		}
		return dataset;
	}

	protected RandomAccessibleInterval<T> expandToFitBatchSize(
		RandomAccessibleInterval<T> dataset, long[] tiling)
	{
		for (int i = 0; i < dataset.numDimensions(); i++) {
			if (tilingActions[i] == TilingAction.TILE_WITHOUT_PADDING) {
				dataset = expandDimToSize(dataset, i, getTileSize(dataset, i, tiling, batchSize) *
					tiling[i]);
			}
		}
		return dataset;
	}

	protected long[] calculateTileSize(RandomAccessibleInterval<T> dataset,
		long[] tiling)
	{
		final long[] tileSize = Intervals.dimensionsAsLongArray(dataset);
		for (int i = 0; i < tileSize.length; i++) {
			tileSize[i] /= tiling[i];
		}
		return tileSize;
	}

	protected AdvancedTiledView<T> createTiledView(RandomAccessibleInterval<T> input, long[] tileSize, long[] padding,
		AxisType[] types)
	{
		return new AdvancedTiledView<>(input, tileSize, padding, types);
	}

	@Override
	public RandomAccessibleInterval<T> postprocess(Task parent,
		final AdvancedTiledView<T> results, AxisType[] axisTypes)
	{

		parent.log("POSTPROCESSING");

		List<RandomAccessibleInterval<T>> resultData = results.getProcessedTiles();

		if (resultData != null && resultData.size() > 0) {

			RandomAccessibleInterval<T> firstResult = resultData.get(0);

			parent.log("Output axes: " + Arrays.toString(axisTypes));

			DatasetHelper.debugDim(parent, "result 0 before padding removement",
				firstResult);

			long[] grid = new long[axisTypes.length];
			Arrays.fill(grid, 1);
			for (int i = 0; i < grid.length; i++) {
				for (int j = 0; j < results.getOriginalAxes().length; j++) {
					if (results.getOriginalAxes()[j].equals(axisTypes[i])) {
						grid[i] = results.numDimensions() > j ? results.dimension(j) : 1;
						break;
					}
				}
			}
			for (int i = 0; i < resultData.size(); i++) {
				resultData.set(i, removePadding(resultData.get(i), results.getOverlapComplete(),
					results.getOriginalAxes(), axisTypes));
			}

			// TODO log padding / test padding
			DatasetHelper.debugDim(parent, "result 0 after padding removement",
				firstResult);

			parent.log("Merging tiles..");

			final RandomAccessibleInterval<T> mergedResult = arrangeAndCombineTiles(
				resultData, grid);

			DatasetHelper.debugDim(parent, "merge", mergedResult);
			parent.log("Crop to original size..");

			RandomAccessibleInterval<T> fittedResult = undoExpansion(mergedResult,
				results.getOriginalDims(), axisTypes);

			parent.log("Output axes: " + Arrays.toString(axisTypes));
			DatasetHelper.debugDim(parent, "fittedResult dimensions", fittedResult);

			return fittedResult;
		}

		parent.setFailed();
		return null;
	}

	@Override
	public int getTilesNum() {
		return tilesNum;
	}

	protected RandomAccessibleInterval<T> removePadding(
		RandomAccessibleInterval<T> result, long[] padding, AxisType[] oldAxes,
		AxisType[] newAxes)
	{

		final long[] negPadding = new long[result.numDimensions()];
		for (int i = 0; i < oldAxes.length; i++) {
			for (int j = 0; j < newAxes.length; j++) {
				if (oldAxes[i] == newAxes[j]) {
					negPadding[j] = -padding[i];
				}
			}
		}
		return Views.zeroMin(Views.expandZero(result, negPadding));

	}

	protected RandomAccessibleInterval<T> arrangeAndCombineTiles(
		List<RandomAccessibleInterval<T>> results, long[] grid)
	{
		status.debug("grid: " + Arrays.toString(grid));
		// Arrange and combine the tiles again
		final RandomAccessibleInterval<T> result = new GridView<>(new ListImg<>(
			results, grid));
		return result;
	}

	protected RandomAccessibleInterval<T> undoExpansion(
		RandomAccessibleInterval<T> result, Map<AxisType, Long> originalDims,
		AxisType[] outputAxes)
	{
		RandomAccessibleInterval<T> fittedResult = null;
		for (int i = 0; i < result.numDimensions(); i++) {
			AxisType axis = outputAxes[i];
			// TODO maybe implement this in a more dynamic way, use tilingActions
			if (axis != Axes.CHANNEL) {
				if(originalDims.get(axis) == null) continue;
				long originalSize = originalDims.get(axis);
				fittedResult = expandDimToSize(fittedResult == null ? result
					: fittedResult, i, originalSize);
			}
		}
		return fittedResult;
	}

	protected RandomAccessibleInterval<T> expandDimToSize(
		final RandomAccessibleInterval<T> im, final int d, final long size)
	{
		final int n = im.numDimensions();
		final long[] min = new long[n];
		final long[] max = new long[n];
		im.min(min);
		im.max(max);
		max[d] += (size - im.dimension(d));
		return Views.interval(Views.extendMirrorDouble(im), new FinalInterval(min,
			max));
	}

}

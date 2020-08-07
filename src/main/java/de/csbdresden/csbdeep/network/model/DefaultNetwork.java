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

package de.csbdresden.csbdeep.network.model;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.scijava.io.location.Location;

import de.csbdresden.csbdeep.imglib2.TiledView;
import de.csbdresden.csbdeep.task.Task;
import de.csbdresden.csbdeep.util.IOHelper;
import net.imagej.Dataset;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public abstract class DefaultNetwork<T extends RealType<T>> implements
		Network<T>
{

	protected Task status;
	protected ImageTensor inputNode = null;
	protected ImageTensor outputNode = null;
	protected TiledView<T> tiledView;
	protected Integer doneTileCount;
	protected boolean dropSingletonDims = false;
	protected NetworkSettings networkSettings;
	ExecutorService pool;

	public DefaultNetwork(Task associatedTask) {
		this.status = associatedTask;
	}

	protected abstract boolean loadModel(Location source, String modelName);

	@Override
	public boolean loadModel(final String pathOrURL, final String modelName)
		throws FileNotFoundException
	{

		final Location source = IOHelper.loadFileOrURL(pathOrURL);
		return loadModel(source, modelName);

	}

	@Override
	public abstract void preprocess();

	@Override
	public List<RandomAccessibleInterval<T>> call()
		throws IllegalArgumentException, ExecutionException, OutOfMemoryError
	{

		pool = Executors.newSingleThreadExecutor();

		final boolean multithreading = false;

		final Cursor<RandomAccessibleInterval<T>> cursor = Views.iterable(tiledView)
			.cursor();

		// Loop over the tiles and execute the prediction
		final List<RandomAccessibleInterval<T>> results = new ArrayList<>();
		final List<Future<RandomAccessibleInterval<T>>> futures = new ArrayList<>();

		while (cursor.hasNext()) {
			final RandomAccessibleInterval<T> tile = cursor.next();

			final Future<RandomAccessibleInterval<T>> future = pool.submit(() -> execute(tile));

			log("Processing tile " + (doneTileCount + 1) + "..");

			futures.add(future);

			if (!multithreading) {
				try {
					final RandomAccessibleInterval<T> res = future.get();
					if (res == null) return null;
					results.add(res);
					upTileCount();
				}
				catch (final IllegalArgumentException exc) {
					pool.shutdownNow();
					fail();
					throw  exc;
				}
				catch (final InterruptedException exc) {
					pool.shutdownNow();
//					fail();
					return null;
				}
			}
		}
		if (multithreading) {
			for (final Future<RandomAccessibleInterval<T>> future : futures) {
				try {
					final RandomAccessibleInterval<T> res = future.get();
					if (res == null) return null;
					results.add(res);
					upTileCount();
				}
				catch (final InterruptedException exc) {
					pool.shutdownNow();
					fail();
					return null;
				}
			}
		}

		return results;
	}

	@Override
	public abstract RandomAccessibleInterval<T> execute(
		RandomAccessibleInterval<T> tile) throws Exception;

	@Override
	public Task getStatus() {
		return status;
	}

	@Override
	public ImageTensor getInputNode() {
		return inputNode;
	}

	@Override
	public ImageTensor getOutputNode() {
		return outputNode;
	}

	@Override
	public void loadInputNode(final Dataset dataset) {
		inputNode = new ImageTensor();
		inputNode.initialize(dataset);
	}

	@Override
	public void loadOutputNode(Dataset dataset) {
		outputNode = new ImageTensor();
		outputNode.initialize(dataset);
	}

	@Override
	public abstract void initMapping();

	@Override
	public abstract List<Integer> dropSingletonDims();

	@Override
	public abstract boolean isInitialized();

	@Override
	public void resetTileCount() {
		doneTileCount = 0;
		status.setCurrentStep(doneTileCount);
	}

	protected void upTileCount() {
		doneTileCount++;
		status.setCurrentStep(doneTileCount);
	}

	@Override
	public void setTiledView(final TiledView<T> tiledView) {
		this.tiledView = tiledView;
	}

	protected void log(final String text) {
		if (status != null) {
			status.log(text);
		}else {
			System.out.println("[INFO] " + text);
		}
	}

	protected void logError(final String text) {
		if (status != null) {
			status.logError(text);
		}else {
			System.out.println("[ERROR] " + text);
		}
	}

	void fail() {
		status.setFailed();
	}

	@Override
	public void cancel(String reason) {
		pool.shutdownNow();
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public String getCancelReason() {
		return null;
	}

	/**
	 * Set if singleton dimensions of the output image should be dropped. If the
	 * tile size in one dimension is only one this could remove an important
	 * dimension. Default value is true.
	 */
	@Override
	public void setDropSingletonDims(final boolean dropSingletonDims) {
		this.dropSingletonDims = dropSingletonDims;
	}

//	@Override
//	public void setDoDimensionReduction(final boolean doDimensionReduction) {
//		setDoDimensionReduction(doDimensionReduction, Axes.Z);
//	}
//
//	@Override
//	public void setDoDimensionReduction(final boolean doDimensionReduction,
//		final AxisType axisToRemove)
//	{
//		this.doDimensionReduction = doDimensionReduction;
//		this.axisToRemove = axisToRemove;
//	}

	@Override
	public void dispose() {
		if (pool != null) {
			pool.shutdown();
		}
		pool = null;
	}

	@Override
	public void clear() {
		inputNode = null;
		outputNode = null;
	}
}

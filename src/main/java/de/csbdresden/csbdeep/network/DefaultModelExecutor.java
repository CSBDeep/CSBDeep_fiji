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

package de.csbdresden.csbdeep.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.DefaultTask;
import de.csbdresden.csbdeep.tiling.AdvancedTiledView;
import de.csbdresden.csbdeep.util.DatasetHelper;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class DefaultModelExecutor<T extends RealType<T>> extends DefaultTask
	implements ModelExecutor<T>
{

	private static String PROGRESS_CANCELED = "Canceled";
	private ExecutorService pool = null;
	private Network network = null;
	private boolean canceled = false;

	@Override
	public List<AdvancedTiledView<T>> run(final List<AdvancedTiledView<T>> input,
		final Network network) throws OutOfMemoryError, ExecutionException {
		if(!isCanceled()) {
			setStarted();
			this.network = network;
			if (input.size() > 0) {
				DatasetHelper.logDim(this, "Network input size", input.get(0)
						.randomAccess().get());
			}

			setCurrentStep(0);
			network.resetTileCount();
			setNumSteps(getSteps(input));

			pool = Executors.newWorkStealingPool();
			final List<AdvancedTiledView<T>> output = new ArrayList<>();
			for (AdvancedTiledView<T> tile : input) {
				try {
					output.add(run(tile, network));
				} catch (ExecutionException e) {
					throw e;
				}
				if(isCanceled()) return null;
			}
			pool.shutdown();
			if(isCanceled()) return null;
			if (output.size() > 0) {
				DatasetHelper.logDim(this, "Network output size", output.get(0)
						.getProcessedTiles().get(0));
			}
			setFinished();
			return output;
		}
		return null;
	}

	private int getSteps(List<AdvancedTiledView<T>> input) {
		int numSteps = 0;
		for (AdvancedTiledView<T> tile : input) {
			int steps = 1;
			for (int i = 0; i < tile.numDimensions(); i++) {
				steps *= tile.dimension(i);
			}
			numSteps += steps;
		}
		return numSteps;
	}

	private AdvancedTiledView<T> run(final AdvancedTiledView<T> input,
		final Network network) throws OutOfMemoryError, IllegalArgumentException, ExecutionException {

		input.getProcessedTiles().clear();

		try {
			network.setTiledView(input);
			Future<List<RandomAccessibleInterval<T>>> resultFuture = pool.submit(network);
			if(resultFuture != null) {
				List<RandomAccessibleInterval<T>> result = resultFuture.get();
				if(result != null) {
					input.getProcessedTiles().addAll(result);
				}
			}

		}
		catch(final CancellationException | RejectedExecutionException | InterruptedException e) {
			//canceled
			setFailed();
			log(PROGRESS_CANCELED);
			cancel(PROGRESS_CANCELED);
			return null;
		}
		catch(final IllegalArgumentException e) {
			setFailed();
			throw e;
		}
		catch (final ExecutionException | IllegalStateException exc) {
			if(exc.getMessage() != null && exc.getMessage().contains("OOM")) {
				setIdle();
				throw new OutOfMemoryError();
			}
			exc.printStackTrace();
			setFailed();
			throw exc;
		}

		return input;
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void cancel(final String reason) {
		canceled = true;
		if (pool != null && !pool.isShutdown()) {
			pool.shutdownNow();
		}
		if(network != null) {
			network.cancel(reason);
		}
	}

	@Override
	public String getCancelReason() {
		return null;
	}

}

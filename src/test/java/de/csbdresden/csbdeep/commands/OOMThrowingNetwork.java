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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import de.csbdresden.csbdeep.network.ModelExecutor;
import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.DefaultTask;
import de.csbdresden.csbdeep.tiling.AdvancedTiledView;
import de.csbdresden.csbdeep.tiling.Tiling;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;

@Plugin(type = Command.class)
public class OOMThrowingNetwork extends GenericNetwork
{

	@Parameter
	Tiling.TilingAction[] actions;

	@Parameter(type = ItemIO.OUTPUT)
	List nTilesHistory = new ArrayList();

	@Parameter(type = ItemIO.OUTPUT)
	List batchSizeHistory = new ArrayList();

	private class OOMThrowingModelExecutor extends DefaultTask implements ModelExecutor {
		@Override
		public List<AdvancedTiledView> run(List input, Network network) {
			throw new OutOfMemoryError();
		}
	}

	@Override
	public void initialize() {
	}

	@Override
	protected ModelExecutor initModelExecutor() {
		return new OOMThrowingModelExecutor();
	}

	@Override
	public void run() throws OutOfMemoryError {

		nTilesHistory.clear();
		batchSizeHistory.clear();

		inputTiler = initInputTiler();
		modelExecutor = initModelExecutor();
		initTiling();

		List list = new ArrayList();
		list.add(getInput().getImgPlus());

		try {
			tryToTileAndRunNetwork(list);
		}
		catch(OutOfMemoryError e) {
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected List tileAndRunNetwork(List<RandomAccessibleInterval> input) throws ExecutionException {
		AxisType[] finalInputAxes =  new AxisType[getInput().numDimensions()];
		Tiling.TilingAction[] tilingActions = actions;
		dummyTiling(finalInputAxes);
		final List<AdvancedTiledView> tiledInput = inputTiler.run(
				input, finalInputAxes, tiling, tilingActions);
		nTiles = tiling.getTilesNum();
		if(tiledInput == null) return null;
		return modelExecutor.run(tiledInput, network);
	}


	public void dummyTiling(final AxisType[] finalInputAxes) {
		for (int i = 0; i < input.numDimensions(); i++) {
			finalInputAxes[i] = input.axis(i).type();
		}
	}

	@Override
	protected void handleOutOfMemoryError() {
		nTilesHistory.add(nTiles);
		batchSizeHistory.add(batchSize);
		super.handleOutOfMemoryError();
	}


}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import de.csbdresden.csbdeep.tiling.Tiling;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.real.FloatType;

public class OOMThrowingTest {

	@Test
	public void testHandlingOfOOMs() {
		ImageJ ij = new ImageJ();
		final Dataset input =  ij.dataset().create(new FloatType(), new long[]{10,20,30}, "", new AxisType[]{Axes.X, Axes.Y, Axes.Z});
		final Future<CommandModule> future = ij.command().run(OOMThrowingNetwork.class, false,
				"input", input,
				"nTiles", 1,
				"overlap", 0,
				"blockMultiple", 10,
				"actions", new Tiling.TilingAction[]{Tiling.TilingAction.TILE_WITH_PADDING, Tiling.TilingAction.TILE_WITH_PADDING, Tiling.TilingAction.TILE_WITH_PADDING});
		assertNotEquals(null, future);
		final Module module = ij.module().waitFor(future);
		List nTilesHistory = (List) module.getOutput("nTilesHistory");
		List batchSizeHistory = (List) module.getOutput("batchSizeHistory");
		assertEquals(nTilesHistory.size(), batchSizeHistory.size());
		assertEquals(4, nTilesHistory.size());
		assertEquals(1, nTilesHistory.get(0));
		assertEquals(2, nTilesHistory.get(1));
		assertEquals(4, nTilesHistory.get(2));
		assertEquals(6, nTilesHistory.get(3));
		assertEquals(1, batchSizeHistory.get(0));
		assertEquals(1, batchSizeHistory.get(1));
		assertEquals(1, batchSizeHistory.get(2));
		assertEquals(1, batchSizeHistory.get(3));
	}

	@Test
	public void testHandlingOfOOMsSmallDataset() {
		ImageJ ij = new ImageJ();
		final Dataset input =  ij.dataset().create(new FloatType(), new long[]{3,4,5}, "", new AxisType[]{Axes.X, Axes.Y, Axes.TIME});
		final Future<CommandModule> future = ij.command().run(OOMThrowingNetwork.class, false,
				"input", input,
				"nTiles", 1,
				"overlap", 0,
				"blockMultiple", 10,
				"actions", new Tiling.TilingAction[]{Tiling.TilingAction.TILE_WITH_PADDING, Tiling.TilingAction.TILE_WITH_PADDING, Tiling.TilingAction.TILE_WITH_PADDING});
		assertNotEquals(null, future);
		final Module module = ij.module().waitFor(future);
		List nTilesHistory = (List) module.getOutput("nTilesHistory");
		List batchSizeHistory = (List) module.getOutput("batchSizeHistory");
		assertEquals(nTilesHistory.size(), batchSizeHistory.size());
		assertEquals(1, nTilesHistory.size());
		assertEquals(1, nTilesHistory.get(0));
		assertEquals(1, batchSizeHistory.get(0));
	}

}

package mpicbg.csbd.commands;

import mpicbg.csbd.tiling.Tiling;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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

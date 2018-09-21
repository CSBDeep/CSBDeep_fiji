
package mpicbg.csbd.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import mpicbg.csbd.CSBDeepTest;
import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.task.Task;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.DefaultTiling;
import mpicbg.csbd.tiling.Tiling;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class TilingTest extends CSBDeepTest {

	@Test
	public void testTilingZXY() {

		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
		final long[] datasetSize = { 10, 50, 100 };
		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
		final Task task = new DefaultTask();

		launchImageJ();

		final Dataset dataset = ij.dataset().create(new FloatType(), datasetSize,
			"", axes);
		final RandomAccessibleInterval<FloatType> input =
			(RandomAccessibleInterval<FloatType>) dataset.getImgPlus();
		final AdvancedTiledView<FloatType> tiledView = tiling.preprocess(input,
				axes, getTilingActions(dataset), task);

		tiledView.getProcessedTiles().clear();
		final Cursor<RandomAccessibleInterval<FloatType>> cursor = Views.iterable(
			tiledView).cursor();
		while (cursor.hasNext()) {
			tiledView.getProcessedTiles().add(cursor.next());
		}

		final RandomAccessibleInterval<FloatType> output = tiling.postprocess(task,
			tiledView, axes);

		assertTrue(tiledView != null);
		assertTrue(output != null);

		tiledView.dispose();

		compareDimensions(input, output);
	}

	@Test
	public void testTilingZXYC() {

		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
		final long[] datasetSize = { 10, 50, 100, 1 };
		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y, Axes.CHANNEL };
		final Task task = new DefaultTask();

		launchImageJ();

		final Dataset dataset = ij.dataset().create(new FloatType(), datasetSize,
				"", axes);
		final RandomAccessibleInterval<FloatType> input =
				(RandomAccessibleInterval<FloatType>) dataset.getImgPlus();
		final AdvancedTiledView<FloatType> tiledView = tiling.preprocess(input,
				axes, getTilingActions(dataset), task);

		tiledView.getProcessedTiles().clear();
		final Cursor<RandomAccessibleInterval<FloatType>> cursor = Views.iterable(
				tiledView).cursor();
		while (cursor.hasNext()) {
			tiledView.getProcessedTiles().add(cursor.next());
		}

		final RandomAccessibleInterval<FloatType> output = tiling.postprocess(task,
				tiledView, axes);

		assertTrue(tiledView != null);
		assertTrue(output != null);

		tiledView.dispose();

		compareDimensions(input, output);
	}

	@Test
	public void testNoTiling() {

		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
		final long[] datasetSize = { 10, 50, 100 };
		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
		Arrays.fill(actions, Tiling.TilingAction.NO_TILING);

		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);

		assertEquals(1, getNumTiles(tiledView));

		tiledView.dispose();
	}

	@Test
	public void testSingleDimensionTiling() {

		final Tiling tiling = new DefaultTiling(2, 1, 32, 32);
		final long[] datasetSize = { 10, 50, 100 };
		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
		Arrays.fill(actions, Tiling.TilingAction.TILE_WITH_PADDING);

		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);

		assertEquals(1, tiledView.dimension(0));
		assertEquals(1, tiledView.dimension(1));
		assertEquals(2, tiledView.dimension(2));

		assertEquals(32, tiledView.getBlockSize()[0]);
		assertEquals(64, tiledView.getBlockSize()[1]);
		assertEquals(64, tiledView.getBlockSize()[2]);

		assertEquals(0, tiledView.getOverlap()[0]);
		assertEquals(0, tiledView.getOverlap()[1]);
		assertEquals(32, tiledView.getOverlap()[2]);

		tiledView.dispose();

	}

	@Test
	public void testMultiDimensionTiling() {

		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
		final long[] datasetSize = { 10, 50, 100 };
		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
		Arrays.fill(actions, Tiling.TilingAction.TILE_WITH_PADDING);

		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);

		assertEquals(1, tiledView.dimension(0));
		assertEquals(2, tiledView.dimension(1));
		assertEquals(4, tiledView.dimension(2));

		assertEquals(32, tiledView.getBlockSize()[0]);
		assertEquals(32, tiledView.getBlockSize()[1]);
		assertEquals(32, tiledView.getBlockSize()[2]);

		assertEquals(0, tiledView.getOverlap()[0]);
		assertEquals(32, tiledView.getOverlap()[1]);
		assertEquals(32, tiledView.getOverlap()[2]);

		tiledView.dispose();

	}

	@Test
	public void testNoPaddingTiling() {

		final Tiling tiling = new DefaultTiling(10, 1, 10, 0);
		final long[] datasetSize = { 10, 20, 30 };
		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
		Arrays.fill(actions, Tiling.TilingAction.TILE_WITH_PADDING);

		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);

		assertEquals(1, tiledView.dimension(0));
		assertEquals(2, tiledView.dimension(1));
		assertEquals(3, tiledView.dimension(2));

		assertEquals(10, tiledView.getBlockSize()[0]);
		assertEquals(10, tiledView.getBlockSize()[1]);
		assertEquals(10, tiledView.getBlockSize()[2]);

		assertEquals(0, tiledView.getOverlap()[0]);
		assertEquals(0, tiledView.getOverlap()[1]);
		assertEquals(0, tiledView.getOverlap()[2]);

		tiledView.dispose();

	}


	@Test
	public void testBatching() {

		final Tiling tiling = new DefaultTiling(1, 5, 10, 32);
		final long[] datasetSize = { 10, 50, 100 };
		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
		Arrays.fill(actions, Tiling.TilingAction.TILE_WITHOUT_PADDING);

		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);

		assertEquals(2, tiledView.dimension(0));
		assertEquals(10, tiledView.dimension(1));
		assertEquals(20, tiledView.dimension(2));

		assertEquals(10, tiledView.getBlockSize()[0]);
		assertEquals(10, tiledView.getBlockSize()[1]);
		assertEquals(10, tiledView.getBlockSize()[2]);

		assertEquals(0, tiledView.getOverlap()[0]);
		assertEquals(0, tiledView.getOverlap()[1]);
		assertEquals(0, tiledView.getOverlap()[2]);

		tiledView.dispose();

	}

	@Test
	public void testSmallDataset() {

		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
		final long[] datasetSize = { 3, 4, 5 };
		final AxisType[] axes = { Axes.X, Axes.Y, Axes.TIME };
		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
		Arrays.fill(actions, Tiling.TilingAction.TILE_WITH_PADDING);

		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);

		assertEquals(1, tiledView.dimension(0));
		assertEquals(1, tiledView.dimension(1));
		assertEquals(1, tiledView.dimension(2));

		assertEquals(32, tiledView.getBlockSize()[0]);
		assertEquals(32, tiledView.getBlockSize()[1]);
		assertEquals(32, tiledView.getBlockSize()[2]);

		assertEquals(0, tiledView.getOverlap()[0]);
		assertEquals(0, tiledView.getOverlap()[1]);
		assertEquals(0, tiledView.getOverlap()[2]);

		tiledView.dispose();
	}

	private AdvancedTiledView<FloatType> runTiling(long[] datasetSize, AxisType[] axes, Tiling tiling, Tiling.TilingAction[] actions) {

		launchImageJ();

		final Dataset dataset = ij.dataset().create(new FloatType(), datasetSize,
				"", axes);
		final RandomAccessibleInterval<FloatType> input =
				(RandomAccessibleInterval<FloatType>) dataset.getImgPlus();

		final AdvancedTiledView<FloatType> tiledView = tiling.preprocess(input,
				axes, actions, new DefaultTask());

		return tiledView;

	}

	protected Tiling.TilingAction[] getTilingActions(Dataset input) {
		Tiling.TilingAction[] actions = new Tiling.TilingAction[input
			.numDimensions()];
		Arrays.fill(actions, Tiling.TilingAction.NO_TILING);
		for (int i = 0; i < actions.length; i++) {
			AxisType type = input.axis(i).type();
			if (type.isSpatial()) {
				actions[i] = Tiling.TilingAction.TILE_WITH_PADDING;
			}
		}
		return actions;
	}

	// @Test
	// public void testDatasetWrap() {
	// ImageJ ij = new ImageJ();
	// Dataset dataset = ij.dataset().create(new FloatType(), new long[]{10, 10,
	// 10, 2}, "", new AxisType[]{Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL});
	// final RandomAccessibleInterval< FloatType > input =
	// ( RandomAccessibleInterval< FloatType > ) dataset.getImgPlus();
	// long[] grid = {1,2,1,1};
	// long[] blockSize = {10,5,10,2};
	// long[] overlap = {5,5,5,0};
	// TiledView tv = new TiledView( input, blockSize, overlap );
	//
	// List<RandomAccessibleInterval> results = new ArrayList<>();
	// final Cursor< RandomAccessibleInterval< FloatType > > cursor =
	// Views.iterable( tv ).cursor();
	// while ( cursor.hasNext() ) {
	// results.add( cursor.next() );
	// }
	//
	// final RandomAccessibleInterval< FloatType > result =
	// new GridView<FloatType>( new ListImg<>( results, grid ) );
	//
	//
	// }

}

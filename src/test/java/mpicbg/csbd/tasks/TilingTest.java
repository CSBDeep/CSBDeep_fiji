package mpicbg.csbd.tasks;

import static org.junit.Assert.assertTrue;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.junit.Test;

import mpicbg.csbd.CSBDeepTest;
import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.task.Task;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.DefaultTiling;
import mpicbg.csbd.tiling.Tiling;

public class TilingTest extends CSBDeepTest {

	@Test
	public void testTiling() {

		final Tiling tiling = new DefaultTiling( 8, 32, 32 );
		final long[] datasetSize = { 10, 50, 100 };
		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
		final Task task = new DefaultTask();

		final ImageJ ij = new ImageJ();

		final Dataset dataset = ij.dataset().create( new FloatType(), datasetSize, "", axes );
		final RandomAccessibleInterval< FloatType > input =
				( RandomAccessibleInterval< FloatType > ) dataset.getImgPlus();
		final AdvancedTiledView< FloatType > tiledView = tiling.preprocess( input, dataset, task );

		tiledView.getProcessedTiles().clear();
		final Cursor< RandomAccessibleInterval< FloatType > > cursor =
				Views.iterable( tiledView ).cursor();
		while ( cursor.hasNext() ) {
			tiledView.getProcessedTiles().add( cursor.next() );
		}

		final RandomAccessibleInterval< FloatType > output =
				tiling.postprocess( task, tiledView, axes );

		assertTrue( tiledView != null );
		assertTrue( output != null );

		compareDimensions( input, output );
	}

}

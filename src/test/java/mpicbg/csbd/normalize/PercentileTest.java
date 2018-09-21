
package mpicbg.csbd.normalize;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.junit.Ignore;
import org.junit.Test;

import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.real.FloatType;

public class PercentileTest {

	@Test
	public void testPercentileSortedArray() {

		final long[] dimensions = new long[] { 5, 2 };
		final double[] data = new double[(int) mult(dimensions)];
		for (int i = 0; i < data.length; i++) {
			data[i] = i;
		}

		testPercentiles(dimensions, data, new float[] { 3.0f, 97.0f });

	}

	@Test
	public void testPercentileInvertedArray() {

		final long[] dimensions = new long[] { 5, 2 };
		final double[] data = new double[(int) mult(dimensions)];
		for (int i = 0; i < data.length; i++) {
			data[i] = data.length - i;
		}

		testPercentiles(dimensions, data, new float[] { 1.0f, 99.0f });

	}

	@Test
	public void testMaxPercentilesRandomArray() {

		final long[] dimensions = new long[] { 10, 20, 2 };
		final double[] data = new double[(int) mult(dimensions)];
		Random random = new Random();
		for (int i = 0; i < data.length; i++) {
			data[i] = random.nextDouble();
		}

		testPercentiles(dimensions, data, new float[] { 0.000000001f,
			99.99999999f });

	}

	@Test
	@Ignore
	public void testPercentileInvertedArrayLong() {

		final long[] dimensions = new long[] { 500, 200 };
		final double[] data = new double[(int) mult(dimensions)];
		for (int i = 0; i < data.length; i++) {
			data[i] = (data.length - i) * 0.001;
		}

		testPercentiles(dimensions, data, new float[] { 1.0f, 99.0f });

	}

	@Test
	@Ignore
	public void testPercentileRandomArrayLong() {

		final long[] dimensions = new long[] { 294, 285, 2, 30 };
		final double[] data = new double[(int) mult(dimensions)];
		Random random = new Random();
		for (int i = 0; i < data.length; i++) {
			data[i] = random.nextDouble();
		}

		testPercentiles(dimensions, data, new float[] { 1.0f, 97.f });

	}

	@Test
	@Ignore
	public void testCachedCellImg() {

		System.out.println("Max heap size: " + Runtime.getRuntime().maxMemory());

		long[] dims = new long[] { 2000000 };

		final Img<FloatType> src = new DiskCachedCellImgFactory<>(new FloatType())
			.create(dims);
		final Img<FloatType> dst = new DiskCachedCellImgFactory<>(new FloatType())
			.create(dims);

		Cursor<FloatType> srcCursor = src.cursor();
		Cursor<FloatType> dstCursor = dst.cursor();

		while (srcCursor.hasNext()) {
			srcCursor.fwd();
			dstCursor.fwd();
			dstCursor.get().set(srcCursor.get());
		}
	}

	@Test
	@Ignore
	public void testHistogramNormalization() {

		ImageJ ij = new ImageJ();

		System.out.println("started imagej");

		final Img<FloatType> img = new DiskCachedCellImgFactory<>(new FloatType())
			.create(new long[] { 100000, 100000, 20 });

		System.out.println("created img");

		Histogram1d<FloatType> histogram = ij.op().image().histogram(img, 1000);

		System.out.println("created histogram");
	}

	private long mult(long[] dims) {
		long res = 1;
		for (int i = 0; i < dims.length; i++) {
			res *= dims[i];
		}
		return res;
	}

	private void testPercentiles(long[] dimensions, double[] data,
		float[] percentiles)
	{

		// Get current size of heap in bytes
		System.out.println("Current heap size: " + Runtime.getRuntime()
			.totalMemory());

		// Get maximum size of heap in bytes. The heap cannot grow beyond this
		// size.// Any attempt will result in an OutOfMemoryException.
		System.out.println("Max heap size: " + Runtime.getRuntime().maxMemory());

		// Get amount of free memory within the heap in bytes. This size will
		// increase // after garbage collection and decrease as new objects are
		// created.
		System.out.println("Free heap size: " + Runtime.getRuntime().freeMemory());

		mpicbg.csbd.normalize.Percentile normalizer = new HistogramPercentile<>();
		ImageJ ij = new ImageJ();

		// ij.launch();

		final Img<FloatType> img = new CellImgFactory<>(new FloatType()).create(
			dimensions);

		Cursor<FloatType> cursor = img.cursor();
		int i = -1;
		while (cursor.hasNext()) {
			cursor.fwd();
			i++;
			cursor.get().set((float) data[i]);
		}

		float[] res1 = normalizer.computePercentiles(img, percentiles, ij
			.op());

		Percentile percentile = new Percentile();
		percentile.setData(data);
		for (int j = 0; j < res1.length; j++) {
			double res2 = percentile.evaluate(percentiles[j]);
			assertEquals((float) res2, res1[j], 0.001);
		}

	}

}

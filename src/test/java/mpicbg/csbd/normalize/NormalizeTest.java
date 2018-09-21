
package mpicbg.csbd.normalize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

@Ignore
public class NormalizeTest {

	@Test
	public void testNormalizeSortedArray() {

		final long[] dimensions = new long[] { 11, 1 };
		final double[] data = new double[(int) mult(dimensions)];
		for (int i = 0; i < data.length; i++) {
			data[i] = i;
		}

		testNormalization(dimensions, data, new double[] { 3.0f, 97.0f });

	}

	@Test
	public void testNormalizeInvertedArray() {

		final long[] dimensions = new long[] { 5, 2 };
		final double[] data = new double[(int) mult(dimensions)];
		for (int i = 0; i < data.length; i++) {
			data[i] = data.length - i;
		}

		testNormalization(dimensions, data, new double[] { 1.0f, 99.0f });

	}

	@Test
	public void testNormalizeInvertedArrayLong() {

		final long[] dimensions = new long[] { 50, 200 };
		final double[] data = new double[(int) mult(dimensions)];
		for (int i = 0; i < data.length; i++) {
			data[i] = data.length - i;
		}

		testNormalization(dimensions, data, new double[] { 1.0f, 99.0f });

	}

	@Test
	public void testNormalizeRandomArrayLong() {

		final long[] dimensions = new long[] { 294, 285, 2, 3 };
		final double[] data = new double[(int) mult(dimensions)];
		Random random = new Random();
		for (int i = 0; i < data.length; i++) {
			data[i] = random.nextDouble();
		}

		testNormalization(dimensions, data, new double[] { 1.0f, 97.f });

	}

	@Test
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

	private long mult(long[] dims) {
		long res = 1;
		for (int i = 0; i < dims.length; i++) {
			res *= dims[i];
		}
		return res;
	}

	private void testNormalization(long[] dimensions, double[] data,
		double[] percentiles)
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

		PercentileNormalizer normalizer = new PercentileNormalizer();
		ImageJ ij = new ImageJ();

		ij.launch();

		Dataset dataset = ij.dataset().create(new FloatType(), dimensions,
			"test", null);

		Cursor<FloatType> cursor = (Cursor<FloatType>) dataset
			.getImgPlus().cursor();
		int i = -1;
		while (cursor.hasNext()) {
			cursor.fwd();
			i++;
			cursor.get().set((int) data[i]);
			assertEquals((int) data[i], (int) cursor.get().getRealFloat());
		}

		normalizer.setup(new float[]{3.0f, 99.8f}, new float[]{0,1}, true);
		Dataset res1 = normalizer.normalize(dataset, ij.op(), ij.dataset());

		float[] resValues = normalizer.getResValues();

		assertNotEquals(resValues[0], resValues[1]);

		System.out.println("resValues 0: " + resValues[0]);
		System.out.println("resValues 1: " + resValues[1]);

		IterableInterval<FloatType> res2 = ij.op().image().normalize((IterableInterval<FloatType>) dataset.getImgPlus(), new FloatType(resValues[0]), new FloatType(resValues[1]), new FloatType(0.f), new FloatType(1.f));

		ij.ui().show("input", dataset);
		ij.ui().show("normalized1", res1);
		ij.ui().show("normalized2", res2);

		RandomAccess<FloatType> raOrig = (RandomAccess<FloatType>) dataset.getImgPlus().randomAccess();
		RandomAccess<FloatType> raRes1 = (RandomAccess<FloatType>) res1
			.getImgPlus().randomAccess();
		Cursor<FloatType> cursorRes2 = res2.localizingCursor();
		i = -1;
		while (cursorRes2.hasNext()) {
			cursorRes2.fwd();
			raOrig.setPosition(cursorRes2);
			raRes1.setPosition(cursorRes2);
			i++;
//			System.out.println(data[i] + " " + raRes1.get() + " " + cursorRes2.get());
			assertEquals((int) data[i], (int)raOrig.get().getRealFloat());
			assertFalse(Float.isNaN(raRes1.get().get()));
			assertFalse(Float.isNaN(cursorRes2.get().get()));
//			assertEquals(raRes1.get().getRealFloat(), cursorRes2.get().getRealFloat(), 0.01);
			// assertNotEquals(cursor2.get(), cursor.get());
		}

	}

}

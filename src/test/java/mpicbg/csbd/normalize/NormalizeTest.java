package mpicbg.csbd.normalize;

import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class NormalizeTest {

	@Test
	public void testPercentileSortedArray(){

		final long[] dimensions = new long[] { 5, 2 };
		final double[] data = new double[(int)mult(dimensions)];
		for(int i = 0; i < data.length; i++) {
			data[i] = i;
		}

		testPercentiles(dimensions, data, new double[]{3.0f, 97.0f});

	}


	@Test
	public void testPercentileInvertedArray(){

		final long[] dimensions = new long[] { 5, 2 };
		final double[] data = new double[(int)mult(dimensions)];
		for(int i = 0; i < data.length; i++) {
			data[i] = data.length-i;
		}

		testPercentiles(dimensions, data, new double[]{1.0f, 99.0f});

	}

	@Test
	public void testPercentileInvertedArrayLong(){

		final long[] dimensions = new long[] { 50, 20 };
		final double[] data = new double[(int)mult(dimensions)];
		for(int i = 0; i < data.length; i++) {
			data[i] = data.length-i;
		}

		testPercentiles(dimensions, data, new double[]{1.0f, 99.0f});

	}

	@Test
	public void testPercentileRandomArrayLong(){

		final long[] dimensions = new long[] { 600, 200, 116 };
		final double[] data = new double[(int)mult(dimensions)];
		Random random = new Random();
		for(int i = 0; i < data.length; i++) {
			data[i] = random.nextDouble();
		}

		testPercentiles(dimensions, data, new double[]{1.0f, 97.f});

	}

	@Test
	public void testCachedCellImg(){
		ImageJ ij = new ImageJ();

		final Img< FloatType > img = new DiskCachedCellImgFactory<>(new FloatType())
				.create( new long[]{10000,10000, 20} );

		FloatType el = img.firstElement();

		PercentileNormalizer<FloatType> normalizer = new PercentileNormalizer<>();
		normalizer.setup(new double[]{1.0, 99.0}, new float[]{0,1}, false);
		normalizer.normalize(img, ij);
	}

	private long mult(long[] dims){
		long res = 1;
		for(int i = 0; i < dims.length; i++) {
			res *= dims[i];
		}
		return res;
	}

	private void testPercentiles(long[] dimensions, double[] data, double[] percentiles) {

		// Get current size of heap in bytes
		System.out.println("Current heap size: " + Runtime.getRuntime().totalMemory());

		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
		System.out.println("Max heap size: " + Runtime.getRuntime().maxMemory());

		// Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
		System.out.println("Free heap size: " + Runtime.getRuntime().freeMemory());

		PercentileNormalizer<FloatType> normalizer = new PercentileNormalizer<>();
		ImageJ ij = new ImageJ();

		ij.launch();

		final Img< FloatType > img = new CellImgFactory<>(new FloatType())
				.create( dimensions );

		Cursor<FloatType> cursor = img.cursor();
		int i = -1;
		while(cursor.hasNext()) {
			cursor.fwd();
			i++;
			cursor.get().set((float)data[i]);
		}

		normalizer.setup(percentiles, new float[]{0,1}, false);
		normalizer.normalize(img, ij);
		List<FloatType> res1 = normalizer.getPercentiles();

		Percentile percentile = new Percentile();
		percentile.setData(data);
		for(int j = 0; j < res1.size(); j++) {
			double res2 = percentile.evaluate(percentiles[j]);
			assertEquals((float) res2, res1.get(j).getRealFloat(), 0.001);
		}
	}

}

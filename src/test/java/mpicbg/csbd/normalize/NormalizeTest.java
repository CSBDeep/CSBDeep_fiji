package mpicbg.csbd.normalize;

import net.imagej.ImageJ;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;

public class NormalizeTest {

	@Test
	public void testNormalization(){
		PercentileNormalizer<FloatType> normalizer = new PercentileNormalizer<>();
		ImageJ ij = new ImageJ();

		final long[] dimensions = new long[] { 6000, 200, 113 };

		final Img< FloatType > img = new DiskCachedCellImgFactory<>(new FloatType())
				.create( dimensions );
///		final Img< FloatType > img = new CellImgFactory<>(new FloatType())
//				.create( dimensions );

		FloatType first = img.firstElement();

		normalizer.setup(new float[]{0.3f,0.97f}, new float[]{0,1}, false);
		normalizer.normalize(img, ij);
	}

}

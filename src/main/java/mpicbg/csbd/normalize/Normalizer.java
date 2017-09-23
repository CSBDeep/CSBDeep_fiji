package mpicbg.csbd.normalize;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.ui.UIService;

public interface Normalizer {

	void prepareNormalization( Dataset input );

	void testNormalization( Dataset input, UIService uiService );

	boolean isActive();

	float normalize( float val );
	
	<T extends RealType<T>> Img<FloatType> normalizeImage( RandomAccessibleInterval<T> im);
}

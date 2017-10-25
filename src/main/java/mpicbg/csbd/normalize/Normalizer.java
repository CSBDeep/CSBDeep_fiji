package mpicbg.csbd.normalize;

import net.imagej.Dataset;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.ui.UIService;

public interface Normalizer< T extends RealType< T > > {

	void prepareNormalization( IterableInterval<T> input );

	void testNormalization( Dataset input, UIService uiService );

	boolean isActive();

	float normalize( float val );
	
	Img<FloatType> normalizeImage( RandomAccessibleInterval<T> im);
}

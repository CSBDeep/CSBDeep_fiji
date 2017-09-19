package mpicbg.csbd.normalize;

import net.imagej.Dataset;

import org.scijava.ui.UIService;

public interface Normalizer {

	void prepareNormalization( Dataset input );

	void testNormalization( Dataset input, UIService uiService );

	boolean isActive();

	float normalize( float val );
}

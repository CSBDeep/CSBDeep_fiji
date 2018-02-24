package mpicbg.csbd.tasks;

import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

public interface InputNormalizer {

	public List< RandomAccessibleInterval< FloatType > >
			run( List< RandomAccessibleInterval< FloatType > > input );

}

package mpicbg.csbd.util.task;

import java.util.List;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

public interface InputProcessor {

	public List< RandomAccessibleInterval< FloatType > > run( Dataset input );

}

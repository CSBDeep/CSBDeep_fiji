package mpicbg.csbd.normalize.task;

import java.util.List;

import mpicbg.csbd.task.Task;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

public interface InputNormalizer extends Task {

	public List< RandomAccessibleInterval< FloatType > >
			run( List< RandomAccessibleInterval< FloatType > > input, ImageJ ij );

}

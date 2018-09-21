
package mpicbg.csbd.util.task;

import java.util.List;

import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface InputProcessor<T extends RealType<T>> extends Task {

	List<RandomAccessibleInterval<T>> run(Dataset input, int numDimensions);

}

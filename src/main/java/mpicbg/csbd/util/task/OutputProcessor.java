
package mpicbg.csbd.util.task;

import java.util.List;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface OutputProcessor<T extends RealType<T>> extends Task {

	List<Dataset> run(final List<RandomAccessibleInterval<T>> result,
		final Dataset datasetView, final Network network,
		final DatasetService datasetService);
}

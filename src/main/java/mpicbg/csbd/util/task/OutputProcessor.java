
package mpicbg.csbd.util.task;

import java.util.List;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public interface OutputProcessor<T extends RealType<T>> extends Task {

	List<Dataset> run(final List<RandomAccessibleInterval<T>> result,
		final Dataset datasetView, final AxisType[] axes,
		final DatasetService datasetService);

}

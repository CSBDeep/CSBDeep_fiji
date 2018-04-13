package mpicbg.csbd.util.task;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import java.util.List;

public interface OutputProcessor extends Task {

	public List< Dataset > run(
			final List< RandomAccessibleInterval< FloatType > > result,
			final Dataset datasetView,
			final Network network,
			final DatasetService datasetService );
}

package mpicbg.csbd.util.task;

import java.util.List;

import mpicbg.csbd.task.Task;
import net.imagej.DatasetService;
import net.imagej.display.DatasetView;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.network.Network;

public interface OutputProcessor extends Task {

	public List< DatasetView > run(
			final List< RandomAccessibleInterval< FloatType > > result,
			final DatasetView datasetView,
			final Network network,
			final DatasetService datasetService );
}

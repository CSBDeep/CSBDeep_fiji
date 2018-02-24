package mpicbg.csbd.util.task;

import java.util.List;

import net.imagej.display.DatasetView;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.network.Network;

public interface OutputProcessor {

	public List< DatasetView > run(
			List< RandomAccessibleInterval< FloatType > > result,
			DatasetView datasetView,
			Network network );
}

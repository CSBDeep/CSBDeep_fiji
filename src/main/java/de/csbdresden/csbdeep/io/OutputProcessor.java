
package de.csbdresden.csbdeep.io;

import java.util.List;

import de.csbdresden.csbdeep.task.Task;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface OutputProcessor<T extends RealType<T>> extends Task {

	Dataset run(final List<RandomAccessibleInterval<T>> result,
		final Dataset datasetView, final AxisType[] axes,
		final DatasetService datasetService);

}

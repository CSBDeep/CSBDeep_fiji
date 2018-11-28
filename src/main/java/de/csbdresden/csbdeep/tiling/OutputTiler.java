
package de.csbdresden.csbdeep.tiling;

import java.util.List;

import de.csbdresden.csbdeep.task.Task;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface OutputTiler<T extends RealType<T>> extends Task {

	List<RandomAccessibleInterval<T>> run(List<AdvancedTiledView<T>> input,
		Tiling tiling, AxisType[] axisTypes);

}

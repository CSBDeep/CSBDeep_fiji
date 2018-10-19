
package org.csbdeep.tiling;

import java.util.List;

import org.csbdeep.task.Task;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface InputTiler<T extends RealType<T>> extends Task {

	List<AdvancedTiledView<T>> run(
			List<RandomAccessibleInterval<T>> input, AxisType[] axes,
			Tiling tiling, Tiling.TilingAction[] tilingActions);
}

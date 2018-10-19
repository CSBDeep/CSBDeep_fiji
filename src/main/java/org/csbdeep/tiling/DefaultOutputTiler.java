
package org.csbdeep.tiling;

import java.util.ArrayList;
import java.util.List;

import org.csbdeep.task.DefaultTask;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class DefaultOutputTiler<T extends RealType<T>> extends DefaultTask
	implements OutputTiler<T>
{

	@Override
	public List<RandomAccessibleInterval<T>> run(
		final List<AdvancedTiledView<T>> input, final Tiling tiling,
		final AxisType[] axisTypes)
	{

		setStarted();

		final List<RandomAccessibleInterval<T>> output = new ArrayList<>();

		if(input != null) {
			for (AdvancedTiledView<T> image : input) {
				output.add(tiling.postprocess(this, image, axisTypes));
			}

		}

		setFinished();

		return output;
	}

}


package de.csbdresden.csbdeep.io;

import java.util.List;

import de.csbdresden.csbdeep.network.model.ImageTensor;
import de.csbdresden.csbdeep.task.Task;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface OutputProcessor<T extends RealType<T>, O> extends Task {

	O run(final List<RandomAccessibleInterval<T>> result, final ImageTensor node);

}

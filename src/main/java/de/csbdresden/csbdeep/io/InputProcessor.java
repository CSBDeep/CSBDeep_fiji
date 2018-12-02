
package de.csbdresden.csbdeep.io;

import java.util.List;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.Task;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface InputProcessor<T extends RealType<T>> extends Task {

	List<RandomAccessibleInterval<T>> run(Dataset input, Network network);

}

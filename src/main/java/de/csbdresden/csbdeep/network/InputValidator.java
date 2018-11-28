
package de.csbdresden.csbdeep.network;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.Task;
import net.imagej.Dataset;
import net.imglib2.exception.IncompatibleTypeException;

public interface InputValidator extends Task {

	void run(Dataset input, Network network) throws IncompatibleTypeException;

}

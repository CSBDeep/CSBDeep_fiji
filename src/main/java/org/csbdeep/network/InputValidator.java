
package org.csbdeep.network;

import org.csbdeep.network.model.Network;
import org.csbdeep.task.Task;

import net.imagej.Dataset;
import net.imglib2.exception.IncompatibleTypeException;

public interface InputValidator extends Task {

	void run(Dataset input, Network network) throws IncompatibleTypeException;

}

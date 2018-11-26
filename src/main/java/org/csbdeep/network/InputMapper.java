
package org.csbdeep.network;

import org.csbdeep.network.model.Network;
import org.csbdeep.task.Task;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;

public interface InputMapper extends Task {

	void setMapping(final AxisType[] mapping);

	void run(Dataset input, Network network);

	AxisType[] getMapping();
}

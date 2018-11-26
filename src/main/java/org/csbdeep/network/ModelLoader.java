
package org.csbdeep.network;

import java.io.FileNotFoundException;

import org.csbdeep.network.model.Network;
import org.csbdeep.task.Task;

import net.imagej.Dataset;

public interface ModelLoader extends Task {

	void run(String modelName, Network network, String modelFileUrl, Dataset input) throws FileNotFoundException;

}

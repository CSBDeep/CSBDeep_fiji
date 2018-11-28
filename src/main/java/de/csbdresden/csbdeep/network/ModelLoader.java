
package de.csbdresden.csbdeep.network;

import java.io.FileNotFoundException;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.Task;
import net.imagej.Dataset;

public interface ModelLoader extends Task {

	void run(String modelName, Network network, String modelFileUrl, Dataset input) throws FileNotFoundException;

}

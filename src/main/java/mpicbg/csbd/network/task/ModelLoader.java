
package mpicbg.csbd.network.task;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;

public interface ModelLoader extends Task {

	void run(String modelName, Network network, String modelFileUrl,
		String inputNodeName, String outputNodeName, Dataset input);

}

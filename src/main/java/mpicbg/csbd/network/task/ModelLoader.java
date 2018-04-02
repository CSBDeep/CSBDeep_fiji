package mpicbg.csbd.network.task;

import mpicbg.csbd.task.Task;
import net.imagej.display.DatasetView;

import mpicbg.csbd.network.Network;

public interface ModelLoader extends Task {

	public void run(
			String modelName,
			Network network,
			String modelFileUrl,
			String inputNodeName,
			String outputNodeName,
			DatasetView input );

}

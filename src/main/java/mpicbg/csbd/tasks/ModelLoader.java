package mpicbg.csbd.tasks;

import net.imagej.display.DatasetView;

import mpicbg.csbd.network.Network;

public interface ModelLoader {

	public void run(
			String modelName,
			Network network,
			String modelFileUrl,
			String inputNodeName,
			String outputNodeName,
			DatasetView input );

}

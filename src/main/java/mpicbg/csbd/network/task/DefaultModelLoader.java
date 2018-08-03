
package mpicbg.csbd.network.task;

import java.io.FileNotFoundException;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.DefaultTask;
import net.imagej.Dataset;

public class DefaultModelLoader extends DefaultTask implements ModelLoader {

	@Override
	public void run(final String modelName, final Network network,
		final String modelFileUrl, final String inputNodeName,
		final String outputNodeName, final Dataset input)
	{

		setStarted();

		if (!network.isInitialized()) {
			loadNetwork(modelName, network, modelFileUrl, inputNodeName,
				outputNodeName, input);
			if (!network.isInitialized()) {
				setFailed();
				return;
			}
			network.preprocess();
		}

		setFinished();

	}

	protected void loadNetwork(final String modelName, final Network network,
		final String modelFileUrl, final String inputNodeName,
		final String outputNodeName, final Dataset input)
	{

		try {

			network.loadModel(modelFileUrl, modelName);
			network.loadInputNode(inputNodeName, input);
			network.loadOutputNode(outputNodeName);
			network.initMapping();

		}
		catch (final FileNotFoundException exc1) {

			exc1.printStackTrace();

		}

	}

}

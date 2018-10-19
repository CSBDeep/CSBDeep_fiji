
package org.csbdeep.network;

import java.io.FileNotFoundException;

import org.csbdeep.network.model.Network;
import org.csbdeep.task.DefaultTask;
import net.imagej.Dataset;

public class DefaultModelLoader extends DefaultTask implements ModelLoader {

	@Override
	public void run(final String modelName, final Network network,
		final String modelFileUrl, final Dataset input)
	{

		setStarted();

		if (!network.isInitialized()) {
			loadNetwork(modelName, network, modelFileUrl, input);
			if (!network.isInitialized()) {
				setFailed();
				return;
			}
			network.preprocess();
		}

		setFinished();

	}

	protected void loadNetwork(final String modelName, final Network network,
		final String modelFileUrl, final Dataset input)
	{

		try {

			network.loadModel(modelFileUrl, modelName);
			network.loadInputNode(input);
			network.loadOutputNode(input);
			network.initMapping();

		}
		catch (final FileNotFoundException exc1) {

			exc1.printStackTrace();

		}

	}

}

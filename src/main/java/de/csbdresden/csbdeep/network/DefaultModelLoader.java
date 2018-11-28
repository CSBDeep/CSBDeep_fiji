
package de.csbdresden.csbdeep.network;

import java.io.FileNotFoundException;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.DefaultTask;
import net.imagej.Dataset;

public class DefaultModelLoader extends DefaultTask implements ModelLoader {

	@Override
	public void run(final String modelName, final Network network,
		final String modelFileUrl, final Dataset input) throws FileNotFoundException {

		setStarted();

		if (!network.isInitialized()) {
			try {
				loadNetwork(modelName, network, modelFileUrl, input);
			} catch (FileNotFoundException e) {
				setFailed();
				throw e;
			}
			if (!network.isInitialized()) {
				setFailed();
				return;
			}
			network.preprocess();
		}

		setFinished();

	}

	protected void loadNetwork(final String modelName, final Network network,
		final String modelFileUrl, final Dataset input) throws FileNotFoundException {

		if(modelFileUrl.isEmpty()) return;

		boolean loaded = network.loadModel(modelFileUrl, modelName);
		if(!loaded) return;
		network.loadInputNode(input);
		network.loadOutputNode(input);
		network.initMapping();

	}

}

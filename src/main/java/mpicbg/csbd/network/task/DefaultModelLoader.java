package mpicbg.csbd.network.task;

import java.io.FileNotFoundException;

import net.imagej.display.DatasetView;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.DefaultTask;

public class DefaultModelLoader extends DefaultTask implements ModelLoader {

	@Override
	public void run(
			final String modelName,
			final Network network,
			final String modelFileUrl,
			final String inputNodeName,
			final String outputNodeName,
			final DatasetView input ) {

		setStarted();

		log( "Loading model " + modelName + ".. " );

		if ( !network.isInitialized() ) {
			loadNetwork( modelName, network, modelFileUrl, inputNodeName, outputNodeName, input );
			if ( !network.isInitialized() ) {
				setFailed();
				return;
			}
			network.preprocess();
		}

		setFinished();

	}

	protected void loadNetwork(
			final String modelName,
			final Network network,
			final String modelFileUrl,
			final String inputNodeName,
			final String outputNodeName,
			final DatasetView input ) {

		try {

			loadNetworkFile( network, modelFileUrl, modelName );
			initializeNetwork( network, inputNodeName, input, outputNodeName );

		} catch ( final FileNotFoundException exc1 ) {

			exc1.printStackTrace();

		}

	}

	protected void loadNetworkFile(
			final Network network,
			final String modelFileUrl,
			final String modelName ) throws FileNotFoundException {
		network.loadModel( modelFileUrl, modelName );
	}

	protected void initializeNetwork(
			final Network network,
			final String inputNodeName,
			final DatasetView input,
			final String outputNodeName ) {
		network.loadInputNode( inputNodeName, input.getData() );
		network.loadOutputNode( outputNodeName );
		network.initMapping();
	}

}

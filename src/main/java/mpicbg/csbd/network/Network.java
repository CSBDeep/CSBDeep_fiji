package mpicbg.csbd.network;

import java.io.FileNotFoundException;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.io.location.Location;

import mpicbg.csbd.util.IOHelper;
import mpicbg.csbd.util.Task;

public abstract class Network {
	
	protected Task status;
	protected ImageNode inputNode = new ImageNode();
	protected ImageNode outputNode = new ImageNode();
	protected boolean supportsGPU = false;
	
	public void loadLibrary() {}
	
	public abstract void setInput( Dataset dataset );
	
	protected abstract boolean loadModel( Location source, String modelName );
	
	public boolean loadModel( String pathOrURL, String modelName ) throws FileNotFoundException {

		Location source = IOHelper.loadFileOrURL( pathOrURL );
		return loadModel( source, modelName );
		
	}

	
	public abstract void preprocess();
	
	public abstract RandomAccessibleInterval< FloatType > execute(RandomAccessibleInterval< FloatType > tile, boolean dropSingletonDims) throws Exception;
	
	public Task getStatus() {
		return status;
	}
	
	public ImageNode getInputNode() {
		return inputNode;
	}

	public ImageNode getOutputNode() {
		return outputNode;
	}
	
	public boolean isSupportingGPU() {
		return supportsGPU;
	}

	public void loadInputNode( String defaultName, Dataset dataset ) {
		inputNode.initialize( dataset );
		inputNode.setName( defaultName );
	}

	public void loadOutputNode( String defaultName ) {
		outputNode.setName( defaultName );
	}

	public abstract void initMapping();

	public abstract boolean isInitialized();
	
}

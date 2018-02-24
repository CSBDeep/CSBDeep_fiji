package mpicbg.csbd.tasks;

import java.io.FileNotFoundException;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.io.location.Location;

import mpicbg.csbd.network.DefaultNetwork;

public class PseudoNetwork extends DefaultNetwork {

	private long[] inputShape;
	private boolean initialized = false;

	public PseudoNetwork() {}

	@Override
	public boolean loadModel(
			final String pathOrURL,
			final String modelName ) throws FileNotFoundException {

		return true;

	}

	@Override
	public void loadLibrary() {}

	@Override
	public void loadInputNode( final String defaultName, final Dataset dataset ) {
		inputNode.setName( "input" );
		inputNode.initializeNodeMapping();
	}

	@Override
	public void loadOutputNode( final String defaultName ) {
		outputNode.setName( "output" );
		outputNode.initializeNodeMapping();
	}

	@Override
	protected boolean loadModel( final Location source, final String modelName ) {
		initialized = true;
		return true;
	}

	@Override
	public void preprocess() {
		initMapping();
		calculateMapping();
	}

	@Override
	public void initMapping() {
		inputNode.initMapping();
	}

	protected void calculateMapping() {

		for ( int i = 0; i < inputNode.getNodeShape().length; i++ ) {
			outputNode.setNodeAxis( i, inputNode.getNodeAxis( i ) );
		}
		handleDimensionReduction();
		inputNode.generateMapping();
		outputNode.generateMapping();

		System.out.println( "INPUT NODE: " );
		inputNode.printMapping();
		System.out.println( "OUTPUT NODE: " );
		outputNode.printMapping();
	}

	private void handleDimensionReduction() {
		if ( doDimensionReduction ) {
			getOutputNode().removeAxisFromMapping( inputNode.getDataset(), axisToRemove );
			final Dataset outputDummy =
					createEmptyDuplicateWithoutAxis( inputNode.getDataset(), axisToRemove );
			getOutputNode().initialize( outputDummy );
		} else {
			getOutputNode().initialize( inputNode.getDataset().duplicate() );
		}
	}

	private < T > Dataset
			createEmptyDuplicateWithoutAxis( final Dataset input, final AxisType axisToRemove ) {
		int numDims = input.numDimensions();
		if ( input.axis( Axes.Z ) != null ) {
			numDims--;
		}
		final long[] dims = new long[ numDims ];
		final AxisType[] axes = new AxisType[ numDims ];
		int j = 0;
		for ( int i = 0; i < input.numDimensions(); i++ ) {
			final AxisType axisType = input.axis( i ).type();
			if ( axisType != axisToRemove ) {
				axes[ j ] = axisType;
				dims[ j ] = input.dimension( i );
				j++;
			}
		}
		final Dataset result = new ImageJ().dataset().create( new FloatType(), dims, "", axes );
		return result;
	}

	@Override
	public RandomAccessibleInterval< FloatType >
			execute( final RandomAccessibleInterval< FloatType > tile ) throws Exception {

		return tile;

	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	public long[] getInputShape() {
		return inputShape;
	}

	public void setInputShape( final long[] inputShape ) {
		this.inputShape = inputShape;
	}

}

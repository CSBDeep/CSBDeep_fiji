package mpicbg.csbd.network.tensorflow;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.io.location.Location;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;
import org.tensorflow.framework.TensorShapeProto;

import mpicbg.csbd.network.Network;

public class TensorFlowNetwork extends Network {
	
	private SavedModelBundle model;
	private SignatureDef sig;
	private TensorFlowService tensorFlowService;
	private TensorInfo inputTensorInfo, outputTensorInfo;
	
	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	private static final String MODEL_TAG = "serve";
	protected static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	public TensorFlowNetwork() {
		ImageJ ij = new ImageJ();
		tensorFlowService = ( TensorFlowService ) ij.get( TensorFlowService.class.getName() );
	}

	@Override
	public void loadLibrary() {
		System.out.println( "Loading tensorflow jni from library path..." );
		try {
			System.loadLibrary( "tensorflow_jni" );
			supportsGPU = true;
		} catch ( final UnsatisfiedLinkError e ) {
			supportsGPU = false;
			System.out.println( "Couldn't load tensorflow from library path:" );
			System.out.println( e.getMessage() );
			System.out.println( "If the problem is CUDA related. Make sure CUDA and cuDNN are in the LD_LIBRARY_PATH." );
			System.out.println( "The current library path is: LD_LIBRARY_PATH=" + System.getenv( "LD_LIBRARY_PATH" ) );
			System.out.println( "Using CPU version from jar file." );
		}
	}

	@Override
	public void loadInputNode( String defaultName, Dataset dataset ) {
		super.loadInputNode( defaultName, dataset );
		if ( sig != null && sig.isInitialized() && sig.getInputsCount() > 0 ) {
			inputNode.setName( sig.getInputsMap().keySet().iterator().next() );
			setInputTensor( sig.getInputsOrThrow( inputNode.getName() ) );
			inputNode.setNodeShape( getShape(getInputTensorInfo().getTensorShape()));
			inputNode.initializeNodeMapping();
		}
	}

	@Override
	public void loadOutputNode( String defaultName ) {
		super.loadOutputNode( defaultName );
		if ( sig != null && sig.isInitialized() && sig.getOutputsCount() > 0 ) {
			outputNode.setName( sig.getOutputsMap().keySet().iterator().next() );
			setOutputTensor( sig.getOutputsOrThrow( outputNode.getName() ) );
			outputNode.setNodeShape( getShape(getOutputTensorInfo().getTensorShape()));
			outputNode.initializeNodeMapping(inputNode.getNodeShape());
		}
	}
	
	private long[] getShape( TensorShapeProto tensorShape) {
		long[] shape = new long[tensorShape.getDimCount()];
		for(int i = 0; i < shape.length; i++){
			shape[i] = tensorShape.getDim( i ).getSize();
		}
		return shape;
	}
	
	@Override
	protected boolean loadModel( Location source, String modelName ) {
		try {
			model = tensorFlowService.loadModel( source, modelName, MODEL_TAG );
		} catch ( TensorFlowException | IOException e ) {
			e.printStackTrace();
			return false;
		}
		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		try {
			sig = MetaGraphDef.parseFrom( model.metaGraphDef() ).getSignatureDefOrThrow(
					DEFAULT_SERVING_SIGNATURE_DEF_KEY );
		} catch ( final InvalidProtocolBufferException e ) {
//			e.printStackTrace();
		}
		return true;
	}
	
	protected void setModel(SavedModelBundle model) {
		this.model = model;
	}
	
	@Override
	public void preprocess() {
		initMapping();
		calculateMapping();
	}

//	public RandomAccessibleInterval< FloatType > networkNodeToDataset(Tensor tensor, int[] mapping, boolean dropSingletonDims) {
//		return DatasetTensorflowConverter.tensorToDataset( tensor, mapping, dropSingletonDims );
//	}
	
	@Override
	public void initMapping() {
		inputNode.initMapping();
	}
	
	protected void calculateMapping() {

		for ( int i = 0; i < inputNode.getNodeShape().length; i++ ) {
			outputNode.setNodeAxis( i, inputNode.getNodeAxis( i ) );
		}
		outputNode.setNodePadding( inputNode.getNodePadding() );
		handleDimensionReduction();		
		inputNode.generateMapping();
		outputNode.generateMapping();
		
		System.out.println( "INPUT NODE: " );
		inputNode.printMapping();
		System.out.println( "OUTPUT NODE: " );
		outputNode.printMapping();
	}
	
	private void handleDimensionReduction() {
		if ( inputTensorInfo.getTensorShape().getDimCount() == outputTensorInfo.getTensorShape().getDimCount() + 1 ) {
			getOutputNode().removeZFromMapping(inputNode.getDataset());
			Dataset outputDummy = createEmptyDuplicateWithoutZAxis( inputNode.getDataset() );
			getOutputNode().initialize( outputDummy );
		}else{
			getOutputNode().initialize( inputNode.getDataset().duplicate() );
		}
	}
	
	private <T> Dataset createEmptyDuplicateWithoutZAxis(Dataset input) {
		int numDims = input.numDimensions();
		if(input.axis( Axes.Z ) != null){
			numDims--;
		}
		long[] dims = new long[numDims];
		AxisType[] axes = new AxisType[numDims];
		int j = 0;
		for(int i = 0; i < input.numDimensions(); i++) {
			AxisType axisType = input.axis( i ).type(); 
			if(axisType != Axes.Z) {
				axes[j] = axisType;
				dims[j] = input.dimension( i );
				j++;
			}
		}
		Dataset result = new ImageJ().dataset().create( new FloatType(), dims, "", axes );
		return result;
	}

	@Override
	public RandomAccessibleInterval< FloatType > execute( final RandomAccessibleInterval< FloatType > tile ) throws Exception {

		final Tensor inputTensor = DatasetTensorflowConverter.datasetToTensor( tile, getInputNode().getMapping() );
		if ( inputTensor != null ) {
			Tensor outputTensor = null;
			outputTensor = TensorFlowRunner.executeGraph(
					model,
					inputTensor,
					getInputTensorInfo(),
					getOutputTensorInfo() );

			if ( outputTensor != null ) { return DatasetTensorflowConverter.tensorToDataset(
					outputTensor,
					getOutputNode().getMapping(),
					dropSingletonDims ); }
		}
		return null;
	}
	
	@Override
	public boolean isInitialized() {
		return model != null;
	}
	
	public void setInputTensor( final TensorInfo tensorInfo ) {
		inputTensorInfo = tensorInfo;
		System.out.println(
				"DatasetTensorBridge::setInputTensorShape: " + tensorInfo.getTensorShape().getDimList() + "]" );
	}

	public void setOutputTensor( final TensorInfo tensorInfo ) {
		outputTensorInfo = tensorInfo;
		System.out.println(
				"DatasetTensorBridge::setOutputTensorShape: " + tensorInfo.getTensorShape().getDimList() + "]" );
	}

	public TensorInfo getInputTensorInfo() {
		return inputTensorInfo;
	}

	public TensorInfo getOutputTensorInfo() {
		return outputTensorInfo;
	}

	
}

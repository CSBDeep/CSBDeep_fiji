
package mpicbg.csbd.network.tensorflow;

import java.io.IOException;
import java.util.Arrays;

import org.scijava.io.location.Location;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;
import org.tensorflow.framework.TensorShapeProto;

import com.google.protobuf.InvalidProtocolBufferException;

import mpicbg.csbd.network.DefaultNetwork;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class TensorFlowNetwork<T extends RealType<T>> extends
	DefaultNetwork<T>
{

	private SavedModelBundle model;
	private SignatureDef sig;
	private final TensorFlowService tensorFlowService;
	private final DatasetService datasetService;
	private TensorInfo inputTensorInfo, outputTensorInfo;
	private boolean foundJNI = true;
	private boolean gpuSupport = false;
	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	private static final String MODEL_TAG = "serve";
	protected static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY =
		"serving_default";

	public TensorFlowNetwork(TensorFlowService tensorFlowService,
		DatasetService datasetService, Task associatedTask)
	{
		super(associatedTask);
		this.tensorFlowService = tensorFlowService;
		this.datasetService = datasetService;
		log("imagej-tensorflow version: " + tensorFlowService.getVersion());
		try {
			log("tensorflow version: " + TensorFlow.version());
		}
		catch (final UnsatisfiedLinkError e){
			foundJNI = false;
			logError("Couldn't load tensorflow library.");
			logError("By default, CSBDeep will load the tensorflow CPU library. " +
					"If you added a libtensorflow-jni.jar file to Fiji.app/lib/linux64 " +
					"to get GPU support, make sure you have the matching CUDA and cuDNN " +
					"versions installed. If you want to use the CPU library, make sure " +
					"the GPU tensorflow library file is removed from the lib folder.");
			e.printStackTrace();
		}
	}

	@Override
	public void testGPUSupport() {
		log("The current library path is: LD_LIBRARY_PATH=" + System.getenv(
			"LD_LIBRARY_PATH"));
		try {
			System.loadLibrary("tensorflow_jni");
			gpuSupport = true;
		}
		catch (final UnsatisfiedLinkError e) {
			gpuSupport = false;
		}
		if (!gpuSupport) {
			log("Couldn't load tensorflow GPU support.");
			log(
				"If the problem is CUDA related, make sure CUDA and cuDNN are in the LD_LIBRARY_PATH.");
			log("Using CPU version from jar file.");
		}
	}

	@Override
	public void loadInputNode(final String defaultName, final Dataset dataset) {
		super.loadInputNode(defaultName, dataset);
		if (sig != null && sig.isInitialized() && sig.getInputsCount() > 0) {
			inputNode.setName(sig.getInputsMap().keySet().iterator().next());
			setInputTensor(sig.getInputsOrThrow(inputNode.getName()));
			inputNode.setNodeShape(getShape(getInputTensorInfo().getTensorShape()));
			inputNode.initializeNodeMapping();
		}
	}

	@Override
	public void loadOutputNode(final String defaultName) {
		super.loadOutputNode(defaultName);
		if (sig != null && sig.isInitialized() && sig.getOutputsCount() > 0) {
			outputNode.setName(sig.getOutputsMap().keySet().iterator().next());
			setOutputTensor(sig.getOutputsOrThrow(outputNode.getName()));
			outputNode.setNodeShape(getShape(getOutputTensorInfo().getTensorShape()));
			outputNode.initializeNodeMapping(inputNode.getNodeShape());
		}
	}

	private long[] getShape(final TensorShapeProto tensorShape) {
		final long[] shape = new long[tensorShape.getDimCount()];
		for (int i = 0; i < shape.length; i++) {
			shape[i] = tensorShape.getDim(i).getSize();
		}
		return shape;
	}

	@Override
	protected boolean loadModel(final Location source, final String modelName) {
		try {
			if (model != null) {
				model.close();
			}
			model = tensorFlowService.loadModel(source, modelName, MODEL_TAG);
		}
		catch (TensorFlowException | IOException e) {
			e.printStackTrace();
			return false;
		}
		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		try {
			sig = MetaGraphDef.parseFrom(model.metaGraphDef()).getSignatureDefOrThrow(
				DEFAULT_SERVING_SIGNATURE_DEF_KEY);
		}
		catch (final InvalidProtocolBufferException e) {
			 e.printStackTrace();
		}
		return true;
	}

	protected void setModel(final SavedModelBundle model) {
		this.model = model;
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

		for (int i = 0; i < inputNode.getNodeShape().length; i++) {
			outputNode.setNodeAxis(i, inputNode.getNodeAxis(i));
		}
		handleDimensionReduction();
		generateMapping();
	}

	@Override
	public void doDimensionReduction() {
		handleDimensionReduction();
		generateMapping();
	}

	@Override
	public boolean libraryLoaded() {
		return foundJNI;
	}

	@Override
	public boolean supportsGPU() {
		return gpuSupport;
	}

	private void generateMapping() {
		inputNode.generateMapping();
		outputNode.generateMapping();
	}

	private void handleDimensionReduction() {
		if (doDimensionReduction) {
			getOutputNode().removeAxisFromMapping(axisToRemove);
			final Dataset outputDummy = createEmptyDuplicateWithoutAxis(inputNode
				.getDataset(), axisToRemove);
			getOutputNode().initialize(outputDummy);
		}
		else {
			getOutputNode().initialize(inputNode.getDataset().duplicate());
		}
	}

	private <T> Dataset createEmptyDuplicateWithoutAxis(final Dataset input,
		final AxisType axisToRemove)
	{
		int numDims = input.numDimensions();
		if (input.axis(axisToRemove) != null) {
			numDims--;
		}
		final long[] dims = new long[numDims];
		final AxisType[] axes = new AxisType[numDims];
		int j = 0;
		for (int i = 0; i < input.numDimensions(); i++) {
			final AxisType axisType = input.axis(i).type();
			if (axisType != axisToRemove) {
				axes[j] = axisType;
				dims[j] = input.dimension(i);
				j++;
			}
		}
		final Dataset result = datasetService.create(new FloatType(), dims, "",
			axes);
		return result;
	}

	// TODO this is the tensorflow runner
	@Override
	public RandomAccessibleInterval<T> execute(
		final RandomAccessibleInterval<T> tile) throws Exception
	{

		final Tensor inputTensor = DatasetTensorflowConverter.datasetToTensor(tile,
			getInputNode().getMapping());
		if (inputTensor != null) {
			RandomAccessibleInterval<T> output = null;
			Tensor outputTensor = TensorFlowRunner.executeGraph(model, inputTensor,
				getInputTensorInfo(), getOutputTensorInfo());

			if (outputTensor != null) {
				output = DatasetTensorflowConverter.tensorToDataset(outputTensor, tile
					.randomAccess().get(), getOutputNode().getMapping(),
					dropSingletonDims);
				outputTensor.close();
			}
			inputTensor.close();
			return output;
		}
		return null;
	}

	@Override
	public boolean isInitialized() {
		return model != null;
	}

	public void setInputTensor(final TensorInfo tensorInfo) {
		inputTensorInfo = tensorInfo;
		logTensorShape("Shape of input tensor", tensorInfo);
	}

	private void logTensorShape(String title, final TensorInfo tensorInfo) {
		long[] dims = new long[tensorInfo.getTensorShape().getDimCount()];
		for (int i = 0; i < dims.length; i++) {
			dims[i] = tensorInfo.getTensorShape().getDimList().get(i).getSize();
		}
		log(title + ": " + Arrays.toString(dims));
	}

	public void setOutputTensor(final TensorInfo tensorInfo) {
		outputTensorInfo = tensorInfo;
		logTensorShape("Shape of output tensor", tensorInfo);
	}

	public TensorInfo getInputTensorInfo() {
		return inputTensorInfo;
	}

	public TensorInfo getOutputTensorInfo() {
		return outputTensorInfo;
	}

	@Override
	public void dispose() {
		super.dispose();
		sig = null;
		model = null;
		inputTensorInfo = null;
		outputTensorInfo = null;
		foundJNI = true;
		gpuSupport = false;
	}

}

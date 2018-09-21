
package mpicbg.csbd.tasks;

import java.io.FileNotFoundException;

import org.scijava.io.location.Location;

import mpicbg.csbd.network.DefaultNetwork;
import mpicbg.csbd.network.ImageTensor;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class PseudoNetwork<T extends RealType<T>> extends DefaultNetwork<T> {

	private long[] inputShape;
	private boolean initialized = false;

	public PseudoNetwork(Task associatedTask) {
		super(associatedTask);
	}

	@Override
	public boolean loadModel(final String pathOrURL, final String modelName)
		throws FileNotFoundException
	{

		return true;

	}

	@Override
	public void testGPUSupport() {}

	@Override
	public void loadInputNode(final Dataset dataset) {
		inputNode = new ImageTensor();
		inputNode.initializeNodeMapping();
	}

	@Override
	public void loadOutputNode(Dataset dataset) {
		outputNode = new ImageTensor();
		outputNode.initializeNodeMapping();
	}

	@Override
	protected boolean loadModel(final Location source, final String modelName) {
		initialized = true;
		return true;
	}

	@Override
	public void preprocess() {
		initMapping();
		calculateMapping();
	}

	@Override
	public boolean supportsGPU() {
		return false;
	}

	@Override
	public void initMapping() {
		inputNode.setMappingDefaults();
	}

	@Override
	public void calculateMapping() {

		for (int i = 0; i < inputNode.getNodeShape().length; i++) {
			outputNode.setNodeAxis(i, inputNode.getNodeAxis(i));
		}
		handleDimensionReduction();
		inputNode.generateMapping();
		outputNode.generateMapping();

		System.out.println("INPUT NODE: ");
		inputNode.printMapping();
		System.out.println("OUTPUT NODE: ");
		outputNode.printMapping();
	}

	private void handleDimensionReduction() {
		getOutputNode().initialize(inputNode.getImage());
	}

	private Dataset createEmptyDuplicateWithoutAxis(final Dataset input,
		final AxisType axisToRemove)
	{
		int numDims = input.numDimensions();
		if (input.axis(Axes.Z) != null) {
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
		// TODO should not be FloatType but T and should not create ImageJ instance
		// (memory leak)
		final Dataset result = new ImageJ().dataset().create(new FloatType(), dims,
			"", axes);
		return result;
	}

	@Override
	public RandomAccessibleInterval<T> execute(
		final RandomAccessibleInterval<T> tile) throws Exception
	{

		return tile;

	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void doDimensionReduction() {}

	@Override
	public boolean libraryLoaded() {
		return true;
	}

	public long[] getInputShape() {
		return inputShape;
	}

	public void setInputShape(final long[] inputShape) {
		this.inputShape = inputShape;
	}

}

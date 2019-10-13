
package de.csbdresden.csbdeep.tasks;

import java.io.FileNotFoundException;
import java.util.List;

import org.scijava.io.location.Location;

import de.csbdresden.csbdeep.network.model.DefaultNetwork;
import de.csbdresden.csbdeep.network.model.ImageTensor;
import de.csbdresden.csbdeep.task.Task;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class PseudoNetwork<T extends RealType<T>> extends DefaultNetwork<T> {

	private long[] inputShape;
	private boolean initialized = false;

	public PseudoNetwork(Task associatedTask) {
		super(associatedTask);
	}

	@Override
	public boolean loadModel(final String pathOrURL, final String modelName)
		throws FileNotFoundException {
		return true;
	}

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
	public void initMapping() {
		inputNode.setMappingDefaults();
	}

	@Override
	public List<Integer> dropSingletonDims() {
		return null;
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


package mpicbg.csbd.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mpicbg.csbd.util.ArrayHelper;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

// TODO rename
public class ImageTensor {

	// I do not use the following line because it was returning the axes in a
	// different order in different setups
	// AxisType[] axes = Axes.knownTypes();
	AxisType[] availableAxes = { Axes.X, Axes.Y, Axes.Z, Axes.TIME,
		Axes.CHANNEL };

	private String name;
	private final List<AxisType> nodeAxes = new ArrayList<>();
	private long[] nodeShape;
	private final List<Integer> finalMapping = new ArrayList<>();
	private Dataset dataset;
	private boolean mappingInitialized = false;
	private boolean reducedZ = false;

	public ImageTensor() {

	}

	public void initialize(final Dataset dataset) {
		this.dataset = dataset;
		final long[] dims = new long[dataset.numDimensions()];
		dataset.dimensions(dims);
	}

	public void setNodeShape(final long[] shape) {

		nodeShape = shape;
		while (nodeAxes.size() > nodeShape.length) {
			nodeAxes.remove(nodeAxes.size() - 1);
		}
	}

	public void initializeNodeMapping() {
		initializeNodeMapping(nodeShape);
	}

	public void initializeNodeMapping(final long[] shape) {
		nodeAxes.clear();
		for (int i = 0; i < shape.length; i++) {
			nodeAxes.add(null);
		}
	}

	public long[] getNodeShape() {
		return nodeShape;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int[] getMapping() {
		return ArrayHelper.toIntArray(finalMapping);
	}

	public void generateMapping() {

		finalMapping.clear();

		for (int i = 0; i < nodeShape.length; i++) {
			finalMapping.add(getNodeDimByDatasetDim(i));
		}

		// if a dimension is not set, assign an unused dimension
		ArrayHelper.replaceNegativeIndicesWithUnusedIndices(finalMapping);
	}

	public AxisType getDimType(final int dim) {
		if (dataset.numDimensions() > dim) {
			return dataset.axis(dim).type();
		}
		return null;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void initMapping() {
		if (!isMappingInitialized()) {
			setMappingDefaults();
		}
	}

	public void setMappingDefaults() {
		setMappingInitialized(true);
		final int tensorDimCount = nodeShape.length;
		if (tensorDimCount == 5) {
			setNodeAxis(0, Axes.TIME);
			setNodeAxis(1, Axes.Z);
			setNodeAxis(2, Axes.Y);
			setNodeAxis(3, Axes.X);
			setNodeAxis(4, Axes.CHANNEL);
		}
		else {
			if (tensorDimCount == 4) {
				setNodeAxis(1, Axes.Y);
				setNodeAxis(2, Axes.X);
				if (dataset.dimension(Axes.Z) > 1) {
					setNodeAxis(0, Axes.Z);
					if (dataset.dimension(Axes.CHANNEL) > 1) {
						setNodeAxis(3, Axes.CHANNEL);
					}
					else {
						setNodeAxis(3, Axes.TIME);
					}
				}
				else {
					if (dataset.dimension(Axes.CHANNEL) > 1) {
						setNodeAxis(0, Axes.CHANNEL);
						setNodeAxis(3, Axes.TIME);
					}
					else {
						setNodeAxis(0, Axes.TIME);
						setNodeAxis(3, Axes.CHANNEL);
					}
				}
			}
		}
		// printMapping();
	}

	public void resetMapping() {
		nodeAxes.clear();
	}

	public int numDimensions() {
		return dataset.numDimensions();
	}

	public long getDatasetDimSize(final int knownAxesIndex) {
		if (availableAxes.length > knownAxesIndex) {
			return dataset.dimension(dataset.dimensionIndex(
				availableAxes[knownAxesIndex]));
		}
		return 1;
	}

	public String getDatasetDimName(final AxisType axis) {
		return axis.getLabel().substring(0, 1);
	}

	public String getDatasetDimName(final int knownAxesIndex) {
		if (availableAxes.length > knownAxesIndex) {
			return getDatasetDimName(availableAxes[knownAxesIndex]);
		}
		return "not found";
	}

	public boolean removeAxisFromMapping(final AxisType axisToRemove) {
		System.out.println("REMOVING " + axisToRemove.getLabel());
		if (!reducedZ) {
			if (nodeAxes.contains(axisToRemove)) {
				nodeAxes.remove(axisToRemove);
				reducedZ = true;
			}
			printMapping();
		}
		return reducedZ;
	}

	public void setMappingInitialized(final boolean mappingInitialized) {
		this.mappingInitialized = mappingInitialized;
	}

	public boolean isMappingInitialized() {
		return mappingInitialized;
	}

	public void printMapping() {
		if (dataset != null) {
			final AxisType[] axes = new AxisType[dataset.numDimensions()];
			for (int i = 0; i < dataset.numDimensions(); i++) {
				axes[i] = dataset.axis(i).type();
			}
			System.out.println("datasetAxes:" + Arrays.toString(axes));
		}
		System.out.println("nodeAxes:" + nodeAxes.toString());
		System.out.println("mapping:" + finalMapping.toString());
		System.out.println("--------------");
	}

	public Long getDatasetDimSizeByNodeDim(final int nodeDim) {
		final Integer index = getDatasetDimIndexByTFIndex(nodeDim);
		if (index != null) {
			return dataset.dimension(index);
		}
		return (long) 1;
	}

	public Integer getDatasetDimIndexByTFIndex(final int nodeDim) {
		if (nodeAxes.size() > nodeDim) {
			final AxisType axis = nodeAxes.get(nodeDim);
			if (dataset.axis(axis) != null) {
				return dataset.dimensionIndex(axis);
			}
		}
		return null;
	}

	public String getDatasetDimNameByNodeDim(final int nodeDim) {
		if (nodeAxes.size() > nodeDim) {
			return getDatasetDimName(nodeAxes.get(nodeDim));
		}
		return null;
	}

	public Integer getNodeDimByDatasetDim(final int datasetDim) {
		if (dataset.numDimensions() > datasetDim) {
			return nodeAxes.indexOf(dataset.axis(datasetDim).type());
		}
		return -1;
	}

	public AxisType getAxisByDatasetDim(final int datasetDim) {
		if (dataset.numDimensions() > datasetDim) {
			return dataset.axis(datasetDim).type();
		}
		return null;
	}

	public void setNodeAxis(final int index, final AxisType axisType) {
		if (nodeAxes.size() > index) {
			nodeAxes.set(index, axisType);
		}
	}

	public AxisType getNodeAxis(final int index) {
		return nodeAxes.get(index);
	}

	public void setNodeAxisByKnownAxesIndex(final int nodeDim,
		final int knownAxesIndex)
	{
		if (knownAxesIndex < availableAxes.length && nodeDim < nodeAxes.size()) {
			nodeAxes.set(nodeDim, availableAxes[knownAxesIndex]);
		}
	}

	public void setMapping(final AxisType[] newmapping) {
		setMappingInitialized(true);
		nodeAxes.clear();
		for (int i = 0; i < newmapping.length; i++) {
			nodeAxes.add(newmapping[i]);
		}
		printMapping();
	}

	public List<AxisType> getNodeAxes() {
		return nodeAxes;
	}

}

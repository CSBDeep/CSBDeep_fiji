
package mpicbg.csbd.network;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import mpicbg.csbd.task.Task;
import mpicbg.csbd.util.ArrayHelper;
import mpicbg.csbd.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

// TODO rename
public class ImageTensor {

	private class Dimension {
		public Dimension(AxisType type, long size) {
			this.type = type;
			this.size = size;
		}
		private AxisType type;
		private long size;

		public long getSize() {
			return size;
		}

		public AxisType getType() {
			return type;
		}

		public String toString() {
			return "(" + type + ", " + size + ")";
		}
	}

	// I do not use the following line because it was returning the axes in a
	// different order in different setups
	// AxisType[] axes = Axes.knownTypes();
	AxisType[] availableAxes = { Axes.X, Axes.Y, Axes.Z, Axes.TIME,
		Axes.CHANNEL };

	private String name;
	private List<Dimension> image;
	private List<Dimension> node;
	private final List<Integer> finalMapping = new ArrayList<>();

	public ImageTensor() {
	}

	public void initialize(final Dataset dataset) {
		image = new ArrayList<>();
		AxisType[] axes = DatasetHelper.getDimensionsAllAssigned(dataset);
		for (int i = 0; i < axes.length; i++) {
			image.add(new Dimension(axes[i], dataset.dimension(i)));
		}
	}

	public void initialize(List<Dimension> otherImage) {
		image = new ArrayList<>();
		for (int i = 0; i < otherImage.size(); i++) {
			image.add(new Dimension(otherImage.get(i).getType(), otherImage.get(i).getSize()));
		}
	}

	public void setNodeShape(final long[] shape) {

		node = new ArrayList<>();
		for(long dim : shape) {
			node.add(new Dimension(null, dim));
		}

//		nodeShape = shape;
//		while (nodeAxes.size() > nodeShape.length) {
//			nodeAxes.remove(nodeAxes.size() - 1);
//		}
	}

	public void initializeNodeMapping() {
		for (Dimension dim : node)
			dim.type = null;
	}

	public Long[] getNodeShape() {
		return node.stream()
				.map(Dimension::getSize)
				.collect(Collectors.toList()).toArray(new Long[0]);
	}

	public List<Dimension> getImage() {
		return image;
	}

	public List<Dimension> getNode() {
		return node;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int[] getMappingIndices() {
		return ArrayHelper.toIntArray(finalMapping);
	}

	public AxisType[] getMapping() {
		return node.stream()
				.map(Dimension::getType)
				.collect(Collectors.toList()).toArray(new AxisType[0]);
	}

	public void generateMapping() {

		finalMapping.clear();

		for (int i = 0; i < node.size(); i++) {
			finalMapping.add(getNodeDimByDatasetDim(i));
		}

		// if a size is not set, assign an unused size
		ArrayHelper.replaceNegativeIndicesWithUnusedIndices(finalMapping);

	}

	public long imageDimension(AxisType type) {
		for(Dimension dim : image) {
			if (dim.getType().equals(type)) return dim.getSize();
		}
		return 1;
	}

	public void setMappingDefaults() {
		final int tensorDimCount = node.size();
		if (tensorDimCount == 5) {
			setNodeAxis(0, Axes.TIME);
			setNodeAxis(1, Axes.Z);
			setNodeAxis(2, Axes.Y);
			setNodeAxis(3, Axes.X);
			setNodeAxis(4, Axes.CHANNEL);
			if(imageDimension(Axes.Z) <= 1 && imageDimension(Axes.TIME) > 1) {
				setNodeAxis(1, Axes.TIME);
				setNodeAxis(0, Axes.Z);
			}
		}
		else {
			if (tensorDimCount == 4) {
				setNodeAxis(1, Axes.Y);
				setNodeAxis(2, Axes.X);
				setNodeAxis(3, Axes.CHANNEL);
				if (imageDimension(Axes.Z) > 1) {
					setNodeAxis(0, Axes.Z);
				}
				else {
					setNodeAxis(0, Axes.TIME);
				}
			}
		}
		// printMapping();
	}

	public int numDimensions() {
		return image.size();
	}

	public String getDatasetDimName(final AxisType axis) {
		return axis.getLabel().substring(0, 1);
	}

	public String getDatasetDimName(final int datasetAxisIndex) {
		if (numDimensions() > datasetAxisIndex) {
			return getDatasetDimName(image.get(datasetAxisIndex).getType());
		}
		return "not found";
	}

	public void printMapping() {
		printMapping(null);
	}

	public void printMapping(Task task) {
		Consumer<String> logFunction = System.out::println;
		if(task != null) logFunction = task::log;
		logFunction.accept("Mapping of tensor " + getName() + ": ");
		if (image != null) {
			final AxisType[] axes = new AxisType[image.size()];
			for (int i = 0; i < image.size(); i++) {
				axes[i] = image.get(i).getType();
			}
			logFunction.accept("   datasetAxes:" + Arrays.toString(axes));
		}
		logFunction.accept("   nodeAxes:" + node.toString());
		logFunction.accept("   mapping:" + finalMapping.toString());
	}

	public Long getDatasetDimSizeByNodeDim(final int nodeDim) {
		final Integer index = getDatasetDimIndexByTFIndex(nodeDim);
		if (index != null) {
			return image.get(index).size;
		}
		return (long) 1;
	}

	public Integer getDatasetDimIndexByTFIndex(final int nodeDim) {
		if (node.size() > nodeDim) {
			final AxisType axis = node.get(nodeDim).getType();
			for (int i = 0; i < image.size(); i++) {
				if(image.get(i).getType().equals(axis)) return i;
			}
		}
		return null;
	}

	public String getDatasetDimNameByNodeDim(final int nodeDim) {
		if (node.size() > nodeDim) {
			return getDatasetDimName(node.get(nodeDim).getType());
		}
		return null;
	}

	public Integer getNodeDimByDatasetDim(final int datasetDim) {
		if (image.size() > datasetDim) {
			for (int i = 0; i < node.size(); i++) {
				if(node.get(i).getType().equals(image.get(datasetDim).getType())) return i;
			}
		}
		return -1;
	}

	public void setNodeAxis(final int index, final AxisType axisType) {
		if (node.size() > index) {
			node.get(index).type = axisType;
		}
	}

	public AxisType getNodeAxis(final int index) {
		return node.get(index).getType();
	}

	public void setNodeAxisByKnownAxesIndex(final int nodeDim,
		final int knownAxesIndex)
	{
		if (knownAxesIndex < availableAxes.length && nodeDim < node.size()) {
			node.get(nodeDim).type = availableAxes[knownAxesIndex];
		}
	}

	public void setMapping(final AxisType[] newmapping) {
//		node.clear();
		for (int i = 0; i < newmapping.length; i++) {
			node.get(i).type = newmapping[i];
		}
		printMapping();
	}

	public List<AxisType> getNodeAxes() {
		return node.stream()
				.map(Dimension::getType)
				.collect(Collectors.toList());
	}

	public List<AxisType> getImageAxes() {
		return image.stream()
				.map(Dimension::getType)
				.collect(Collectors.toList());
	}

	public List<Long> getImageDimensions() {
		return image.stream()
				.map(Dimension::getSize)
				.collect(Collectors.toList());
	}

}

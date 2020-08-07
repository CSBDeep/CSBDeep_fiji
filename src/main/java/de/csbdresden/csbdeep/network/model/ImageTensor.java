/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2020 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package de.csbdresden.csbdeep.network.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.csbdresden.csbdeep.task.Task;
import de.csbdresden.csbdeep.tiling.Tiling;
import de.csbdresden.csbdeep.util.ArrayHelper;
import de.csbdresden.csbdeep.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ImageTensor {

	private class Dimension {

		Dimension(AxisType type, long size) {
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
	private boolean tilingAllowed = true;

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

	void setNode(long[] dimensions, AxisType[] axisTypes) {
		if(node != null) node.clear();
		else node = new ArrayList<>();
		for (int i = 0; i < dimensions.length; i++) {
			node.add(new Dimension(axisTypes[i], dimensions[i]));
		}
	}

	public <T extends RealType<T> & NativeType<T>> void setImageShape(RandomAccessibleInterval<T> result) {
		AxisType[] axes = getFinalAxesArray();
		image.clear();
		for (int i = 0; i < result.numDimensions(); i++) {
			image.add(new Dimension(axes[i], result.dimension(i)));
		}
	}

	public void initialize(long[] dimensions, AxisType[] axisTypes) {
		image = new ArrayList<>();
		for (int i = 0; i < dimensions.length; i++) {
			image.add(new Dimension(axisTypes[i], dimensions[i]));
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
			finalMapping.add(getDatasetDimIndexByNodeIndex(i));
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
		final Integer index = getDatasetDimIndexByNodeIndex(nodeDim);
		if (index != null) {
			return image.get(index).size;
		}
		return (long) 1;
	}

	private Integer getDatasetDimIndexByNodeIndex(final int nodeDim) {
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

	private Integer getNodeDimByDatasetDim(final int datasetDim) {
		if (image.size() > datasetDim) {
			for (int i = 0; i < node.size(); i++) {
				if(node.get(i).getType() != null && node.get(i).getType().equals(image.get(datasetDim).getType())) return i;
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
		if(index >= node.size()) return null;
		return node.get(index).getType();
	}

	public void setNodeAxisByKnownAxesIndex(final int nodeDim,
		final int knownAxesIndex)
	{
		if (knownAxesIndex < availableAxes.length && nodeDim < node.size()) {
			node.get(nodeDim).type = availableAxes[knownAxesIndex];
		}
	}

	public void setNodeAxisByImageAxisIndex(final int nodeDim,
	                                        final int axisIndex)
	{
		if (axisIndex < image.size()) {
			node.get(nodeDim).type = image.get(axisIndex).type;
		}
	}

	public void setMapping(final AxisType[] newmapping) {
//		node.clear();
		for (int i = 0; i < newmapping.length; i++) {
			node.get(i).type = newmapping[i];
		}
		generateMapping();
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

	public AxisType[] getAxesArray() {
		int numDim = getNodeShape().length;
		boolean hasChannel = imageHasChannel();
		if(!hasChannel && numDim <= image.size()) numDim = image.size()+1;
		final AxisType[] res = new AxisType[numDim];
		Arrays.fill(res, Axes.unknown());
		if(!hasChannel) res[image.size()] = Axes.CHANNEL;
		for (int i = 0; i < getMapping().length; i++) {
			int nodeI = getNodeDimByDatasetDim(i);
			if(nodeI >= 0) {
				res[i] = node.get(nodeI).type;
			}
		}
		return res;
	}


	public AxisType[] getFinalAxesArray() {
		AxisType[] res = new AxisType[getNodeShape().length];
		Arrays.fill(res, Axes.unknown());
		for (int i = 0; i < res.length; i++) {
			int imgI = finalMapping.indexOf(i);
			if(imgI >= 0) res[i] = node.get(imgI).type;
		}
		return res;
	}

	private boolean imageHasChannel() {
		for(Dimension d : image) {
			if(d != null && d.type != null && d.type.equals(Axes.CHANNEL)) return true;
		}
		return false;
	}

	public Tiling.TilingAction[] getTilingActions() {

		if(getNodeShape().length == 0) return null;
		Tiling.TilingAction[] actions = new Tiling.TilingAction[Math.max(getImageDimensions().size(), getNodeShape().length)];
		Arrays.fill(actions, Tiling.TilingAction.NO_TILING);
		actions[0] = Tiling.TilingAction.TILE_WITHOUT_PADDING; // img batch dimension
		for (int i = 1; i < getNodeShape().length-1; i++) {
			if(getNodeShape()[i] < 0) {
				actions[i] = Tiling.TilingAction.TILE_WITH_PADDING;
			}
		}
		//permute
		Tiling.TilingAction[] imgActions = new Tiling.TilingAction[actions.length];
		Arrays.fill(imgActions, Tiling.TilingAction.NO_TILING);
		for (int i = 0; i < finalMapping.size(); i++) {
			imgActions[i] = actions[finalMapping.indexOf(i)];
		}
		return imgActions;
	}

	private boolean isImageDimUseless(int index) {
		return image.size() <= index || image.get(index).size == 1L;
	}

	public List<Integer> dropSingletonDims() {
		List<Integer> res = new ArrayList<>();
		for (int i = image.size()-1; i >= 0; i--) {
			if(isImageDimUseless(i)) {
				res.add(i);
				image.remove(i);
				for (int j = i; j < image.size() ; j++) {
					for (int k = 0; k < finalMapping.size(); k++) {
						if(finalMapping.get(k) > i) {
							finalMapping.set(k, finalMapping.get(k)-1);
						}
					}
				}
			}
		}
		return res;
	}

	public void setTilingAllowed(boolean allowed) {
		this.tilingAllowed = allowed;
	}

	public boolean getTilingAllowed() {
		return tilingAllowed;
	}

}

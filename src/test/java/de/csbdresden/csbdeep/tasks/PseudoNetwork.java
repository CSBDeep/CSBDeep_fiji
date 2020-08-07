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

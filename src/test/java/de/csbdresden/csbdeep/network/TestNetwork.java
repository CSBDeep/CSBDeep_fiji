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

package de.csbdresden.csbdeep.network;

import de.csbdresden.csbdeep.network.model.tensorflow.TensorFlowNetwork;
import net.imagej.Dataset;
import net.imglib2.type.numeric.RealType;
import org.tensorflow.framework.TensorInfo;

public abstract class TestNetwork<T extends RealType<T>> extends
		TensorFlowNetwork<T>
{

	protected long[] inputShape;
	protected long[] outputShape;
	protected int inputCount = 1;
	protected int outputCount = 1;

	public TestNetwork()
	{
		super(null);
	}

	@Override
	public void loadInputNode(final Dataset dataset) {
		super.loadInputNode( dataset);
		if (inputCount > 0) {
			inputNode.setName("input");
			inputNode.setNodeShape(inputShape);
			inputNode.initializeNodeMapping();
		}
	}

	@Override
	public void loadOutputNode(Dataset dataset) {
		super.loadOutputNode(dataset);
		if (outputCount > 0) {
			outputNode.setName("output");
			outputNode.setNodeShape(outputShape);
			outputNode.initializeNodeMapping();
		}
	}

	@Override
	protected void logTensorShape(String title, final TensorInfo tensorInfo) {
		log("cannot log tensorinfo shape of test networks");
	}

}

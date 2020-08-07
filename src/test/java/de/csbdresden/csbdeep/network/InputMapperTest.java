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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.csbdresden.csbdeep.network.model.Network;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.type.numeric.real.FloatType;

public class InputMapperTest {

	private void doMapping(Dataset input, Network network) {
		network.loadInputNode(input);
		network.loadOutputNode(input);
		network.initMapping();
		new DefaultInputMapper().run(input, network);
		network.calculateMapping();
		System.out.println("INPUT:");
		network.getInputNode().printMapping();
		System.out.println("OUTPUT:");
		network.getOutputNode().printMapping();
	}

	@Test
	public void network2D2D_input2D() {
		ImageJ ij = new ImageJ();
		Dataset input = ij.dataset().create(new FloatType(), new long[]{2,3}, "", new AxisType[]{Axes.X, Axes.Y});
		Network network = new TestNetworkSingle2D2D();
		ij.context().inject(network);

		doMapping(input, network);

		assertEquals(Axes.TIME, network.getInputNode().getNodeAxis(0));
		assertEquals(Axes.Y, network.getInputNode().getNodeAxis(1));
		assertEquals(Axes.X, network.getInputNode().getNodeAxis(2));
		assertEquals(Axes.CHANNEL, network.getInputNode().getNodeAxis(3));

		assertEquals(Axes.TIME, network.getOutputNode().getNodeAxis(0));
		assertEquals(Axes.Y, network.getOutputNode().getNodeAxis(1));
		assertEquals(Axes.X, network.getOutputNode().getNodeAxis(2));
		assertEquals(Axes.CHANNEL, network.getOutputNode().getNodeAxis(3));

		assertEquals(4, network.getInputNode().getMappingIndices().length);
		assertEquals(2, network.getInputNode().getMappingIndices()[0]);
		assertEquals(1, network.getInputNode().getMappingIndices()[1]);

		ij.context().dispose();
	}

	@Test
	public void network2D2D_input3D() {
		ImageJ ij = new ImageJ();
		Dataset input = ij.dataset().create(new FloatType(), new long[]{2,3,4}, "", new AxisType[]{Axes.X, Axes.Y, Axes.Z});
		Network network = new TestNetworkSingle2D2D();
		ij.context().inject(network);

		doMapping(input, network);

		assertEquals(Axes.Z, network.getInputNode().getNodeAxis(0));
		assertEquals(Axes.Y, network.getInputNode().getNodeAxis(1));
		assertEquals(Axes.X, network.getInputNode().getNodeAxis(2));
		assertEquals(Axes.CHANNEL, network.getInputNode().getNodeAxis(3));

		assertEquals(Axes.Z, network.getOutputNode().getNodeAxis(0));
		assertEquals(Axes.Y, network.getOutputNode().getNodeAxis(1));
		assertEquals(Axes.X, network.getOutputNode().getNodeAxis(2));
		assertEquals(Axes.CHANNEL, network.getOutputNode().getNodeAxis(3));

		assertEquals(4, network.getInputNode().getMappingIndices().length);
		assertEquals(2, network.getInputNode().getMappingIndices()[0]);
		assertEquals(1, network.getInputNode().getMappingIndices()[1]);
		assertEquals(0, network.getInputNode().getMappingIndices()[2]);

		ij.context().dispose();
	}

	@Test
	public void network3D2D_input3D() {
		ImageJ ij = new ImageJ();
		Dataset input = ij.dataset().create(new FloatType(), new long[]{2,3,4}, "", new AxisType[]{Axes.X, Axes.Y, Axes.Z});
		Network network = new TestNetworkSingle3D2D();
		ij.context().inject(network);

		doMapping(input, network);

		assertEquals(Axes.TIME, network.getInputNode().getNodeAxis(0));
		assertEquals(Axes.Z, network.getInputNode().getNodeAxis(1));
		assertEquals(Axes.Y, network.getInputNode().getNodeAxis(2));
		assertEquals(Axes.X, network.getInputNode().getNodeAxis(3));
		assertEquals(Axes.CHANNEL, network.getInputNode().getNodeAxis(4));

		assertEquals(Axes.TIME, network.getOutputNode().getNodeAxis(0));
		assertEquals(Axes.Y, network.getOutputNode().getNodeAxis(1));
		assertEquals(Axes.X, network.getOutputNode().getNodeAxis(2));
		assertEquals(Axes.CHANNEL, network.getOutputNode().getNodeAxis(3));

		assertEquals(5, network.getInputNode().getMappingIndices().length);
		assertEquals(3, network.getInputNode().getMappingIndices()[0]);
		assertEquals(2, network.getInputNode().getMappingIndices()[1]);
		assertEquals(1, network.getInputNode().getMappingIndices()[2]);

		ij.context().dispose();
	}

	@Test
	public void single3D3DTest() {
		ImageJ ij = new ImageJ();
		Dataset input = ij.dataset().create(new FloatType(), new long[]{2,3,4}, "", new AxisType[]{Axes.X, Axes.Y, Axes.Z});
		Network network = new TestNetworkSingle3D3D();
		ij.context().inject(network);

		doMapping(input, network);

		assertEquals(Axes.TIME, network.getInputNode().getNodeAxis(0));
		assertEquals(Axes.Z, network.getInputNode().getNodeAxis(1));
		assertEquals(Axes.Y, network.getInputNode().getNodeAxis(2));
		assertEquals(Axes.X, network.getInputNode().getNodeAxis(3));
		assertEquals(Axes.CHANNEL, network.getInputNode().getNodeAxis(4));

		assertEquals(Axes.TIME, network.getOutputNode().getNodeAxis(0));
		assertEquals(Axes.Z, network.getOutputNode().getNodeAxis(1));
		assertEquals(Axes.Y, network.getOutputNode().getNodeAxis(2));
		assertEquals(Axes.X, network.getOutputNode().getNodeAxis(3));
		assertEquals(Axes.CHANNEL, network.getOutputNode().getNodeAxis(4));

		assertEquals(5, network.getInputNode().getMappingIndices().length);
		assertEquals(3, network.getInputNode().getMappingIndices()[0]);
		assertEquals(2, network.getInputNode().getMappingIndices()[1]);
		assertEquals(1, network.getInputNode().getMappingIndices()[2]);

		ij.context().dispose();
	}
}

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

package de.csbdresden.csbdeep.commands;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.scijava.module.Module;

import de.csbdresden.csbdeep.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.real.FloatType;

public class GenericNetworksMismatchTest extends CSBDeepTest {

	@Test
	public void test3DNetworkWith3DInputImage() {

		launchImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 20, 1 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test("denoise3D/model.zip", input);

	}

	@Test
	public void test2DNetworkWith2DInputImage() {

		launchImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 10, 20 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test("denoise2D/model.zip", input);
		test("denoise2D/model.zip", input);

	}

	@Test
	public void test3DNetworkWith2DInputImage() {

		launchImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 20 }, new AxisType[] {
				Axes.X, Axes.Y });

		test("denoise3D/model.zip", input);

	}

	@Test
	public void test2DNetworkWith3DInputImage() {

		launchImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 20, 30 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test("denoise2D/model.zip", input);

	}

	private void test(String network, Dataset input) {
		URL networkUrl = this.getClass().getResource(network);
		try {
			final Module module = ij.command().run(GenericNetwork.class,
					false,
					"input", input,
					"modelFile", new File(networkUrl.getPath()),
					"overlap", 2).get();
			assertNotNull(module);
			final Dataset output = (Dataset) module.getOutput("output");
			assertNotNull(output);
			testResultAxesAndSize(input, output);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

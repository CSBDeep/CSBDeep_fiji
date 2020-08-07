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

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import de.csbdresden.csbdeep.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;

public class NetPlanariaTest extends CSBDeepTest {

	@Test
	public void testPlanariaFloatTypeXY() {
		testDataset(new FloatType(), new long[] { 4, 3 }, new AxisType[] {
				Axes.X, Axes.Y });
	}

	@Test
	public void testPlanariaFloatTypeXYZ() {
		testDataset(new FloatType(), new long[] { 4, 3, 2 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });
	}

	@Test
	public void testPlanariaFloatTypeXYZC() {
		testDataset(new FloatType(), new long[] { 3, 4, 10, 1 }, new AxisType[] {
			Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL });
	}

	@Test
	public void testPlanariaInvalidDataset() {
		launchImageJ();
		final Dataset input = createDataset(new FloatType(), new long[] { 3, 4, 10, 2 }, new AxisType[]
				{ Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL });
		final Dataset result = runPlugin(NetPlanaria.class, input);
		assertNull(result);
	}

	@Test
	public void testPlanariaFloatTypeXZY() {
		testDataset(new FloatType(), new long[] { 5, 10, 1 }, new AxisType[] {
				Axes.X, Axes.Z, Axes.Y });
	}

	@Test
	public void testNetPlanariaByteType() {
		testDataset(new ByteType(), new long[] { 20, 10, 5 }, new AxisType[] {
			Axes.X, Axes.Y, Axes.Z });
	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes)
	{

		launchImageJ();
		final Dataset input = createDataset(type, dims, axes);
		final Dataset output = runPlugin(NetPlanaria.class, input);
		assertNotNull(output);
		testResultAxesAndSize(input, output);
	}

}

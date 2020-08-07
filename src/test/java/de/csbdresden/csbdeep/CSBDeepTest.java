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

package de.csbdresden.csbdeep;

import static de.csbdresden.csbdeep.tiling.DefaultTiling.arrayProduct;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.Future;

import org.junit.After;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import de.csbdresden.csbdeep.imglib2.TiledView;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.AbstractInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class CSBDeepTest {

	protected ImageJ ij;

	@After
	public void disposeIJ() {
		if(ij != null) ij.context().dispose();
	}

	public void testCSBDeepTest() {
		launchImageJ();
		final Dataset input = createDataset(new FloatType(), new long[] { 30, 80, 2,
			5 }, new AxisType[] { Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z });
		assertEquals(Axes.X, input.axis(0).type());
		assertEquals(Axes.Y, input.axis(1).type());
		assertEquals(Axes.CHANNEL, input.axis(2).type());
		assertEquals(Axes.Z, input.axis(3).type());
	}

	protected void launchImageJ() {
		launchImageJ(true);
	}

	protected void launchImageJ(boolean headless) {
		if (ij == null) {
			ij = new ImageJ();
			ij.ui().setHeadless(headless);
		}
	}

	protected <T extends RealType<T> & NativeType<T>> Dataset createDataset(
		final T type, final long[] dims, final AxisType[] axes)
	{
		return ij.dataset().create(type, dims, "", axes);
	}

	protected <C extends Command> Dataset runPlugin(
		final Class<C> pluginClass, final Dataset dataset)
	{
		final Future<CommandModule> future = ij.command().run(pluginClass, false,
			"input", dataset);
		assertNotEquals(null, future);
		final Module module = ij.module().waitFor(future);
		return (Dataset) module.getOutput("output");
	}

	protected void testResultAxesAndSize(final Dataset input,
		final Dataset output)
	{
		printDim("input", input);
		printAxes("input", input);
		printDim("output", output);
		printAxes("output", output);
		int j = 0;
		for (int i = 0; i < input.numDimensions(); i++) {
			if (input.dimension(i) == 1L) continue;
			if (input.axis(i).type() == Axes.CHANNEL) {
				assertTrue(
						"Since the demo networks are probabilistic, the channels should double",
						output.dimension(j) == input.dimension(i)*2);
			}else {
				assertEquals(input.dimension(i), output.dimension(j));
			}
			assertEquals(input.axis(i).type(), output.axis(j).type());
			j++;
		}
		for (int i = 0; i < output.numDimensions(); i++) {
			if (!input.axis(output.axis(i).type()).isPresent() && !output.axis(i).type().equals(Axes.CHANNEL)) {
				assertEquals(1, output.dimension(i));
			}
		}
	}

	protected <T> void compareDimensions(final RandomAccessibleInterval<T> input,
		final RandomAccessibleInterval<T> output)
	{
		for (int i = 0; i < input.numDimensions(); i++) {
			assertEquals(input.dimension(i), output.dimension(i));
		}
	}

	protected void testResultAxesAndSizeByRemovingAxis(final Dataset input,
		final Dataset output, final AxisType axisToRemove)
	{
		printDim("input", input);
		printAxes("input", input);
		printDim("output", output);
		printAxes("output", output);
		int i_output = 0;
		for (int i = 0; i < input.numDimensions(); i++) {
			final AxisType axis = input.axis(i).type();
			if (axis == axisToRemove) continue;
			assertEquals(axis, output.axis(i_output).type());
			assertEquals(input.dimension(i), output.dimension(i_output));
			i_output++;
		}
	}

	protected static void printDim(final String title, final Dataset input) {
		final long[] dims = new long[input.numDimensions()];
		input.dimensions(dims);
		System.out.println(title + ": " + Arrays.toString(dims));
	}

	protected static void printDim(final String title, final AbstractInterval input) {
		final long[] dims = new long[input.numDimensions()];
		input.dimensions(dims);
		System.out.println(title + ": " + Arrays.toString(dims));
	}

	protected static void printAxes(final String title, final Dataset input) {
		final String[] axes = new String[input.numDimensions()];
		for (int i = 0; i < axes.length; i++) {
			axes[i] = input.axis(i).type().getLabel();
		}
		System.out.println(title + ": " + Arrays.toString(axes));
	}

	protected static long getNumTiles(TiledView tiledView) {
		long[] dims = new long[tiledView.numDimensions()];
		tiledView.dimensions(dims);
		return arrayProduct(dims);
	}

}

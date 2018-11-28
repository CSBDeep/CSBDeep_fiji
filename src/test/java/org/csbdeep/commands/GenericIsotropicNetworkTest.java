
package org.csbdeep.commands;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import org.csbdeep.CSBDeepTest;
import org.junit.Assert;
import org.junit.Test;
import org.scijava.module.Module;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class GenericIsotropicNetworkTest extends CSBDeepTest {

	@Test
	public void testCompatibleInput() {
		launchImageJ();
		testDataset(new FloatType(), new long[] { 10, 10, 10, 2 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL });

	}

	@Test
	public void testIncompatibleInput() {
		launchImageJ();

		URL networkUrl = this.getClass().getResource("isoNet/model.zip");
		final Dataset input = createDataset(new FloatType(), new long[] { 10, 10, 10 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL });
		boolean noException = true;
		try {
			final Module module = ij.command().run(GenericIsotropicNetwork.class, false,
					"modelFile", new File(networkUrl.getPath()), "input", input, "scale", 1.5).get();
			assertNotNull(module);
			assertNull(module.getOutput("output"));
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes) {

		URL networkUrl = this.getClass().getResource("isoNet/model.zip");
		final Dataset input = createDataset(type, dims, axes);
		try {
			final Module module = ij.command().run(GenericIsotropicNetwork.class, false,
					"modelFile", new File(networkUrl.getPath()), "input", input, "scale", 1.5).get();
			assertNotEquals(null, module);
			final Dataset output = (Dataset) module.getOutput("output");

			Assert.assertNotNull(output);
			testResultAxesAndSize(input, output);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void testResultAxesAndSize(final Dataset input,
	                                     final Dataset output)
	{
		printDim("input", input);
		printAxes("input", input);
		printDim("output", output);
		printAxes("output", output);
		int i_output = 0;
		for (int i = 0; i < input.numDimensions(); i++) {
			final AxisType axis = input.axis(i).type();
			if (axis == Axes.Z) {
				assertTrue(
						"Z axis dimension size output should be greater than input size ",
						output.dimension(i_output) > input.dimension(i));
			}else {
				assertEquals(input.dimension(i), output.dimension(i_output));
			}
			assertEquals(axis, output.axis(i_output).type());
			i_output++;
		}
	}

}

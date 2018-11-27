
package org.csbdeep.commands;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.real.FloatType;
import org.csbdeep.CSBDeepTest;
import org.junit.Assert;
import org.junit.Test;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class GenericIsotropicNetworkTest extends CSBDeepTest {

	@Test
	public void testGenericIsotropicNetwork() {
		launchImageJ();
		testDataset(new FloatType(), new long[] { 10, 10, 10, 2 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL });

	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes) {

		URL networkUrl = this.getClass().getResource("isoNet/model.zip");
		final Dataset input = createDataset(type, dims, axes);
		final Future<CommandModule> future = ij.command().run(GenericIsotropicNetwork.class, false,
				"modelFile", new File(networkUrl.getPath()), "input", input, "scale", 1.5);
		assertNotEquals(null, future);
		final Module module = ij.module().waitFor(future);
		final Dataset output = (Dataset) module.getOutput("output");

		Assert.assertNotNull(output);
		testResultAxesAndSize(input, output);
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

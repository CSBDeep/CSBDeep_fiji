
package de.csbdresden.csbdeep.commands;

import static org.junit.Assert.*;

import java.util.concurrent.Future;

import org.junit.Test;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import de.csbdresden.csbdeep.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class NetIsoTest extends CSBDeepTest {

	@Test
	public void testNetIsoInvalidDataset() {
		launchImageJ();
		final Dataset input = createDataset(new FloatType(), new long[] {5, 10}, new AxisType[]
				{Axes.X, Axes.Y});
		final Dataset result = runPlugin(NetIso.class, input);
		assertNull(result);
	}

	@Test
	public void testNetIso() {
		testDataset(new FloatType(), new long[] { 5, 10, 2, 5 }, new AxisType[] {
			Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z });
	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes)
	{

		launchImageJ();
		final Dataset input = createDataset(type, dims, axes);
		final Future<CommandModule> future = ij.command().run(NetIso.class, false,
				"input", input, "scale", 1.5);
		assertNotEquals(null, future);
		final Module module = ij.module().waitFor(future);
		final Dataset output = (Dataset) module.getOutput("output");

		assertNotNull(output);
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
				if (axis == Axes.CHANNEL) {
					assertTrue(
							"Since the iso network is probabilistic, the channels should double",
							output.dimension(i_output) == input.dimension(i)*2);
				}
				else {
					assertEquals(input.dimension(i), output.dimension(i_output));
				}
			}
			assertEquals(axis, output.axis(i_output).type());
			i_output++;
		}
	}
}

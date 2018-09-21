
package mpicbg.csbd.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;

import mpicbg.csbd.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

public class NetIsoTest extends CSBDeepTest {

	@Test
	// @Ignore
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
		final List<Dataset> result = (List<Dataset>) module.getOutput("output");

		assertTrue("result should contain one dataset, not " + result.size(), result
			.size() == 1);
		final Dataset output = result.get(0);
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

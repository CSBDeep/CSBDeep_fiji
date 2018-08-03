
package mpicbg.csbd.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import mpicbg.csbd.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class NetIsoTest extends CSBDeepTest {

	@Test
	// @Ignore
	public void testNetIso() {
		testDataset(new FloatType(), new long[] { 30, 80, 2, 5 }, new AxisType[] {
			Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z });
	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes)
	{

		launchImageJ();
		final Dataset input = createDataset(type, dims, axes);
		final List<Dataset> result = runPlugin(NetIso.class, input);
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
			}
			else {
				assertEquals(input.dimension(i), output.dimension(i_output));
			}
			assertEquals(axis, output.axis(i_output).type());
			i_output++;
		}
	}
}

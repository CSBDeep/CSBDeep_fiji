
package mpicbg.csbd;

import mpicbg.csbd.imglib2.TiledView;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.AbstractInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import static mpicbg.csbd.tiling.DefaultTiling.arrayProduct;
import static org.junit.Assert.*;

public class CSBDeepTest {

	protected ImageJ ij;

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
		if (ij == null) {
			ij = new ImageJ();
			ij.ui().setHeadless(true);
		}
	}

	protected <T extends RealType<T> & NativeType<T>> Dataset createDataset(
		final T type, final long[] dims, final AxisType[] axes)
	{
		return ij.dataset().create(type, dims, "", axes);
	}

	protected <C extends Command> List<Dataset> runPlugin(
		final Class<C> pluginClass, final Dataset dataset)
	{
		final Future<CommandModule> future = ij.command().run(pluginClass, false,
			"input", dataset);
		assertNotEquals(null, future);
		final Module module = ij.module().waitFor(future);
		return (List<Dataset>) module.getOutput("output");
	}

	protected void testResultAxesAndSize(final Dataset input,
		final Dataset output)
	{
		printDim("input", input);
		printAxes("input", input);
		printDim("output", output);
		printAxes("output", output);
		for (int i = 0; i < input.numDimensions(); i++) {
			if (input.axis(i).type() == Axes.CHANNEL) {
				assertTrue(
						"Since the demo networks are probabilistic, the channels should double",
						output.dimension(i) == input.dimension(i)*2);
			}else {
				assertEquals(input.dimension(i), output.dimension(i));
			}
			assertEquals(input.axis(i).type(), output.axis(i).type());
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

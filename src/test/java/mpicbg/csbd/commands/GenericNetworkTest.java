
package mpicbg.csbd.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Ignore;
import org.junit.Test;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import mpicbg.csbd.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.real.FloatType;

public class GenericNetworkTest extends CSBDeepTest {

	@Test
	@Ignore
	public void testGenericNetwork() {
		launchImageJ();
		for (int i = 0; i < 1; i++) {

			testDataset(new FloatType(), new long[] { 10, 10, 10 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });
			testDataset(new UnsignedIntType(), new long[] { 10, 10, 10 },
				new AxisType[] { Axes.X, Axes.Y, Axes.Z });
			testDataset(new ByteType(), new long[] { 10, 10, 10 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

			if (i % 10 == 0) System.out.println(i);
		}

	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes)
	{

		final Dataset input = createDataset(type, dims, axes);
		final Future<CommandModule> future = ij.command().run(GenericNetwork.class,
			false, "input", input, "modelFile", new File(
				"/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip"));
		assertNotEquals(null, future);
		final Module module = ij.module().waitFor(future);
		List<Dataset> result = (List<Dataset>) module.getOutput("output");
		assertEquals(1, result.size());
		final Dataset output = result.get(0);
		testResultAxesAndSize(input, output);
	}

}

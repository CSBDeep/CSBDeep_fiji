
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
import net.imglib2.type.numeric.real.FloatType;

public class GenericNetworksMismatchTest extends CSBDeepTest {

	@Test
	@Ignore
	public void test3DNetworkWith3DInputImage() {

		launchImageJ();

		String network = "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise3D/model.zip";
		Dataset input = createDataset(new FloatType(), new long[] { 10, 20, 1 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test(network, input);

	}

	@Test
	@Ignore
	public void test2DNetworkWith2DInputImage() {

		launchImageJ();

		String network = "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip";
		Dataset input = createDataset(new FloatType(), new long[] { 10, 20 }, new AxisType[] {
				Axes.X, Axes.Y });

		test(network, input);

	}

	@Test
	@Ignore
	public void test3DNetworkWith2DInputImage() {

		launchImageJ();

		String network = "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise3D/model.zip";
		Dataset input = createDataset(new FloatType(), new long[] { 10, 20 }, new AxisType[] {
				Axes.X, Axes.Y });

		test(network, input);

	}

	@Test
	@Ignore
	public void test2DNetworkWith3DInputImage() {

		launchImageJ();

		String network = "/home/random/Development/imagej/project/CSBDeep/tests/generic_test2/denoise2D/model.zip";
		Dataset input = createDataset(new FloatType(), new long[] { 10, 20, 30 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test(network, input);

	}

	private void test(String network, Dataset input) {
		final Future<CommandModule> future = ij.command().run(GenericNetwork.class,
				false,
				"input", input,
				"modelFile", new File(network),
				"overlap", 2);
		assertNotEquals(null, future);
		final Module module = ij.module().waitFor(future);
		List<Dataset> result = (List<Dataset>) module.getOutput("output");
		assertEquals(1, result.size());
		final Dataset output = result.get(0);
		testResultAxesAndSize(input, output);
	}

}


package de.csbdresden.csbdeep.commands;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.scijava.module.Module;

import de.csbdresden.csbdeep.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.real.FloatType;

public class GenericNetworksMismatchTest extends CSBDeepTest {

	@Test
	public void test3DNetworkWith3DInputImage() {

		launchImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 20, 1 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test("denoise3D/model.zip", input);

	}

	@Test
	public void test2DNetworkWith2DInputImage() {

		launchImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 10, 20 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test("denoise2D/model.zip", input);
		test("denoise2D/model.zip", input);

	}

	@Test
	public void test3DNetworkWith2DInputImage() {

		launchImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 20 }, new AxisType[] {
				Axes.X, Axes.Y });

		test("denoise3D/model.zip", input);

	}

	@Test
	public void test2DNetworkWith3DInputImage() {

		launchImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 20, 30 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test("denoise2D/model.zip", input);

	}

	private void test(String network, Dataset input) {
		URL networkUrl = this.getClass().getResource(network);
		try {
			final Module module = ij.command().run(GenericNetwork.class,
					false,
					"input", input,
					"modelFile", new File(networkUrl.getPath()),
					"overlap", 2).get();
			assertNotNull(module);
			final Dataset output = (Dataset) module.getOutput("output");
			assertNotNull(output);
			testResultAxesAndSize(input, output);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

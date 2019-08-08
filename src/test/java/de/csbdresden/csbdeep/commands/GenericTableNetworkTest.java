
package de.csbdresden.csbdeep.commands;

import static junit.framework.TestCase.assertNotNull;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.scijava.module.Module;
import org.scijava.table.GenericTable;

import de.csbdresden.csbdeep.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.real.FloatType;

public class GenericTableNetworkTest extends CSBDeepTest {

	@Test
	public void testGenericNetwork() {
		launchImageJ();

		URL networkUrl = this.getClass().getResource("denoise2D/model.zip");

		final Dataset input = createDataset(new FloatType(), new long[] { 3, 3, 3 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		try {
			final Module module = ij.command().run(GenericTableNetwork.class,
					false, "input", input, "modelFile", new File(networkUrl.getPath())).get();
			GenericTable output = (GenericTable) module.getOutput("output");
			assertNotNull(output);
			System.out.println(output);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

	}

}

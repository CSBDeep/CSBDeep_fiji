
package de.csbdresden.csbdeep.commands;

import static junit.framework.TestCase.assertNotNull;

import org.junit.Assert;
import org.junit.Test;

import de.csbdresden.csbdeep.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class NetTriboliumTest extends CSBDeepTest {

	@Test
	public void testNetTriboliumInvalidDataset() {
		launchImageJ();
		final Dataset input = createDataset(new FloatType(), new long[] {5, 10, 10, 2}, new AxisType[]
				{Axes.X, Axes.Y, Axes.TIME, Axes.CHANNEL});
		final Dataset result = runPlugin(NetTribolium.class, input);
		Assert.assertNull(result);
	}

	@Test
	public void testNetTriboliumXYZ() {
		 testDataset(new FloatType(), new long[] {5, 10, 2}, new AxisType[]
		 {Axes.X, Axes.Y, Axes.Z});
	}

	@Test
	public void testNetTriboliumXZY() {
		testDataset(new FloatType(), new long[] { 5, 2, 10 }, new AxisType[] {
				Axes.X, Axes.Z, Axes.Y });
	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes)
	{

		launchImageJ();
		final Dataset input = createDataset(type, dims, axes);
		final Dataset output = runPlugin(NetTribolium.class, input);
		assertNotNull(output);
		testResultAxesAndSize(input, output);
	}

}

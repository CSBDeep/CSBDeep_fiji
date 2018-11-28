
package org.csbdeep.commands;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.csbdeep.CSBDeepTest;
import org.junit.Test;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class NetTubulinTest extends CSBDeepTest {

	@Test
	public void testNetTubulinInvalidDataset() {
		launchImageJ();
		final Dataset input = createDataset(new FloatType(), new long[] {15, 5, 3, 3}, new AxisType[]
				{Axes.X, Axes.Y, Axes.Z, Axes.TIME});
		final Dataset result = runPlugin(NetTubulin.class, input);
		assertNull(result);
	}

	@Test
	public void testNetTubulinXY() {
		testDataset(new FloatType(), new long[] { 3, 4 }, new AxisType[] {
				Axes.X, Axes.Y });
	}

	@Test
	public void testNetTubulinXYZ() {
		testDataset(new FloatType(), new long[] { 3, 4, 5 }, new AxisType[] {
			Axes.X, Axes.Y, Axes.TIME });
	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes)
	{

		launchImageJ();
		final Dataset input = createDataset(type, dims, axes);
		final Dataset output = runPlugin(NetTubulin.class, input);
		assertNotNull(output);
		testResultAxesAndSize(input, output);
	}

}

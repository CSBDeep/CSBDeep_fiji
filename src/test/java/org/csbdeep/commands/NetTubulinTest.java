
package org.csbdeep.commands;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import org.csbdeep.CSBDeepTest;
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
		final Dataset input = createDataset(new FloatType(), new long[] {1, 1, 1, 1}, new AxisType[]
				{Axes.X, Axes.Y, Axes.Z, Axes.TIME});
		final List<Dataset> result = runPlugin(NetTubulin.class, input);
		assertEquals(0, result.size());
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
		final List<Dataset> result = runPlugin(NetTubulin.class, input);
		assertEquals(1, result.size());
		final Dataset output = result.get(0);
		testResultAxesAndSize(input, output);
	}

}

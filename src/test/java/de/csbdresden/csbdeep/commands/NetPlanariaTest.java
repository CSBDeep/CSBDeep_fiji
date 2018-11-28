
package de.csbdresden.csbdeep.commands;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import de.csbdresden.csbdeep.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;

public class NetPlanariaTest extends CSBDeepTest {

	@Test
	public void testPlanariaFloatTypeXY() {
		testDataset(new FloatType(), new long[] { 4, 3 }, new AxisType[] {
				Axes.X, Axes.Y });
	}

	@Test
	public void testPlanariaFloatTypeXYZ() {
		testDataset(new FloatType(), new long[] { 4, 3, 2 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });
	}

	@Test
	public void testPlanariaFloatTypeXYZC() {
		testDataset(new FloatType(), new long[] { 3, 4, 10, 1 }, new AxisType[] {
			Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL });
	}

	@Test
	public void testPlanariaInvalidDataset() throws InterruptedException {
		launchImageJ();
		final Dataset input = createDataset(new FloatType(), new long[] { 3, 4, 10, 2 }, new AxisType[]
				{ Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL });
		final Dataset result = runPlugin(NetPlanaria.class, input);
		assertNull(result);
	}

	@Test
	public void testPlanariaFloatTypeXZY() {
		testDataset(new FloatType(), new long[] { 5, 10, 1 }, new AxisType[] {
				Axes.X, Axes.Z, Axes.Y });
	}

	@Test
	public void testNetPlanariaByteType() {
		testDataset(new ByteType(), new long[] { 20, 10, 5 }, new AxisType[] {
			Axes.X, Axes.Y, Axes.Z });
	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes)
	{

		launchImageJ();
		final Dataset input = createDataset(type, dims, axes);
		final Dataset output = runPlugin(NetPlanaria.class, input);
		assertNotNull(output);
		testResultAxesAndSize(input, output);
	}

}

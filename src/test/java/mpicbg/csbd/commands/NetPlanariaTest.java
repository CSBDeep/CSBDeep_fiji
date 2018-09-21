
package mpicbg.csbd.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import mpicbg.csbd.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;

public class NetPlanariaTest extends CSBDeepTest {

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
		final List<Dataset> result = runPlugin(NetPlanaria.class, input);
		assertEquals(1, result.size());
		final Dataset output = result.get(0);
		testResultAxesAndSize(input, output);
	}

}

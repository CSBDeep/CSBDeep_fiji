
package mpicbg.csbd.commands;

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

public class NetProjectTest extends CSBDeepTest {

	// @Ignore
	@Test
	public void testNetProject_Float_XYZ() {
		testDataset(new FloatType(), new long[] { 100, 50, 10 }, new AxisType[] {
			Axes.X, Axes.Y, Axes.Z });
	}

	@Ignore
	@Test
	public void testNetProject_Float_ZXY() {
		testDataset(new FloatType(), new long[] { 10, 50, 100 }, new AxisType[] {
			Axes.Z, Axes.X, Axes.Y });
	}

	@Ignore
	@Test
	public void testNetProject_Float_XZY() {
		testDataset(new FloatType(), new long[] { 100, 10, 50 }, new AxisType[] {
			Axes.X, Axes.Z, Axes.Y });
	}

	@Ignore
	@Test
	public void testNetProject_Byte_XZY() {
		testDataset(new ByteType(), new long[] { 50, 10, 100 }, new AxisType[] {
			Axes.X, Axes.Z, Axes.Y });
	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes)
	{

		launchImageJ();
		final Dataset input = createDataset(type, dims, axes);
		final List<Dataset> result = runPlugin(NetProject.class, input);
		assertTrue("result should contain one dataset bis contains " + result
			.size(), result.size() == 1);
		final Dataset output = result.get(0);
		testResultAxesAndSizeByRemovingAxis(input, output, Axes.Z);
	}

}

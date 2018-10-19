
package org.csbdeep.commands;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import org.csbdeep.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;

public class NetProjectTest extends CSBDeepTest {

	@Test
	public void testNetProjectInvalidDatasetXY() {
		launchImageJ();
		final Dataset input = createDataset(new FloatType(), new long[] {5, 10}, new AxisType[]
				{Axes.X, Axes.Y});
		final List<Dataset> result = runPlugin(NetProject.class, input);
		assertEquals(0, result.size());
	}

	@Test
	public void testNetProjectInvalidDatasetXYZT() {
		launchImageJ();
		final Dataset input = createDataset(new FloatType(), new long[] {5, 10, 5, 10}, new AxisType[]
				{Axes.X, Axes.Y, Axes.Z, Axes.TIME});
		final List<Dataset> result = runPlugin(NetProject.class, input);
		assertEquals(0, result.size());
	}

	// @Ignore
	@Test
	public void testNetProject_Float_XYZ() {
		testDataset(new FloatType(), new long[] { 10, 5, 3 }, new AxisType[] {
			Axes.X, Axes.Y, Axes.Z });
	}

	@Ignore
	@Test
	public void testNetProject_Float_ZXY() {
		testDataset(new FloatType(), new long[] { 2, 5, 10 }, new AxisType[] {
			Axes.Z, Axes.X, Axes.Y });
	}

	@Ignore
	@Test
	public void testNetProject_Float_XZY() {
		testDataset(new FloatType(), new long[] { 10, 2, 5 }, new AxisType[] {
			Axes.X, Axes.Z, Axes.Y });
	}

	@Ignore
	@Test
	public void testNetProject_Byte_XZY() {
		testDataset(new ByteType(), new long[] { 5, 2, 10 }, new AxisType[] {
			Axes.X, Axes.Z, Axes.Y });
	}

	public <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
		final long[] dims, final AxisType[] axes)
	{

		launchImageJ();
		final Dataset input = createDataset(type, dims, axes);
		final List<Dataset> result = runPlugin(NetProject.class, input);
		assertEquals(1, result.size());
		final Dataset output = result.get(0);
		testResultAxesAndSizeByRemovingAxis(input, output, Axes.Z);
	}

}

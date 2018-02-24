package mpicbg.csbd.commands;

import static org.junit.Assert.assertTrue;

import java.util.List;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

import mpicbg.csbd.CSBDeepTest;

public class NetPlanariaTest extends CSBDeepTest {

	@Test
	public void testNetPlanaria() {
		testDataset(
				new FloatType(),
				new long[] { 50, 100, 10 },
				new AxisType[] { Axes.X, Axes.Y, Axes.Z } );
		testDataset(
				new FloatType(),
				new long[] { 50, 10, 100 },
				new AxisType[] { Axes.X, Axes.Z, Axes.Y } );
		testDataset(
				new ByteType(),
				new long[] { 100, 50, 10 },
				new AxisType[] { Axes.X, Axes.Y, Axes.Z } );
	}

	public < T extends RealType< T > & NativeType< T > > void
			testDataset( final T type, final long[] dims, final AxisType[] axes ) {

		launchImageJ();
		final Dataset input = createDataset( type, dims, axes );
		final DatasetView datasetView = wrapInDatasetView( input );
		final List< DatasetView > result = runPlugin( NetPlanaria.class, datasetView );
		assertTrue( "result should contain one dataset", result.size() == 1 );
		final Dataset output = result.get( 0 ).getData();
		testResultAxesAndSize( input, output );
	}

}

package mpicbg.csbd.commands;

import mpicbg.csbd.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class NetTriboliumTest extends CSBDeepTest {

	@Test
	public void testNetTribolium() {
//		testDataset(new FloatType(), new long[] {50, 100, 10}, new AxisType[] {Axes.X, Axes.Y, Axes.Z});
		testDataset(
				new FloatType(),
				new long[] { 50, 10, 100 },
				new AxisType[] { Axes.X, Axes.Z, Axes.Y } );
//		testDataset(new ByteType(), new long[] {100, 50, 10}, new AxisType[] {Axes.X, Axes.Y, Axes.Z});
	}

	public < T extends RealType< T > & NativeType< T > > void
			testDataset( final T type, final long[] dims, final AxisType[] axes ) {

		launchImageJ();
		final Dataset input = createDataset( type, dims, axes );
		final List< Dataset > result = runPlugin( NetTribolium.class, input );
		assertTrue( "result should contain one dataset", result.size() == 1 );
		final Dataset output = result.get( 0 );
		testResultAxesAndSize( input, output );
	}

}

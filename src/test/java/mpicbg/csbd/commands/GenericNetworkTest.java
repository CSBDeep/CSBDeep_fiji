package mpicbg.csbd.commands;

import mpicbg.csbd.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class GenericNetworkTest extends CSBDeepTest {

	@Test
	public void testGenericNetwork() throws InterruptedException {
		for(int i = 0; i < 1000; i++) {
			testDataset(
					new FloatType(),
					new long[] { 50, 50, 5 },
					new AxisType[] { Axes.X, Axes.Y, Axes.Z } );
			if(i % 10 == 0)
				System.out.println(i);
		}
	}

	public < T extends RealType< T > & NativeType< T > > void
			testDataset( final T type, final long[] dims, final AxisType[] axes ) {

		launchImageJ();
		final Dataset input = createDataset( type, dims, axes );
		final DatasetView datasetView = wrapInDatasetView( input );
		final List< DatasetView > result = runPlugin( GenericNetwork.class, datasetView );
		datasetView.dispose();
		assertTrue( "result should contain one dataset", result.size() == 1 );
		final Dataset output = result.get( 0 ).getData();
		testResultAxesAndSize( input, output );
		for(DatasetView obj : result) {
			obj.dispose();
		}
	}

}

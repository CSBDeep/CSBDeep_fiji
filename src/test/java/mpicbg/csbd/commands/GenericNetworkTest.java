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

		final Dataset input = createDataset( type, dims, axes );
		final Future< CommandModule > future = ij.command().run(
				GenericNetwork.class,
				false,
				"input",
				input,
				"modelFile",
				new File("/home/random/Development/imagej/project/CSBDeep/data/Tobias Boothe/models.zip"),
				"_modelName", "phago_C2_no_transform_model");
		assertFalse( "Plugin future is null", future == null );
		final Module module = ij.module().waitFor(future);
		List<Dataset> result = (List<Dataset>) module.getOutput("resultDatasets");
		assertEquals( 1, result.size() );
		final Dataset output = result.get( 0 );
		testResultAxesAndSize( input, output );
	}

}

package mpicbg.csbd.commands;

import mpicbg.csbd.CSBDeepTest;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GenericNetworkTest extends CSBDeepTest {

	@Test
	public void testGenericNetwork() throws InterruptedException {
		launchImageJ();
		for (int i = 0; i < 1; i++) {
//			Dataset dataset = ij.dataset().create(new FloatType(), new long[]{50, 50, 50}, "", new AxisType[]{Axes.X, Axes.Y, Axes.Z});
//			TiledView tiledView = new TiledView<FloatType>( ( RandomAccessibleInterval< FloatType > ) dataset.getImgPlus(), new long[]{32,32,32}, new long[]{10,10,10});
//			final RandomAccessibleInterval< FloatType > gridView =
//					new GridView<FloatType>( new ListImg<FloatType>( result, new long[]{1,1,1}) );

			testDataset(new FloatType(), new long[]{10, 10, 10}, new AxisType[]{Axes.X, Axes.Y, Axes.Z});
			testDataset(new UnsignedIntType(), new long[]{10, 10, 10}, new AxisType[]{Axes.X, Axes.Y, Axes.Z});

			if (i % 10 == 0)
				System.out.println(i);
		}

//			testDataset(new FloatType(), new long[]{10, 10, 10}, new AxisType[]{Axes.X, Axes.Y, Axes.Z});

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
		List<Dataset> result = (List<Dataset>) module.getOutput("output");
		assertEquals( 1, result.size() );
		final Dataset output = result.get( 0 );
		testResultAxesAndSize( input, output );
	}

}

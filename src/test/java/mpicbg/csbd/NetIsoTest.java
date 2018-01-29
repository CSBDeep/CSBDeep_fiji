package mpicbg.csbd;

import static org.junit.Assert.*;

import java.util.List;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

import mpicbg.csbd.commands.NetIso;


public class NetIsoTest extends CSBDeepTest {
	
	@Test
	public void testNetProject() {
		testDataset(
				new FloatType(), 
				new long[] {30, 40, 2, 5}, 
				new AxisType[] {Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z});
	}
	
	public <T extends RealType<T> & NativeType<T>> void testDataset(T type, long[] dims, AxisType[] axes) {

		launchImageJ();
		final Dataset input = createDataset(type, dims, axes);
		final DatasetView datasetView = wrapInDatasetView( input );
		final List<DatasetView> result = runPlugin(NetIso.class, datasetView);
		assertTrue("NetIso result should contain one dataset", result.size() == 1);
		final Dataset output = result.get( 0 ).getData();
		testResultAxesAndSize(input, output);
	}
	
	@Override
	protected void testResultAxesAndSize(Dataset input, Dataset output) {
		printDim("input", input);
		printDim("output", output);
		for(int i = 0; i < input.numDimensions(); i++){
			if(input.axis( i ).type() == Axes.Z){
				assertTrue( "Z axis dimension size output should be greater than input size ", 
						output.dimension( i ) > input.dimension( i ));	
			}else {
				assertEquals( output.dimension( i ), input.dimension( i ));
			}
			assertEquals( output.axis( i ).type(), input.axis( i ).type());
		}
	}

}

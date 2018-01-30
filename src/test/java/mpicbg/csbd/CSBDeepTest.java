package mpicbg.csbd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imagej.display.DefaultDatasetView;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.command.Command;
import org.scijava.command.CommandModule;

public abstract class CSBDeepTest {
	
	private ImageJ ij;
	
	protected void launchImageJ() {
		ij = new ImageJ();
		
		// TODO figure out how to run test headless
//		ij.ui().setHeadless( true );

		ij.launch();
	}
	
	protected <T extends RealType<T> & NativeType<T>> Dataset createDataset(T type, long[] dims, AxisType[] axes) {
		return ij.dataset().create(type, dims, "", axes);
	}
	
	protected DatasetView wrapInDatasetView(Dataset dataset) {
		DefaultDatasetView datasetView = new DefaultDatasetView();
		datasetView.setContext( dataset.getContext() );
		datasetView.initialize( dataset );
		datasetView.rebuild();
		return datasetView;
	}
	
	protected <C extends Command> List< DatasetView > runPlugin( Class<C> pluginClass, DatasetView datasetView ) {
		List<DatasetView> result = new ArrayList<>();
		Future< CommandModule > future = ij.command().run( pluginClass, true, 
				"datasetView", datasetView, 
				"resultDatasets", result );
		assertFalse("Plugin future is null",future == null);
		try {
			future.get();
		} catch ( InterruptedException | ExecutionException exc ) {
			exc.printStackTrace();
			fail();
		}
		return result;
	}
	
	protected void testResultAxesAndSize(Dataset input, Dataset output) {
		printDim("input", input);
		printDim("output", output);
		for(int i = 0; i < input.numDimensions(); i++){
			assertEquals( output.axis( i ).type(), input.axis( i ).type());
			assertEquals( output.dimension( i ), input.dimension( i ));
		}
	}
	
	protected void testResultAxesAndSizeByRemovingZ(Dataset input, Dataset output) {
		printDim("input", input);
		printDim("output", output);
		int i_output = 0;
		for(int i = 0; i < input.numDimensions(); i++){
			AxisType axis = input.axis( i ).type();
			if(axis == Axes.Z) continue;
			assertEquals( axis, output.axis( i_output ).type());
			assertEquals( input.dimension( i ), output.dimension( i_output ));
			i_output++;
		}
	}
	
	protected static void printDim( final String title, final Dataset input ) {
		final long[] dims = new long[ input.numDimensions() ];
		input.dimensions( dims );
		System.out.println( title + ": " + Arrays.toString( dims ) );
	}

}

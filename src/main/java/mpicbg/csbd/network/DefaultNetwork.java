package mpicbg.csbd.network;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.io.location.Location;

import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.util.IOHelper;
import mpicbg.csbd.util.Task;

public abstract class DefaultNetwork implements Network {

	protected Task status;
	protected ImageTensor inputNode = new ImageTensor();
	protected ImageTensor outputNode = new ImageTensor();
	protected TiledView< FloatType > tiledView;
	protected boolean supportsGPU = false;
//	protected CSBDeepProgress progressWindow;
	protected Integer doneTileCount;
	protected boolean dropSingletonDims = true;
	protected boolean doDimensionReduction = false;
	protected AxisType axisToRemove;
	ExecutorService pool = Executors.newSingleThreadExecutor();

	@Override
	public void loadLibrary() {}

	protected abstract boolean loadModel( Location source, String modelName );

	@Override
	public boolean loadModel(
			final String pathOrURL,
			final String modelName ) throws FileNotFoundException {

		final Location source = IOHelper.loadFileOrURL( pathOrURL );
		return loadModel( source, modelName );

	}

	@Override
	public abstract void preprocess();

	@Override
	public List< RandomAccessibleInterval< FloatType > > call() throws ExecutionException {

		final List< RandomAccessibleInterval< FloatType > > results = runModel();

		return results;
	}

	protected static void
			printDim( final String title, final RandomAccessibleInterval< FloatType > input ) {
		final long[] dims = new long[ input.numDimensions() ];
		input.dimensions( dims );
		System.out.println( title + ": " + Arrays.toString( dims ) );
	}

	private List< RandomAccessibleInterval< FloatType > > runModel() throws ExecutionException {

//		progressWindow.setStepStart( CSBDeepProgress.STEP_RUNMODEL );

		final boolean multithreading = false;

		final Cursor< RandomAccessibleInterval< FloatType > > cursor =
				Views.iterable( tiledView ).cursor();

		// Loop over the tiles and execute the prediction
		final List< RandomAccessibleInterval< FloatType > > results = new ArrayList<>();
		final List< Future< RandomAccessibleInterval< FloatType > > > futures = new ArrayList<>();

//		progressWindow.setProgressBarValue( 0 );
		doneTileCount = 0;

		while ( cursor.hasNext() ) {
			final RandomAccessibleInterval< FloatType > tile = cursor.next();
			printDim( "tile", tile );
			//uiService.show(tile);
			final Future< RandomAccessibleInterval< FloatType > > future =
					pool.submit( new TileRunner( tile ) );

			log( "Processing tile " + ( doneTileCount + 1 ) + ".." );

			futures.add( future );

			if ( !multithreading ) {
				try {
					final RandomAccessibleInterval< FloatType > res = future.get();
					if ( res == null ) return null;
					results.add( res );
					upTileCount();
				} catch ( final InterruptedException exc ) {
					pool.shutdownNow();
					fail();
					return null;
				}
			}
		}
		if ( multithreading ) {
			for ( final Future< RandomAccessibleInterval< FloatType > > future : futures ) {
				try {
					final RandomAccessibleInterval< FloatType > res = future.get();
					if ( res == null ) return null;
					results.add( res );
					upTileCount();
				} catch ( final InterruptedException exc ) {
					pool.shutdownNow();
					fail();
					return null;
				}
			}
		}

//		progressWindow.setCurrentStepDone();
		return results;
	}

	@Override
	public abstract RandomAccessibleInterval< FloatType >
			execute( RandomAccessibleInterval< FloatType > tile ) throws Exception;

	@Override
	public Task getStatus() {
		return status;
	}

	@Override
	public ImageTensor getInputNode() {
		return inputNode;
	}

	@Override
	public ImageTensor getOutputNode() {
		return outputNode;
	}

	@Override
	public boolean isSupportingGPU() {
		return supportsGPU;
	}

	@Override
	public void loadInputNode( final String defaultName, final Dataset dataset ) {
		inputNode.initialize( dataset );
		inputNode.setName( defaultName );
	}

	@Override
	public void loadOutputNode( final String defaultName ) {
		outputNode.setName( defaultName );
	}

	@Override
	public abstract void initMapping();

	@Override
	public abstract boolean isInitialized();

//	public void setProgressWindow(CSBDeepProgress window) {
//		this.progressWindow = window;
//	}

	protected void upTileCount() {
		doneTileCount++;
//		progressWindow.setProgressBarValue( doneTileCount );
	}

	@Override
	public void setTiledView( final TiledView< FloatType > tiledView ) {
		this.tiledView = tiledView;
	}

	void log( final String text ) {
//		progressWindow.addLog( text );
	}

	void fail() {
		status.setFailed();
//		progressWindow.setCurrentStepFail();
	}

	@Override
	public void cancel() {
		pool.shutdownNow();
	}

	/**
	 * Set if singleton dimensions of the output image should be dropped. If the
	 * tile size in one dimension is only one this could remove an important
	 * dimension. Default value is true.
	 */
	@Override
	public void setDropSingletonDims( final boolean dropSingletonDims ) {
		this.dropSingletonDims = dropSingletonDims;
	}

	@Override
	public void setDoDimensionReduction( final boolean doDimensionReduction ) {
		setDoDimensionReduction( doDimensionReduction, Axes.Z );
	}

	@Override
	public void setDoDimensionReduction(
			final boolean doDimensionReduction,
			final AxisType axisToRemove ) {
		this.doDimensionReduction = doDimensionReduction;
		this.axisToRemove = axisToRemove;
	}

	@Override
	public void close() {
		if ( pool != null ) {
			pool.shutdown();
		}
	}

	class TileRunner implements Callable< RandomAccessibleInterval< FloatType > > {

		RandomAccessibleInterval< FloatType > tile;

		public TileRunner( final RandomAccessibleInterval< FloatType > tile ) {
			this.tile = tile;
		}

		@Override
		public RandomAccessibleInterval< FloatType > call() throws Exception {
			final RandomAccessibleInterval< FloatType > result = execute( tile );
//			ImageJ ij = new ImageJ();
//			ij.ui().show( result );
			return result;
		}
	}
}

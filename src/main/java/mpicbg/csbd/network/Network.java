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
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.io.location.Location;

import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.ui.CSBDeepProgress;
import mpicbg.csbd.util.IOHelper;
import mpicbg.csbd.util.Task;

public abstract class Network implements Callable< List< RandomAccessibleInterval< FloatType > > > {
	
	protected Task status;
	protected ImageNode inputNode = new ImageNode();
	protected ImageNode outputNode = new ImageNode();
	protected TiledView< FloatType > tiledView;
	protected boolean supportsGPU = false;
	protected CSBDeepProgress progressWindow;
	protected Integer doneTileCount;
	protected boolean dropSingletonDims = true;
	ExecutorService pool = Executors.newSingleThreadExecutor();
	
	public void loadLibrary() {}
	
	protected abstract boolean loadModel( Location source, String modelName );
	
	public boolean loadModel( String pathOrURL, String modelName ) throws FileNotFoundException {

		Location source = IOHelper.loadFileOrURL( pathOrURL );
		return loadModel( source, modelName );
		
	}

	public abstract void preprocess();
	
	@Override
	public List< RandomAccessibleInterval< FloatType > > call() throws ExecutionException {

		final List< RandomAccessibleInterval< FloatType > > results = runModel();

		return results;
	}
	

	public List< RandomAccessibleInterval< FloatType > > runModel() throws ExecutionException {

		progressWindow.setStepStart( CSBDeepProgress.STEP_RUNMODEL );

		final boolean multithreading = false;

		final Cursor< RandomAccessibleInterval< FloatType > > cursor =
				Views.iterable( tiledView ).cursor();

		// Loop over the tiles and execute the prediction
		final List< RandomAccessibleInterval< FloatType > > results = new ArrayList<>();
		final List< Future< RandomAccessibleInterval< FloatType > > > futures = new ArrayList<>();

		progressWindow.setProgressBarValue( 0 );
		doneTileCount = 0;

		while ( cursor.hasNext() ) {
			final RandomAccessibleInterval< FloatType > tile = cursor.next();
			//uiService.show(tile);
			final Future< RandomAccessibleInterval< FloatType > > future = pool.submit(
					new TileRunner( tile ) );

			log("Processing tile " + ( doneTileCount + 1 ) + ".." );

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

		progressWindow.setCurrentStepDone();
		return results;
	}

	
	public abstract RandomAccessibleInterval< FloatType > execute(RandomAccessibleInterval< FloatType > tile) throws Exception;
	
	public Task getStatus() {
		return status;
	}
	
	public ImageNode getInputNode() {
		return inputNode;
	}

	public ImageNode getOutputNode() {
		return outputNode;
	}
	
	public boolean isSupportingGPU() {
		return supportsGPU;
	}

	public void loadInputNode( String defaultName, Dataset dataset ) {
		inputNode.initialize( dataset );
		inputNode.setName( defaultName );
	}

	public void loadOutputNode( String defaultName ) {
		outputNode.setName( defaultName );
	}

	public abstract void initMapping();

	public abstract boolean isInitialized();
	
	public void setProgressWindow(CSBDeepProgress window) {
		this.progressWindow = window;
	}
	
	protected void upTileCount() {
		doneTileCount++;
		progressWindow.setProgressBarValue( doneTileCount );
	}
	
	public void setTiledView(TiledView< FloatType > tiledView) {
		this.tiledView = tiledView;
	}
	
	void log(String text) {
		progressWindow.addLog( text );
	}
	
	void fail() {
		status.setFailed(true);
		progressWindow.setCurrentStepFail();
	}
	
	public void cancel() {
		pool.shutdownNow();
	}
	
	/**
	 * Set if singleton dimensions of the output image should be dropped. If the
	 * tile size in one dimension is only one this could remove an important
	 * dimension. Default value is true.
	 */
	public void setDropSingletonDims( final boolean dropSingletonDims ) {
		this.dropSingletonDims = dropSingletonDims;
	}
	
	class TileRunner implements Callable< RandomAccessibleInterval< FloatType > > {

		RandomAccessibleInterval< FloatType > tile;

		public TileRunner(final RandomAccessibleInterval< FloatType > tile) {
			this.tile = tile;
		}

		@Override
		public RandomAccessibleInterval< FloatType > call() throws Exception {
			RandomAccessibleInterval< FloatType > result = execute( tile );
			if ( result != null ) {
				
				removePadding(result);

//				ImageJ ij = new ImageJ();
//				ij.ui().show( result );

			}
			else {
				throw new java.lang.RuntimeException("Network tile result is null");
			}
			return result;

		}

		protected void removePadding( RandomAccessibleInterval< FloatType > result ) {
			int largestDim = getOutputNode().getLargestDimIndex();
			long[] padding = getOutputNode().getNodePadding();

			// Set padding to negative to remove it later
			final long[] negPadding = padding.clone();				
			negPadding[ largestDim ] = -padding[ largestDim ];
			
			long[] dims = new long[result.numDimensions()];
			result.dimensions( dims );
			System.out.println( "result from network: " + Arrays.toString( dims ) );

			final long[] negPaddingPlus = new long[ result.numDimensions() ];
			for ( int i = 0; i < negPadding.length; i++ ) {
				negPaddingPlus[ i ] = negPadding[ i ];
			}
			result = Views.zeroMin( Views.expandZero( result, negPaddingPlus ) );
			
		}

	}
}

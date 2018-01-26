/*-
 * #%L
 * CSBDeep Fiji Plugin: Use deep neural networks for image restoration for fluorescence microscopy.
 * %%
 * Copyright (C) 2017 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package mpicbg.csbd.prediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import mpicbg.csbd.imglib2.ArrangedView;
import mpicbg.csbd.imglib2.CombinedView;
import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.network.Network;
import mpicbg.csbd.ui.CSBDeepProgress;
import mpicbg.csbd.util.Task;

public class TiledPrediction
		implements
		Callable< List< RandomAccessibleInterval< FloatType > > > {

	protected RandomAccessibleInterval< FloatType > input;
	
	protected Network network;

	protected int tilesNum;
	protected int blockMultiple;
	protected long blockWidth;
	protected int overlap;
	protected long largestSize;

	protected final CSBDeepProgress progressWindow;

	protected Integer doneTileCount;

	protected boolean cancelPressed = false;
	
	protected Task status;

	/**
	 * If singleton dimensions in the output should be dropped. Default: true
	 */
	protected boolean dropSingletonDims = true;

	ExecutorService pool = Executors.newSingleThreadExecutor();

	public TiledPrediction(
			final RandomAccessibleInterval< FloatType > input,
			final Network network,
			final CSBDeepProgress progressWindow,
			final int tilesNum,
			final int blockMultiple,
			final int overlap ) {

		this.progressWindow = progressWindow;

		this.input = input;
		this.tilesNum = tilesNum;
		this.blockMultiple = blockMultiple;
		this.overlap = overlap;

		this.network = network;

	}
	
	void log(String text) {
		progressWindow.addLog( text );
	}
	
	void fail() {
		status.setFailed(true);
		progressWindow.setCurrentStepFail();
	}

	protected TiledView< FloatType > preprocess() {

		if ( input != null ) {

			final long[] dims = new long[ input.numDimensions() ];
			input.dimensions( dims );
			log( "Image dimensions: " + Arrays.toString( dims ) );
			log( "Calculate mapping between image and tensor.." );
 
			largestSize = network.getInputNode().getLargestDimSize();

			initializePadding();
			network.preprocess();

			log( "Divide image into " + tilesNum + " tile(s).." );

			progressWindow.setProgressBarMax( tilesNum );

			blockWidth = calculateBlockWidth();
			tilesNum = calculateRealNumTiles();
			RandomAccessibleInterval< FloatType > expandedInput = expandToFitBlockSize(input);
			long[] tileSize = calculateTileSize(expandedInput);
			final TiledView< FloatType > tiledView = createTiledView(expandedInput, tileSize);

			progressWindow.setCurrentStepDone();

			return tiledView;

		}

		return null;

	}
	
	public void initializePadding() {
		long[] padding = new long[ input.numDimensions() ];
		int largestDim = network.getInputNode().getLargestDimIndex();
		padding[ largestDim ] = overlap;
		network.getInputNode().setNodePadding( padding );
		network.getOutputNode().setNodePadding( padding );
		System.out.println( "input padding: " + Arrays.toString( network.getInputNode().getNodePadding() ) );
		System.out.println( "output padding: " + Arrays.toString( network.getOutputNode().getNodePadding() ) );
	}
	
	protected long calculateBlockWidth() {
		final double blockwidthIdeal = largestSize / ( double ) tilesNum;
		final long blockwidth =
				( long ) ( Math.ceil( blockwidthIdeal / blockMultiple ) * blockMultiple );
		System.out.println( "blockwidthIdeal: " + blockwidthIdeal );
		System.out.println( "blockwidth: " + blockwidth );
		System.out.println( "blockMultiple: " + blockMultiple );
		return blockwidth;
	}
	
	protected int calculateRealNumTiles() {
		int numTiles = ( int ) Math.ceil( ( float ) largestSize / blockWidth );
		System.out.println( "nTiles: " + numTiles );
		return numTiles;
	}

	protected RandomAccessibleInterval< FloatType >
			expandToFitBlockSize( RandomAccessibleInterval< FloatType > dataset ) {
		int largestDim = network.getInputNode().getLargestDimIndex();
		RandomAccessibleInterval< FloatType > expandedInput = expandDimToSize( dataset, largestDim, blockWidth * tilesNum );
		// Expand other dimensions to fit blockMultiple
		for ( int i = 0; i < expandedInput.numDimensions(); i++ ) {
//			if ( bridge.getDimTypeByDatasetDim( i ).isXY() ) {
			expandedInput = i == largestDim ? expandedInput : expandDimToSize(
					expandedInput,
					i,
					( long ) Math.ceil(
							expandedInput.dimension(
									i ) / ( double ) blockMultiple ) * blockMultiple );
//			}
		}
		return expandedInput;
	}

	protected long[] calculateTileSize(RandomAccessibleInterval< FloatType > dataset) {
		final long[] tileSize = Intervals.dimensionsAsLongArray( dataset );
		int largestDim = network.getInputNode().getLargestDimIndex();
		tileSize[ largestDim ] = blockWidth;
		System.out.println( "tilesize: " + Arrays.toString( tileSize ) );
		return tileSize;
	}
	
	protected TiledView< FloatType > createTiledView(RandomAccessibleInterval< FloatType > dataset, long[] tileSize) {
		long[] padding = network.getInputNode().getNodePadding();
		return new TiledView<>( dataset, tileSize, padding );
	}

	public List< RandomAccessibleInterval< FloatType > > runModel( final TiledView< FloatType > tiledView ) throws ExecutionException {

		progressWindow.setStepStart( CSBDeepProgress.STEP_RUNMODEL );

		final boolean multithreading = false;

		final Cursor< RandomAccessibleInterval< FloatType > > cursor =
				Views.iterable( tiledView ).cursor();

		// Loop over the tiles and execute the prediction
		final List< RandomAccessibleInterval< FloatType > > results = new ArrayList<>();
		final List< Future< RandomAccessibleInterval< FloatType > > > futures = new ArrayList<>();

		progressWindow.setProgressBarValue( 0 );
		doneTileCount = 0;

		while ( cursor.hasNext() && !cancelPressed ) {
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

	protected void upTileCount() {
		doneTileCount++;
		progressWindow.setProgressBarValue( doneTileCount );
	}

	protected List< RandomAccessibleInterval< FloatType > > postprocess( final List< RandomAccessibleInterval< FloatType > > results ) {

		if ( results != null && results.size() > 0 ) {

			progressWindow.setStepStart( CSBDeepProgress.STEP_POSTPROCESSING );
			
			long[] dims = new long[results.get( 0 ).numDimensions()];
			results.get( 0 ).dimensions( dims );
			System.out.println( "result 0: " + Arrays.toString( dims ));

			log( "Merging tiles.." );

			final RandomAccessibleInterval< FloatType > result = arrangeAndCombineTiles(results);
			
			dims = new long[result.numDimensions()];
			result.dimensions( dims );
			System.out.println( "merge: " + Arrays.toString( dims ));

			log( "Crop to original size.." );

			RandomAccessibleInterval< FloatType > fittedResult = undoExpansion(result);

//			ImageJ ij = new ImageJ();
//			ij.ui().show( "result", result );
//			ij.ui().show( "fittedresult", fittedResult );
//			ij.ui().show( "_expandedresult", expandedresult );

			return splitByLastDim(fittedResult);
		}

		progressWindow.setCurrentStepFail();
		return new ArrayList<>();
	}

	protected RandomAccessibleInterval< FloatType >
			arrangeAndCombineTiles( List< RandomAccessibleInterval< FloatType > > results ) {
		// Arrange and combine the tiles again
		int largestDim = network.getOutputNode().getLargestDimIndex();
		final long[] grid = new long[ results.get( 0 ).numDimensions() ];
		for ( int i = 0; i < grid.length; i++ ) {
			grid[ i ] = i == largestDim ? tilesNum : 1;
		}
		final RandomAccessibleInterval< FloatType > result =
				new CombinedView<>( new ArrangedView<>( results, grid ) );
		return result;
	}
	
	protected RandomAccessibleInterval< FloatType >
	undoExpansion( RandomAccessibleInterval< FloatType > result ) {
        int largestDim = network.getOutputNode().getLargestDimIndex();
        RandomAccessibleInterval< FloatType > fittedResult = expandDimToSize( result, largestDim, largestSize );
        // undo Expand other dimensions to fit blockMultiple
        for ( int i = 0; i < network.getOutputNode().numDimensions(); i++ ) {
        	if ( i != largestDim ) {
        		fittedResult = expandDimToSize( fittedResult, i, network.getOutputNode().getDatasetDimension( i ) );
        	}
        }
        return fittedResult;
    }
	
	protected List< RandomAccessibleInterval< FloatType > >
			splitByLastDim( RandomAccessibleInterval< FloatType > fittedResult ) {
		final int lastdim = fittedResult.numDimensions() - 1;
		return splitChannels( fittedResult, lastdim );
	}

	@Override
	public List< RandomAccessibleInterval< FloatType > > call() throws ExecutionException {
		final TiledView< FloatType > tiledView = preprocess();

		progressWindow.setProgressBarValue( 0 );

		progressWindow.setStepStart( CSBDeepProgress.STEP_RUNMODEL );
		final List< RandomAccessibleInterval< FloatType > > results = runModel( tiledView );

		return postprocess( results );
	}

	public < T extends RealType< T > > List< RandomAccessibleInterval< FloatType > > splitChannels(
			final RandomAccessibleInterval< FloatType > img,
			final int channelDim ) {

		log( "Split result channels.." );

		final ArrayList< RandomAccessibleInterval< FloatType > > res = new ArrayList<>();

		for ( int i = 0; i < img.dimension( channelDim ); i++ ) {
			res.add( Views.zeroMin( Views.hyperSlice( img, channelDim, i ) ) );
		}

		return res;
	}

	protected static void replaceNegativesWithMissingIndices( final int[] arr ) {
		final List< Integer > indices = new ArrayList<>();
		for ( int i = 0; i < arr.length; i++ ) {
			indices.add( arr[ i ] );
		}
		for ( int i = 0; i < arr.length; i++ ) {
			if ( !indices.contains( i ) ) {
				for ( int j = 0; j < arr.length; j++ ) {
					if ( arr[ j ] == -1 ) {
						arr[ j ] = i;
						break;
					}
				}
			}
		}
	}

	protected static RandomAccessibleInterval< FloatType > expandDimToSize(
			final RandomAccessibleInterval< FloatType > im,
			final int d,
			final long size ) {
		final int n = im.numDimensions();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		im.min( min );
		im.max( max );
		max[ d ] += ( size - im.dimension( d ) );
		return Views.interval( Views.extendMirrorDouble( im ), new FinalInterval( min, max ) );
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
			RandomAccessibleInterval< FloatType > result = network.execute( tile, dropSingletonDims );
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
			int largestDim = network.getOutputNode().getLargestDimIndex();
			long[] padding = network.getOutputNode().getNodePadding();

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

	public void cancel() {
		pool.shutdownNow();
	}

}

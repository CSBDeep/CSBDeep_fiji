/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2018 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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
package mpicbg.csbd.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;

import mpicbg.csbd.imglib2.ArrangedView;
import mpicbg.csbd.imglib2.CombinedView;
import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.tensorflow.DatasetConverter;
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.tensorflow.TensorFlowRunner;
import mpicbg.csbd.ui.CSBDeepProgress;

public class TiledPrediction
		implements
		Callable< List< RandomAccessibleInterval< FloatType > > > {

	protected RandomAccessibleInterval< FloatType > input, expandedInput;
	protected SavedModelBundle model;
	protected DatasetTensorBridge bridge;

	protected int nTiles;
	protected int blockMultiple;
	protected int overlap;
	protected int largestDim;
	protected long largestSize;
	protected long[] padding;

	protected int[] mappingIn, mappingOut;

	protected final CSBDeepProgress progressWindow;

	protected Integer doneTileCount;

	protected boolean cancelPressed = false;

	/**
	 * If singleton dimensions in the output should be dropped. Default: true
	 */
	private boolean dropSingletonDims = true;

	ExecutorService pool = Executors.newSingleThreadExecutor();

	public TiledPrediction(
			final RandomAccessibleInterval< FloatType > input,
			final DatasetTensorBridge bridge,
			final SavedModelBundle model,
			final CSBDeepProgress progressWindow,
			final int nTiles,
			final int blockMultiple,
			final int overlap ) {

		this.progressWindow = progressWindow;

		this.input = input;
		this.bridge = bridge;
		this.model = model;
		this.nTiles = nTiles;
		this.blockMultiple = blockMultiple;
		this.overlap = overlap;

	}

	protected TiledView< FloatType > preprocess() {

		if ( input != null ) {

			final long[] dims = new long[ input.numDimensions() ];
			input.dimensions( dims );
			progressWindow.addLog( "Image dimensions: " + Arrays.toString( dims ) );
			progressWindow.addLog( "Calculate mapping between image and tensor.." );

			largestDim = getLargestDim( input, bridge );
			largestSize = input.dimension( largestDim );

			//get mapping for input tensor (index is input image dimension index, value is tensor index)
			mappingIn = new int[ bridge.getInputTensorInfo().getTensorShape().getDimCount() ];
			//get mapping for input tensor (index is input image dimension index, value is tensor index)
			mappingOut = new int[ bridge.getOutputTensorInfo().getTensorShape().getDimCount() ];
			calculateMapping( mappingIn, mappingOut, bridge );

			progressWindow.addLog( "mappingIn: " + Arrays.toString( mappingIn ) );
			progressWindow.addLog( "mappingOut: " + Arrays.toString( mappingOut ) );

			progressWindow.addLog( "Divide image into " + nTiles + " tile(s).." );

			padding = new long[ input.numDimensions() ];

			progressWindow.setProgressBarMax( nTiles );

			// Calculate the blocksize to use
			final double blockwidthIdeal = largestSize / ( double ) nTiles;
			final long blockwidth =
					( long ) ( Math.ceil( blockwidthIdeal / blockMultiple ) * blockMultiple );

			nTiles = ( int ) Math.ceil( ( float ) largestSize / blockwidth );

			System.out.println( "blockwidthIdeal: " + blockwidthIdeal );
			System.out.println( "blockwidth: " + blockwidth );
			System.out.println( "blockMultiple: " + blockMultiple );
			System.out.println( "nTiles: " + nTiles );

			// Expand the image to fit the blocksize
			expandedInput = expandDimToSize( input, largestDim, blockwidth * nTiles );

			// Expand other dimensions to fit blockMultiple
			for ( int i = 0; i < expandedInput.numDimensions(); i++ ) {
//				if ( bridge.getDimTypeByDatasetDim( i ).isXY() ) {
				expandedInput = i == largestDim ? expandedInput : expandDimToSize(
						expandedInput,
						i,
						( long ) Math.ceil(
								expandedInput.dimension(
										i ) / ( double ) blockMultiple ) * blockMultiple );
//				}
			}

			final long[] imdims = new long[ expandedInput.numDimensions() ];
			expandedInput.dimensions( imdims );
			System.out.println( "imdims: " + Arrays.toString( imdims ) );
//				printDim( "After expand", im );

			// Set the tile size
			final long[] tileSize = Intervals.dimensionsAsLongArray( expandedInput );
			tileSize[ largestDim ] = blockwidth;

			// Put the padding per dimension in a array

			padding[ largestDim ] = overlap;

			System.out.println( "tilesize: " + Arrays.toString( tileSize ) );

			// Create the tiled view
			final TiledView< FloatType > tiledView = new TiledView<>( expandedInput, tileSize, padding );

			progressWindow.setCurrentStepDone();

			return tiledView;

		}

		return null;

	}

	public List< RandomAccessibleInterval< FloatType > > runModel( final TiledView< FloatType > tiledView ) throws ExecutionException {

		progressWindow.setStepStart( CSBDeepProgress.STEP_RUNMODEL );

		final boolean multithreading = false;

		// Set padding to negative to remove it later
		final long[] negPadding = padding.clone();
		negPadding[ largestDim ] = -padding[ largestDim ];

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
					new TileRunner( tile, negPadding, progressWindow ) );

			progressWindow.addLog(
					"Processing tile " + ( doneTileCount + 1 ) + ".." );

			futures.add( future );

			if ( !multithreading ) {
				try {
					final RandomAccessibleInterval< FloatType > res = future.get();
					if ( res == null ) return null;
					results.add( res );
					upTileCount();
				} catch ( final InterruptedException exc ) {
					pool.shutdownNow();
					progressWindow.setCurrentStepFail();
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
					progressWindow.setCurrentStepFail();
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

			progressWindow.addLog( "Merging tiles.." );

			// Arrange and combine the tiles again
			final long[] grid = new long[ results.get( 0 ).numDimensions() ];
			for ( int i = 0; i < grid.length; i++ ) {
				grid[ i ] = i == largestDim ? nTiles : 1;
			}
			final RandomAccessibleInterval< FloatType > result =
					new CombinedView<>( new ArrangedView<>( results, grid ) );

			progressWindow.addLog( "Crop to original size.." );

			RandomAccessibleInterval< FloatType > fittedResult =
					expandDimToSize( result, largestDim, largestSize );

			// undo Expand other dimensions to fit blockMultiple
			for ( int i = 0; i < input.numDimensions(); i++ ) {
				if ( i != largestDim ) {
					fittedResult = expandDimToSize( fittedResult, i, input.dimension( i ) );
				}
			}

//			ImageJ ij = new ImageJ();
//			ij.ui().show( "_result", result );
//			ij.ui().show( "_expandedresult", expandedresult );

			final int lastdim = fittedResult.numDimensions() - 1;

			if ( fittedResult.dimension(
					lastdim ) > 0 ) { return splitChannels( fittedResult, lastdim ); }

			progressWindow.setCurrentStepFail();
			return null;
		}

		progressWindow.setCurrentStepFail();
		return new ArrayList<>();
	}

	@Override
	public List< RandomAccessibleInterval< FloatType > > call() throws ExecutionException {
		final TiledView< FloatType > tiledView = preprocess();

		progressWindow.setProgressBarValue( 0 );

		progressWindow.setStepStart( CSBDeepProgress.STEP_RUNMODEL );
		final List< RandomAccessibleInterval< FloatType > > results = runModel( tiledView );

		return postprocess( results );
	}

	protected static < T extends RealType< T > > int getLargestDim( final RandomAccessibleInterval< T > input, final DatasetTensorBridge bridge ) {
		// Get the largest dimension and its size
		int largestDim = 0;
		long largestSize = 0;
		for ( int d = 0; d < input.numDimensions(); d++ ) {
			final long dimSize = input.dimension( d );
			if ( bridge.getDimTypeByDatasetDim( d ).isXY() && dimSize > largestSize ) {
				largestSize = dimSize;
				largestDim = d;
			}
		}
		return largestDim;
	}

	protected static void calculateMapping( final int[] mappingIn, final int[] mappingOut, final DatasetTensorBridge bridge ) {

		for ( int i = 0; i < mappingIn.length; i++ ) {
			mappingIn[ i ] = bridge.getTfIndexByDatasetDim( i );
		}
		replaceNegativesWithMissingIndices( mappingIn );
		System.out.println( "mapping in: " + Arrays.toString( mappingIn ) );

		//check if network reduces dimension, if yes, remote Z from mapping
		bridge.handleDimensionReduction();

		final AxisType[] mappingOutDimType = new AxisType[ mappingOut.length ];
		for ( int i = 0; i < mappingOut.length; i++ ) {
			mappingOut[ i ] =
					bridge.getTfIndexByDatasetDim( i );
			mappingOutDimType[ i ] =
					bridge.getDimTypeByDatasetDim( i );
		}
		replaceNegativesWithMissingIndices( mappingOut );
		System.out.println( "mapping out: " + Arrays.toString( mappingOut ) );
	}

	public < T extends RealType< T > > List< RandomAccessibleInterval< FloatType > > splitChannels(
			final RandomAccessibleInterval< FloatType > img,
			final int channelDim ) {

		progressWindow.addLog( "Split result channels.." );

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

	protected RandomAccessibleInterval< FloatType > executeGraphWithPadding( final RandomAccessibleInterval< FloatType > tile ) throws Exception {

		final Tensor inputTensor = DatasetConverter.datasetToTensor( tile, mappingIn );
		if ( inputTensor != null ) {
			Tensor outputTensor = null;
			outputTensor = TensorFlowRunner.executeGraph(
					model,
					inputTensor,
					bridge.getInputTensorInfo(),
					bridge.getOutputTensorInfo() );

			if ( outputTensor != null ) { return DatasetConverter.tensorToDataset(
					outputTensor,
					mappingOut,
					dropSingletonDims ); }
		}
		return null;
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
		CSBDeepProgress progressWindow;
		long[] negPadding;

		public TileRunner(
				final RandomAccessibleInterval< FloatType > tile,
				final long[] negPadding,
				final CSBDeepProgress progressWindow ) {
			this.tile = tile;
			this.negPadding = negPadding;
			this.progressWindow = progressWindow;
		}

		@Override
		public RandomAccessibleInterval< FloatType > call() throws Exception {
			RandomAccessibleInterval< FloatType > result =
					executeGraphWithPadding( tile );
			if ( result != null ) {

				final long[] negPaddingPlus = new long[ result.numDimensions() ];
				for ( int i = 0; i < negPadding.length; i++ ) {
					negPaddingPlus[ i ] = negPadding[ i ];
				}
				result = Views.zeroMin( Views.expandZero( result, negPaddingPlus ) );

//						ImageJ ij = new ImageJ();
//						ij.ui().show( tileExecuted );

			}
			return result;

		}

	}

	public void cancel() {
		pool.shutdownNow();
	}

}

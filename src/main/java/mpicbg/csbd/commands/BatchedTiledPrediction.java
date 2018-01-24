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
import java.util.concurrent.ExecutionException;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

import org.tensorflow.SavedModelBundle;

import mpicbg.csbd.imglib2.ArrangedView;
import mpicbg.csbd.imglib2.CombinedView;
import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.ui.CSBDeepProgress;

public class BatchedTiledPrediction extends TiledPrediction {

	protected int nBatches;
	protected int batchSize;
	protected int batchDim;
	protected int channelDim;
	protected long batchDimSize;

	public BatchedTiledPrediction(
			final RandomAccessibleInterval< FloatType > input,
			final DatasetTensorBridge bridge,
			final SavedModelBundle model,
			final CSBDeepProgress progressWindow,
			final int nTiles,
			final int blockMultiple,
			final int overlap,
			final int batchSize ) {
		super( input, bridge, model, progressWindow, nTiles, blockMultiple, overlap );
		this.batchSize = batchSize;
	}

	@Override
	public List< RandomAccessibleInterval< FloatType > > call() throws ExecutionException {

		bridge.printMapping();
		batchDim = bridge.getDatasetDimIndexByTFIndex( 0 );
		channelDim = bridge.getDatasetDimIndexByTFIndex(
				bridge.getInputTensorInfo().getTensorShape().getDimCount() - 1 );
		// If there is no channel dimension in the input image, we assume that a channel dimension might be added to the end of the image
		if ( channelDim < 0 ) {
			channelDim = input.numDimensions();
		}
		final TiledView< FloatType > tiledView = preprocess();
		System.out.println( "batchDim  : " + batchDim );
		System.out.println( "channelDim: " + channelDim );
		System.out.println( "largestDim: " + largestDim );
		final long[] tileSize = tiledView.getBlockSize();
		batchDimSize = tileSize[ batchDim ];
		System.out.println( "batchDimSize  : " + batchDimSize );
		nBatches = ( int ) Math.ceil( ( float ) batchDimSize / ( float ) batchSize );
		// If a smaller batch size is sufficient for the same amount of batches, we can use it
		batchSize = ( int ) Math.ceil( ( float ) batchDimSize / ( float ) nBatches );

		progressWindow.setProgressBarMax( nTiles * nBatches );

		final long expandedBatchDimSize = nBatches * batchSize;
		tileSize[ batchDim ] = batchSize;
		final RandomAccessibleInterval< FloatType > expandedInput2 =
				expandDimToSize( expandedInput, batchDim, expandedBatchDimSize );
		final TiledView< FloatType > tiledView2 =
				new TiledView<>( expandedInput2, tileSize, padding );

		final List< RandomAccessibleInterval< FloatType > > results = runModel( tiledView2 );

//			final ImageJ ij = new ImageJ();
//			int i = 0;
//			for ( RandomAccessibleInterval< FloatType > res : results ) {
//				ij.ui().show( "res" + i, res );
//				i++;
//			}

		return postprocess( results );
	}

	@Override
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

			// Calculate the blocksize to use
			final double blockwidthIdeal = largestSize / ( double ) nTiles;
			final long blockwidth =
					( long ) ( Math.ceil( blockwidthIdeal / blockMultiple ) * blockMultiple );

			// Expand the image to fit the blocksize
			expandedInput = expandDimToSize( input, largestDim, blockwidth * nTiles );

			long[] imdims = new long[ expandedInput.numDimensions() ];
			expandedInput.dimensions( imdims );
			System.out.println( "imdims1: " + Arrays.toString( imdims ) );

			// Expand other dimensions to fit blockMultiple
			for ( int i = 0; i < expandedInput.numDimensions(); i++ ) {
				if ( i != largestDim && i != batchDim && i != channelDim ) {
					expandedInput = expandDimToSize(
							expandedInput,
							i,
							( long ) Math.ceil(
									expandedInput.dimension(
											i ) / ( double ) blockMultiple ) * blockMultiple );
				}
			}

			imdims = new long[ expandedInput.numDimensions() ];
			expandedInput.dimensions( imdims );
			System.out.println( "imdims2: " + Arrays.toString( imdims ) );
//				printDim( "After expand", im );

			// Set the tile size
			final long[] tileSize = Intervals.dimensionsAsLongArray( expandedInput );
			tileSize[ largestDim ] = blockwidth;

			// Put the padding per dimension in an array

			padding[ largestDim ] = overlap;

			System.out.println( "tilesize: " + Arrays.toString( tileSize ) );

			// Create the tiled view
			final TiledView< FloatType > tiledView = new TiledView<>( expandedInput, tileSize, padding );

			progressWindow.setCurrentStepDone();

			return tiledView;

		}

		return null;

	}

	@Override
	protected List< RandomAccessibleInterval< FloatType > > postprocess( final List< RandomAccessibleInterval< FloatType > > results ) {

		if ( results != null && results.size() > 0 ) {

			progressWindow.setStepStart( CSBDeepProgress.STEP_POSTPROCESSING );

			progressWindow.addLog( "Merging tiles.." );

			// Arrange and combine the tiles again
			final long[] grid = new long[ results.get( 0 ).numDimensions() ];
			for ( int i = 0; i < grid.length; i++ ) {
				if ( i == largestDim ) {
					grid[ i ] = nTiles;
					continue;
				}
				if ( i == batchDim ) {
					grid[ i ] = nBatches;
					continue;
				}
				grid[ i ] = 1;
			}
			final long[] res0Dimension = new long[ results.get( 0 ).numDimensions() ];
			results.get( 0 ).dimensions( res0Dimension );
			System.out.println( "res0 dimensions: " + Arrays.toString( res0Dimension ) );
			System.out.println( "nBatches: " + nBatches );
			System.out.println( "nTiles: " + nTiles );
			System.out.println( "grid: " + Arrays.toString( grid ) );
			final RandomAccessibleInterval< FloatType > result =
					new CombinedView<>( new ArrangedView<>( results, grid ) );

			final long[] resDimension = new long[ result.numDimensions() ];
			result.dimensions( resDimension );
			System.out.println( "result dimensions: " + Arrays.toString( resDimension ) );

			progressWindow.addLog( "Crop to original size.." );

			RandomAccessibleInterval< FloatType > fittedResult =
					expandDimToSize( result, largestDim, largestSize );

			fittedResult.dimensions( resDimension );
			System.out.println( "fittedResult dimensions: " + Arrays.toString( resDimension ) );

			fittedResult = expandDimToSize( fittedResult, batchDim, batchDimSize );

			fittedResult.dimensions( resDimension );
			System.out.println( "fittedResult2 dimensions: " + Arrays.toString( resDimension ) );

			// undo Expand other dimensions to fit blockMultiple
			for ( int i = 0; i < input.numDimensions(); i++ ) {
				if ( i != largestDim && i != batchDim && i != channelDim ) {
					fittedResult = expandDimToSize( fittedResult, i, input.dimension( i ) );
				}
			}

			fittedResult.dimensions( resDimension );
			System.out.println( "fittedResult3 dimensions: " + Arrays.toString( resDimension ) );

			if ( channelDim >= 0 && channelDim < fittedResult.numDimensions() ) { return splitChannels( fittedResult, channelDim ); }

			progressWindow.setCurrentStepFail();
			return null;
		}

		progressWindow.setCurrentStepFail();
		return new ArrayList<>();
	}

}

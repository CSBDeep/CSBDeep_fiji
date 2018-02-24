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
package mpicbg.csbd.tiling;

import java.util.Arrays;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.imglib2.ArrangedView;
import mpicbg.csbd.imglib2.CombinedView;
import mpicbg.csbd.util.Task;

public class BatchedTiling extends DefaultTiling {

	protected int batchesNum;
	protected int batchSize;
	protected int batchDim;
	protected int channelDim;
	protected long batchDimSize;

	public BatchedTiling(
			final int tilesNum,
			final int blockMultiple,
			final int overlap,
			final int batchSize,
			final int batchDim,
			final int channelDim ) {
		super( tilesNum, blockMultiple, overlap );
		this.batchSize = batchSize;
		this.batchDim = batchDim;
		this.channelDim = channelDim;
	}

	@Override
	protected AdvancedTiledView< FloatType > createTiledView(
			final Task parent,
			final RandomAccessibleInterval< FloatType > dataset,
			final long[] tileSize,
			final long[] padding ) {

		batchDimSize = tileSize[ batchDim ];
		System.out.println( "batchDimSize  : " + batchDimSize );
		batchesNum = ( int ) Math.ceil( ( float ) batchDimSize / ( float ) batchSize );
		// If a smaller batch size is sufficient for the same amount of batches, we can use it
		batchSize = ( int ) Math.ceil( ( float ) batchDimSize / ( float ) batchesNum );

		parent.setNumSteps( tilesNum * batchesNum );

		final long expandedBatchDimSize = batchesNum * batchSize;
		tileSize[ batchDim ] = batchSize;
		final RandomAccessibleInterval< FloatType > expandedInput2 =
				expandDimToSize( dataset, batchDim, expandedBatchDimSize );
		final AdvancedTiledView< FloatType > tiledView2 =
				new AdvancedTiledView<>( expandedInput2, tileSize, padding );
		return tiledView2;
	}

	@Override
	protected RandomAccessibleInterval< FloatType >
			expandToFitBlockSize(
					final RandomAccessibleInterval< FloatType > dataset,
					final long blockWidth,
					final int largestDim ) {

		// If there is no channel dimension in the input image, we assume that a channel dimension might be added to the end of the image
		if ( channelDim < 0 ) {
			channelDim = dataset.numDimensions();
		}
		System.out.println( "batchDim  : " + batchDim );
		System.out.println( "channelDim: " + channelDim );

		RandomAccessibleInterval< FloatType > expandedInput =
				expandDimToSize( dataset, largestDim, blockWidth * tilesNum );

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
//			printDim( "After expand", im );
		return expandedInput;
	}

	@Override
	protected RandomAccessibleInterval< FloatType >
			arrangeAndCombineTiles(
					final List< RandomAccessibleInterval< FloatType > > results,
					final int largestDim ) {
		final long[] grid = new long[ results.get( 0 ).numDimensions() ];
		for ( int i = 0; i < grid.length; i++ ) {
			if ( i == largestDim ) {
				grid[ i ] = tilesNum;
				continue;
			}
			if ( i == batchDim ) {
				grid[ i ] = batchesNum;
				continue;
			}
			grid[ i ] = 1;
		}
		final long[] res0Dimension = new long[ results.get( 0 ).numDimensions() ];
		results.get( 0 ).dimensions( res0Dimension );
		System.out.println( "res0 dimensions: " + Arrays.toString( res0Dimension ) );
		System.out.println( "nBatches: " + batchesNum );
		System.out.println( "nTiles: " + tilesNum );
		System.out.println( "grid: " + Arrays.toString( grid ) );
		final RandomAccessibleInterval< FloatType > result =
				new CombinedView<>( new ArrangedView<>( results, grid ) );
		return result;
	}

}

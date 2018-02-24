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
import java.util.Map;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import mpicbg.csbd.imglib2.ArrangedView;
import mpicbg.csbd.imglib2.CombinedView;
import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.task.Task;

public class DefaultTiling implements Tiling {

//	protected RandomAccessibleInterval< FloatType > input;
	
//	protected Network network;

	protected int tilesNum;
	protected int blockMultiple;
//	protected long blockWidth;
	protected int overlap;
//	protected long largestSize;

	public DefaultTiling(
			final int tilesNum,
			final int blockMultiple,
			final int overlap ) {

		this.tilesNum = tilesNum;
		this.blockMultiple = blockMultiple;
		this.overlap = overlap;

//		this.network = network;

	}

	@Override
	public AdvancedTiledView< FloatType > preprocess(RandomAccessibleInterval< FloatType > input, Dataset dataset, Task parent) {
		
		System.out.println( "PREPROCESSING---------------------------------------" );

		if ( input != null ) {

			final long[] dims = new long[ input.numDimensions() ];
			input.dimensions( dims );
			parent.log( "Image dimensions: " + Arrays.toString( dims ) );
			parent.log( "Calculate mapping between image and tensor.." );
			
//			final long largestSize = network.getInputNode().getLargestDimSize();
			final int largestDimIndex = getLargestDimIndex(dataset);
			final long largestSize = input.dimension( largestDimIndex );

			long[] padding = getPadding(input, largestDimIndex);

			System.out.println( "padding: " + Arrays.toString( padding ) );

			parent.log( "Divide image into " + tilesNum + " tile(s).." );

			parent.setNumSteps( tilesNum );

			final double blockwidthIdeal = largestSize / ( double ) tilesNum;
			final long blockWidth =
					( long ) ( Math.ceil( blockwidthIdeal / blockMultiple ) * blockMultiple );
			tilesNum  = ( int ) Math.ceil( ( float ) largestSize / blockWidth );
			System.out.println( "blockwidthIdeal: " + blockwidthIdeal );
			System.out.println( "blockwidth: " + blockWidth );
			System.out.println( "blockMultiple: " + blockMultiple );
			System.out.println( "nTiles: " + tilesNum );
			RandomAccessibleInterval< FloatType > expandedInput = expandToFitBlockSize(input, blockWidth, largestDimIndex);
			long[] tileSize = calculateTileSize(expandedInput, blockWidth, largestDimIndex);
			final AdvancedTiledView< FloatType > tiledView = createTiledView(parent, expandedInput, tileSize, padding);
			for( int i = 0; i < input.numDimensions(); i++) {
				tiledView.setOriginalDimSize( dataset.axis( i ).type(), input.dimension( i ));
			}
			tiledView.setLargestDim( largestDimIndex );
			
			printDim("tiledView", tiledView);

			parent.setFinished();

			return tiledView;

		}

		return null;

	}
	
	protected static int getLargestDimIndex(Dataset dataset) {
		// Get the largest dimension and its size
		int largestDim = -1;
		long largestSize = 0;
		for ( int i = 0; i < dataset.numDimensions(); i++) {
			final long dimSize = dataset.dimension( i );
			if (dataset.axis( i ).type().isXY() && dimSize > largestSize ) {
				largestSize = dimSize;
				largestDim = i;
			}
		}
		return largestDim;
	}
	
	protected static void printDim( final String title, final TiledView<FloatType> input ) {
		final long[] dims = new long[ input.numDimensions() ];
		input.dimensions( dims );
		System.out.println( title + ": " + Arrays.toString( dims ) );
	}

	
	protected long[] getPadding(RandomAccessibleInterval< FloatType > input, int largestDimIndex) {
		long[] padding = new long[ input.numDimensions() ];
		int largestDim = largestDimIndex;
		padding[ largestDim ] = overlap;
		return padding;
	}

	protected RandomAccessibleInterval< FloatType >
			expandToFitBlockSize( RandomAccessibleInterval< FloatType > dataset, long blockWidth, int largestDim ) {
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

	protected long[] calculateTileSize(RandomAccessibleInterval< FloatType > dataset, long blockWidth, int largestDim) {
		final long[] tileSize = Intervals.dimensionsAsLongArray( dataset );
		tileSize[ largestDim ] = blockWidth;
		System.out.println( "tilesize: " + Arrays.toString( tileSize ) );
		return tileSize;
	}
	
	protected AdvancedTiledView< FloatType > createTiledView(Task parent, RandomAccessibleInterval< FloatType > dataset, long[] tileSize, long[] padding) {
		return new AdvancedTiledView<>( dataset, tileSize, padding );
	}

	@Override
	public RandomAccessibleInterval< FloatType > postprocess( Task parent, final AdvancedTiledView< FloatType > results, AxisType[] axisTypes ) {
		
		List< RandomAccessibleInterval<FloatType> > resultData = results.getProcessedTiles();

		if ( resultData != null && resultData.size() > 0 ) {

			parent.setStarted();
			
			long[] dims = new long[resultData.get( 0 ).numDimensions()];
			resultData.get( 0 ).dimensions( dims );
			System.out.println( "result 0 before padding removement: " + Arrays.toString( dims ));
			
			for(int i = 0; i < resultData.size(); i++) {
				resultData.set( i, removePadding(resultData.get( i ), results.getOverlap(), results.getLargestDim()));
			}
			
			resultData.get( 0 ).dimensions( dims );
			System.out.println( "result 0 after padding removement: " + Arrays.toString( dims ));

			parent.log( "Merging tiles.." );

			final RandomAccessibleInterval< FloatType > mergedResult = arrangeAndCombineTiles(resultData, results.getLargestDim());
			
			dims = new long[mergedResult.numDimensions()];
			mergedResult.dimensions( dims );
			System.out.println( "merge: " + Arrays.toString( dims ));

			parent.log( "Crop to original size.." );
			
			System.out.println( "original dims" + results.getOriginalDims().toString());

			RandomAccessibleInterval< FloatType > fittedResult = undoExpansion(mergedResult, results.getOriginalDims(), axisTypes);
			
			dims = new long[fittedResult.numDimensions()];
			fittedResult.dimensions( dims );
			System.out.println( "fittedResult dimensions: " + Arrays.toString( dims ) );

//			ImageJ ij = new ImageJ();
//			ij.ui().show( "result", result );
//			ij.ui().show( "fittedresult", fittedResult );
//			ij.ui().show( "_expandedresult", expandedresult );

//			parent.log( "Split result channels.." );
			return fittedResult;
		}

		parent.setFailed();
		return null;
	}
	
	protected RandomAccessibleInterval<FloatType> removePadding( RandomAccessibleInterval< FloatType > result, long[] padding, int largestDim ) {

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
		return Views.zeroMin( Views.expandZero( result, negPaddingPlus ) );
		
	}

	protected RandomAccessibleInterval< FloatType >
			arrangeAndCombineTiles( List< RandomAccessibleInterval< FloatType > > results, int largestDim ) {
		// Arrange and combine the tiles again
		final long[] grid = new long[ results.get( 0 ).numDimensions() ];
		for ( int i = 0; i < grid.length; i++ ) {
			grid[ i ] = i == largestDim ? tilesNum : 1;
		}
		final RandomAccessibleInterval< FloatType > result =
				new CombinedView<>( new ArrangedView<>( results, grid ) );
		return result;
	}
	
	protected RandomAccessibleInterval< FloatType >
	undoExpansion( RandomAccessibleInterval< FloatType > result, Map< AxisType, Long > originalDims, AxisType[] outputAxes ) {
//        int largestDim = network.getOutputNode().getLargestDimIndex();
        RandomAccessibleInterval< FloatType > fittedResult = null; 
//        		expandDimToSize( result, largestDim, largestSize );
//        // undo Expand other dimensions to fit blockMultiple
        System.out.println( "Output axes: " + Arrays.toString( outputAxes ) );
        for ( int i = 0; i < result.numDimensions(); i++ ) {
        	AxisType axis = outputAxes[i];
        	if(axis != Axes.CHANNEL) {
        		long originalSize = originalDims.get( axis ); 
        		fittedResult = expandDimToSize( fittedResult == null ? result : fittedResult, i, originalSize );
        	}
        }
        return fittedResult;
    }
	


//	protected static void replaceNegativesWithMissingIndices( final int[] arr ) {
//		final List< Integer > indices = new ArrayList<>();
//		for ( int i = 0; i < arr.length; i++ ) {
//			indices.add( arr[ i ] );
//		}
//		for ( int i = 0; i < arr.length; i++ ) {
//			if ( !indices.contains( i ) ) {
//				for ( int j = 0; j < arr.length; j++ ) {
//					if ( arr[ j ] == -1 ) {
//						arr[ j ] = i;
//						break;
//					}
//				}
//			}
//		}
//	}

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

}

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

import mpicbg.csbd.imglib2.GridView;
import mpicbg.csbd.task.Task;
import mpicbg.csbd.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DefaultTiling implements Tiling {

	protected final int tilesNum;
	protected int blockMultiple;
	protected int overlap;
	protected Task status;

	public DefaultTiling(
			final int tilesNum,
			final int blockMultiple,
			final int overlap ) {

		this.tilesNum = tilesNum;
		this.blockMultiple = blockMultiple;
		this.overlap = overlap;

	}

	@Override
	public AdvancedTiledView< FloatType > preprocess(RandomAccessibleInterval< FloatType > input, Dataset dataset, Task parent) {

	    this.status = parent;

		if ( input != null ) {

			AxisType[] axes = new AxisType[input.numDimensions()];
			for(int i = 0; i < axes.length; i++) {
				axes[i] = dataset.axis(i).type();
			}

			long[] tiling = new long[input.numDimensions()];
			Arrays.fill(tiling, 1);
			computeTiling(input, axes, tiling, tilesNum, blockMultiple);

			long[] padding = getPadding(tiling);

			parent.log( "Dividing image into " + arrayProduct(tiling) + " tile(s).." );

			RandomAccessibleInterval< FloatType > expandedInput = expandToFitBlockSize(input, tiling, blockMultiple);
			long[] tileSize = calculateTileSize(expandedInput, tiling);

			parent.log( "Size of single image tile: " + Arrays.toString( tileSize ) );

			final AdvancedTiledView< FloatType > tiledView = createTiledView(parent, expandedInput, tileSize, padding, axes);
			for( int i = 0; i < input.numDimensions(); i++) {
				tiledView.getOriginalDims().put( dataset.axis( i ).type(), input.dimension( i ));
			}
			
			DatasetHelper.logDim(parent, "Final image tiling", tiledView);
			parent.debug( "Final tile padding: " + Arrays.toString( padding ) );

			int steps = 1;
			for(int i = 0; i < tiledView.numDimensions(); i++) {
				steps *= tiledView.dimension(i);
			}

			return tiledView;

		}

		return null;

	}

	public static long arrayProduct(long[] array) {
		long rtn = 1;
		for (long i : array) {
			rtn *= i;
		}
		return rtn;
	}

	protected long[] computeTiling(RandomAccessibleInterval< FloatType > input, AxisType[] axes, long[] tiling, int nTiles, int blockSize) {
		int currentTiles = 1;
		for(long tiles : tiling) {
			currentTiles *= tiles;
		}
		if(currentTiles >= nTiles) {
			return tiling;
		}else {
			long[] singleTile = new long[input.numDimensions()];
			int maxDim = -1;
			for(int i = 0; i < singleTile.length; i++) {
				if(axes[i].isSpatial()) {
					singleTile[i] = (long) Math.ceil((input.dimension(i) / tiling[i] + blockSize) / blockSize) * blockSize;
					if(singleTile[i] > blockSize && (maxDim < 0 || singleTile[i] > singleTile[maxDim])) {
						maxDim = i;
					}
				}
			}
			if(maxDim >= 0) {
				tiling[maxDim] += 1;
				return computeTiling(input, axes, tiling, nTiles, blockSize);
			}else {
				return tiling;
			}
		}
	}
	
	protected long[] getPadding(long[] tiling) {
		long[] padding = new long[ tiling.length ];
		for(int i = 0; i < padding.length; i++) {
			if(tiling[i] > 1) padding[ i ] = overlap;
		}
		return padding;
	}

	protected RandomAccessibleInterval< FloatType >
			expandToFitBlockSize(RandomAccessibleInterval< FloatType > dataset, long[] tiling, long BlockSize ) {
		for ( int i = 0; i < dataset.numDimensions(); i++ ) {
			dataset = expandDimToSize(
					dataset, i, ( long ) Math.ceil( dataset.dimension( i )/ tiling[i] / ( double ) blockMultiple ) * blockMultiple * tiling[i] );
		}
		return dataset;
	}

	protected long[] calculateTileSize(RandomAccessibleInterval< FloatType > dataset, long[] tiling) {
		final long[] tileSize = Intervals.dimensionsAsLongArray( dataset );
		for(int i = 0; i < tileSize.length; i++) {
			tileSize[i] /= tiling[i];
		}
		return tileSize;
	}
	
	protected AdvancedTiledView< FloatType > createTiledView(Task parent, RandomAccessibleInterval< FloatType > input, long[] tileSize, long[] padding, AxisType[] types) {
		return new AdvancedTiledView<>( input, tileSize, padding, types );
	}

	@Override
	public RandomAccessibleInterval< FloatType > postprocess( Task parent, final AdvancedTiledView< FloatType > results, AxisType[] axisTypes ) {

		parent.log( "POSTPROCESSING" );
		
		List< RandomAccessibleInterval<FloatType> > resultData = results.getProcessedTiles();

		if ( resultData != null && resultData.size() > 0 ) {

			RandomAccessibleInterval<FloatType> firstResult = resultData.get(0);

			parent.log("output axes: " + Arrays.toString(axisTypes));

			DatasetHelper.debugDim(parent, "result 0 before padding removement", firstResult);

			long[] grid = new long[axisTypes.length];
			Arrays.fill(grid, 1);
			for(int i = 0; i < grid.length; i++) {
				for(int j = 0; j < results.getOriginalAxes().length; j++) {
					if(results.getOriginalAxes()[j].equals(axisTypes[i])) {
						grid[i] = results.dimension(j);
						break;
					}
				}
			}
			for(int i = 0; i < resultData.size(); i++) {
				resultData.set( i, removePadding(resultData.get( i ), results.getOverlap(), results.getOriginalAxes(), axisTypes));
			}

			//TODO log padding / test padding
			DatasetHelper.debugDim(parent, "result 0 after padding removement", firstResult);

			parent.log( "Merging tiles.." );

			final RandomAccessibleInterval< FloatType > mergedResult = arrangeAndCombineTiles(resultData, grid);

			DatasetHelper.debugDim(parent, "merge", mergedResult);
			parent.log( "Crop to original size.." );

			RandomAccessibleInterval< FloatType > fittedResult = undoExpansion(mergedResult, results.getOriginalDims(), axisTypes);

            parent.log( "Output axes: " + Arrays.toString( axisTypes ) );
			DatasetHelper.debugDim(parent, "fittedResult dimensions", fittedResult );

			return fittedResult;
		}

		parent.setFailed();
		return null;
	}
	
	protected RandomAccessibleInterval<FloatType> removePadding( RandomAccessibleInterval< FloatType > result, long[] padding, AxisType[] oldAxes, AxisType[] newAxes ) {

		final long[] negPadding = new long[ result.numDimensions() ];
		for ( int i = 0; i < negPadding.length; i++ ) {
			for(int j = 0; j < oldAxes.length; j++) {
				if(newAxes[i] == oldAxes[j]) {
					negPadding[i] = -padding[j];
				}
			}
		}
		return Views.zeroMin( Views.expandZero( result, negPadding ) );
		
	}

	protected RandomAccessibleInterval< FloatType >
			arrangeAndCombineTiles( List< RandomAccessibleInterval< FloatType > > results, long[] grid ) {
        status.debug( "grid: " + Arrays.toString(grid) );
		// Arrange and combine the tiles again
		final RandomAccessibleInterval< FloatType > result =
				new GridView<>( new ListImg<>( results, grid ) );
		return result;
	}
	
	protected RandomAccessibleInterval< FloatType >
	undoExpansion( RandomAccessibleInterval< FloatType > result, Map< AxisType, Long > originalDims, AxisType[] outputAxes ) {
        RandomAccessibleInterval< FloatType > fittedResult = null;
        for ( int i = 0; i < result.numDimensions(); i++ ) {
        	AxisType axis = outputAxes[i];
        	//TODO maybe implement this in a more dynamic way
        	if(axis != Axes.CHANNEL) {
        		long originalSize = originalDims.get( axis );
				fittedResult = expandDimToSize( fittedResult == null ? result : fittedResult, i, originalSize );
        	}
        }
        return fittedResult;
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

}

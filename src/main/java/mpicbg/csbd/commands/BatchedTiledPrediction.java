package mpicbg.csbd.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	protected int batchSize = 10;
	protected int batchDim;
	protected int channelDim;
	protected long batchDimSize;

	public BatchedTiledPrediction(
			RandomAccessibleInterval< FloatType > input,
			DatasetTensorBridge bridge,
			SavedModelBundle model,
			CSBDeepProgress progressWindow,
			int nTiles,
			int blockMultiple,
			int overlap ) {
		super( input, bridge, model, progressWindow, nTiles, blockMultiple, overlap );
		// TODO Auto-generated constructor stub
	}

	@Override
	public List< RandomAccessibleInterval< FloatType > > call() {
		try {

			bridge.printMapping();
			batchDim = bridge.getDatasetDimIndexByTFIndex( 0 );
			channelDim = bridge.getDatasetDimIndexByTFIndex(
					bridge.getInputTensorInfo().getTensorShape().getDimCount() - 1 );
			TiledView< FloatType > tiledView = preprocess( nTiles, blockMultiple, overlap );
			System.out.println( "batchDim  : " + batchDim );
			System.out.println( "channelDim: " + channelDim );
			System.out.println( "largestDim: " + largestDim );
			long[] tileSize = tiledView.getBlockSize();
			batchDimSize = tileSize[ batchDim ];
			System.out.println( "batchDimSize  : " + batchDimSize );
			nBatches = ( int ) Math.ceil( ( float ) batchDimSize / ( float ) batchSize );

			progressWindow.setProgressBarMax( nTiles * nBatches );

			long expandedBatchDimSize = nBatches * batchSize;
			tileSize[ batchDim ] = batchSize;
			RandomAccessibleInterval< FloatType > expandedInput2 =
					expandDimToSize( expandedInput, batchDim, expandedBatchDimSize );
			TiledView< FloatType > tiledView2 =
					new TiledView<>( expandedInput2, tileSize, padding );

			List< RandomAccessibleInterval< FloatType > > results = runModel( tiledView2 );

//			final ImageJ ij = new ImageJ();
//			int i = 0;
//			for ( RandomAccessibleInterval< FloatType > res : results ) {
//				ij.ui().show( "res" + i, res );
//				i++;
//			}

			return postprocess( results );

		} catch ( Error | Exception e ) {
			e.printStackTrace();
			progressWindow.setCurrentStepFail();
		}
		return null;
	}

	@Override
	protected TiledView< FloatType > preprocess(
			final int nTiles,
			final int blockMultiple,
			final int overlap ) {

		if ( input != null ) {

			long[] dims = new long[ input.numDimensions() ];
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

			this.nTiles = nTiles;

			// Calculate the blocksize to use
			double blockwidthIdeal = largestSize / ( double ) nTiles;
			long blockwidth =
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
			long[] tileSize = Intervals.dimensionsAsLongArray( expandedInput );
			tileSize[ largestDim ] = blockwidth;

			// Put the padding per dimension in a array

			padding[ largestDim ] = overlap;

			System.out.println( "tilesize: " + Arrays.toString( tileSize ) );

			// Create the tiled view
			TiledView< FloatType > tiledView = new TiledView<>( expandedInput, tileSize, padding );

			progressWindow.setCurrentStepDone();

			return tiledView;

		}

		return null;

	}

	@Override
	protected List< RandomAccessibleInterval< FloatType > >
			postprocess( List< RandomAccessibleInterval< FloatType > > results ) {

		if ( results != null && results.size() > 0 ) {

			progressWindow.setStepStart( CSBDeepProgress.STEP_POSTPROCESSING );

			progressWindow.addLog( "Merging tiles.." );

			// Arrange and combine the tiles again
			long[] grid = new long[ results.get( 0 ).numDimensions() ];
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
			long[] res0Dimension = new long[ results.get( 0 ).numDimensions() ];
			results.get( 0 ).dimensions( res0Dimension );
			System.out.println( "res0 dimensions: " + Arrays.toString( res0Dimension ) );
			System.out.println( "nBatches: " + nBatches );
			System.out.println( "nTiles: " + nTiles );
			System.out.println( "grid: " + Arrays.toString( grid ) );
			RandomAccessibleInterval< FloatType > result =
					new CombinedView<>( new ArrangedView<>( results, grid ) );

			long[] resDimension = new long[ result.numDimensions() ];
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

			if ( fittedResult.dimension(
					channelDim ) > 0 ) { return splitChannels( fittedResult, channelDim ); }

			progressWindow.setCurrentStepFail();
			return null;
		}

		progressWindow.setCurrentStepFail();
		return new ArrayList<>();
	}

}

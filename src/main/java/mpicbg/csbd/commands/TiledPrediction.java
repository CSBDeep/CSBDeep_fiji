package mpicbg.csbd.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
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

	ExecutorService pool = Executors.newSingleThreadExecutor();

	public TiledPrediction(
			final RandomAccessibleInterval< FloatType > input,
			final DatasetTensorBridge bridge,
			final SavedModelBundle model,
			CSBDeepProgress progressWindow,
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

			progressWindow.setProgressBarMax( nTiles );

			// Calculate the blocksize to use
			double blockwidthIdeal = largestSize / ( double ) nTiles;
			long blockwidth =
					( long ) ( Math.ceil( blockwidthIdeal / blockMultiple ) * blockMultiple );

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

			long[] imdims = new long[ expandedInput.numDimensions() ];
			expandedInput.dimensions( imdims );
			System.out.println( "imdims: " + Arrays.toString( imdims ) );
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

	public List< RandomAccessibleInterval< FloatType > >
			runModel( TiledView< FloatType > tiledView ) {

		progressWindow.setStepStart( CSBDeepProgress.STEP_RUNMODEL );

		boolean multithreading = false;

		// Set padding to negative to remove it later
		long[] negPadding = padding.clone();
		negPadding[ largestDim ] = -padding[ largestDim ];

		Cursor< RandomAccessibleInterval< FloatType > > cursor =
				Views.iterable( tiledView ).cursor();

		// Loop over the tiles and execute the prediction
		List< RandomAccessibleInterval< FloatType > > results = new ArrayList<>();
		List< Future< RandomAccessibleInterval< FloatType > > > futures = new ArrayList<>();

		progressWindow.setProgressBarValue( 0 );
		doneTileCount = 0;

		while ( cursor.hasNext() && !cancelPressed ) {
			RandomAccessibleInterval< FloatType > tile = cursor.next();
			//uiService.show(tile);
			Future< RandomAccessibleInterval< FloatType > > future = pool.submit(
					new TileRunner( tile, negPadding, progressWindow ) );

			progressWindow.addLog(
					"Processing tile " + ( doneTileCount + 1 ) + ".." );

			futures.add( future );

			if ( !multithreading ) {
				try {
					RandomAccessibleInterval< FloatType > res = future.get();
					if ( res == null ) return null;
					results.add( res );
					upTileCount();
				} catch ( InterruptedException exc ) {
					pool.shutdownNow();
					progressWindow.setCurrentStepFail();
					return null;
				} catch ( Exception exc ) {
					pool.shutdownNow();
					exc.printStackTrace();
					progressWindow.setCurrentStepFail();
					return null;
				}
			}
		}
		if ( multithreading ) {
			for ( Future< RandomAccessibleInterval< FloatType > > future : futures ) {
				try {
					RandomAccessibleInterval< FloatType > res = future.get();
					if ( res == null ) return null;
					results.add( res );
					upTileCount();
				} catch ( Exception exc ) {
					pool.shutdownNow();
					exc.printStackTrace();
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

	protected List< RandomAccessibleInterval< FloatType > >
			postprocess( List< RandomAccessibleInterval< FloatType > > results ) {

		if ( results != null && results.size() > 0 ) {

			progressWindow.setStepStart( CSBDeepProgress.STEP_POSTPROCESSING );

			progressWindow.addLog( "Merging tiles.." );

			// Arrange and combine the tiles again
			long[] grid = new long[ results.get( 0 ).numDimensions() ];
			for ( int i = 0; i < grid.length; i++ ) {
				grid[ i ] = i == largestDim ? nTiles : 1;
			}
			RandomAccessibleInterval< FloatType > result =
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

			int lastdim = fittedResult.numDimensions() - 1;

			if ( fittedResult.dimension(
					lastdim ) > 0 ) { return splitChannels( fittedResult, lastdim ); }

			progressWindow.setCurrentStepFail();
			return null;
		}

		progressWindow.setCurrentStepFail();
		return new ArrayList<>();
	}

	@Override
	public List< RandomAccessibleInterval< FloatType > > call() {
		try {

			TiledView< FloatType > tiledView = preprocess();

			progressWindow.setProgressBarValue( 0 );

			progressWindow.setStepStart( CSBDeepProgress.STEP_RUNMODEL );
			List< RandomAccessibleInterval< FloatType > > results = runModel( tiledView );

			return postprocess( results );

		} catch ( Error | Exception e ) {
			e.printStackTrace();
			progressWindow.setCurrentStepFail();
		}
		return null;
	}

	protected static < T extends RealType< T > > int
			getLargestDim( RandomAccessibleInterval< T > input, DatasetTensorBridge bridge ) {
		// Get the largest dimension and its size
		int largestDim = 0;
		long largestSize = 0;
		for ( int d = 0; d < input.numDimensions(); d++ ) {
			long dimSize = input.dimension( d );
			if ( bridge.getDimTypeByDatasetDim( d ).isXY() && dimSize > largestSize ) {
				largestSize = dimSize;
				largestDim = d;
			}
		}
		return largestDim;
	}

	protected static void
			calculateMapping( int[] mappingIn, int[] mappingOut, DatasetTensorBridge bridge ) {

		for ( int i = 0; i < mappingIn.length; i++ ) {
			mappingIn[ i ] = bridge.getTfIndexByDatasetDim( i );
		}
		replaceNegativesWithMissingIndices( mappingIn );
		System.out.println( "mapping in: " + Arrays.toString( mappingIn ) );

		//check if network reduces dimension, if yes, remote Z from mapping
		bridge.handleDimensionReduction();

		AxisType[] mappingOutDimType = new AxisType[ mappingOut.length ];
		for ( int i = 0; i < mappingOut.length; i++ ) {
			mappingOut[ i ] =
					bridge.getTfIndexByDatasetDim( i );
			mappingOutDimType[ i ] =
					bridge.getDimTypeByDatasetDim( i );
		}
		replaceNegativesWithMissingIndices( mappingOut );
		System.out.println( "mapping out: " + Arrays.toString( mappingOut ) );
	}

	public < T extends RealType< T > > List< RandomAccessibleInterval< FloatType > >
			splitChannels( RandomAccessibleInterval< FloatType > img, int channelDim ) {

		progressWindow.addLog( "Split result channels.." );

		ArrayList< RandomAccessibleInterval< FloatType > > res = new ArrayList<>();

		for ( int i = 0; i < img.dimension( channelDim ); i++ ) {
			res.add( Views.zeroMin( Views.hyperSlice( img, channelDim, i ) ) );
		}

		return res;
	}

	protected static void replaceNegativesWithMissingIndices( int[] arr ) {
		List< Integer > indices = new ArrayList<>();
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

	protected static RandomAccessibleInterval< FloatType >
			expandDimToSize( RandomAccessibleInterval< FloatType > im, int d, long size ) {
		final int n = im.numDimensions();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		im.min( min );
		im.max( max );
		max[ d ] += ( size - im.dimension( d ) );
		return Views.interval( Views.extendMirrorDouble( im ), new FinalInterval( min, max ) );
	}

	protected RandomAccessibleInterval< FloatType >
			executeGraphWithPadding( final RandomAccessibleInterval< FloatType > tile ) {

		Tensor inputTensor = DatasetConverter.datasetToTensor( tile, mappingIn );
		if ( inputTensor != null ) {
			Tensor outputTensor = null;
			try {
				outputTensor = TensorFlowRunner.executeGraph(
						model,
						inputTensor,
						bridge.getInputTensorInfo(),
						bridge.getOutputTensorInfo() );
			} catch ( Exception e ) {
				e.printStackTrace();
				progressWindow.addError(
						"Error while running model: \n" + e.getLocalizedMessage() );
				progressWindow.setCurrentStepFail();
			}
			if ( outputTensor != null ) { return DatasetConverter.tensorToDataset(
					outputTensor,
					mappingOut ); }
		}
		return null;
	}

	class TileRunner implements Callable< RandomAccessibleInterval< FloatType > > {

		RandomAccessibleInterval< FloatType > tile;
		CSBDeepProgress progressWindow;
		long[] negPadding;

		public TileRunner(
				RandomAccessibleInterval< FloatType > tile,
				long[] negPadding,
				CSBDeepProgress progressWindow ) {
			this.tile = tile;
			this.negPadding = negPadding;
			this.progressWindow = progressWindow;
		}

		@Override
		public RandomAccessibleInterval< FloatType > call() throws Exception {
			RandomAccessibleInterval< FloatType > result =
					executeGraphWithPadding( tile );
			if ( result != null ) {

				long[] negPaddingPlus = new long[ result.numDimensions() ];
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

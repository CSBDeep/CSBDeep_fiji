package mpicbg.csbd.commands;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.scijava.thread.ThreadService;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;

import mpicbg.csbd.imglib2.ArrangedView;
import mpicbg.csbd.imglib2.CombinedView;
import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.tensorflow.DatasetConverter;
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.tensorflow.TensorFlowRunner;
import mpicbg.csbd.ui.CSBDeepProgress;

public class TiledPrediction {

	private RandomAccessibleInterval< FloatType > input;
	private SavedModelBundle model;
	private DatasetTensorBridge bridge;

	private int nTiles;
	private int largestDim;
	private long largestSize;
	private long[] padding;

	private int[] mappingIn, mappingOut;

	private ThreadService threadService;

	private final CSBDeepProgress progressWindow;

	private int doneTileCount;

	public TiledPrediction(
			final RandomAccessibleInterval< FloatType > input,
			final DatasetTensorBridge bridge,
			final SavedModelBundle model,
			CSBDeepProgress progressWindow ) {

		this.progressWindow = progressWindow;

		final ImageJ ij = new ImageJ();
		threadService = ij.thread();

		this.input = input;
		this.bridge = bridge;
		this.model = model;

	}

	private TiledView< FloatType > preprocess(
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

			progressWindow.addLog( "Divide image in " + nTiles + " tile(s).." );

			padding = new long[ input.numDimensions() ];

			this.nTiles = nTiles;

			// Calculate the blocksize to use
			double blockwidthIdeal = largestSize / ( double ) nTiles;
			long blockwidth =
					( long ) ( Math.ceil( blockwidthIdeal / blockMultiple ) * blockMultiple );

			// Expand the image to fit the blocksize
			RandomAccessibleInterval< FloatType > im =
					expandDimToSize( input, largestDim, blockwidth * nTiles );

			// Expand other dimensions to fit blockMultiple
			for ( int i = 0; i < im.numDimensions(); i++ ) {
				if ( bridge.getDimTypeByDatasetDim( i ).isXY() ) {
					im = i == largestDim ? im : expandDimToSize(
							im,
							i,
							( long ) Math.ceil(
									im.dimension(
											i ) / ( double ) blockMultiple ) * blockMultiple );
				}
			}

			long[] imdims = new long[ im.numDimensions() ];
			im.dimensions( imdims );
			System.out.println( "imdims: " + Arrays.toString( imdims ) );
//				printDim( "After expand", im );

			// Set the tile size
			long[] tileSize = Intervals.dimensionsAsLongArray( im );
			tileSize[ largestDim ] = blockwidth;

			// Put the padding per dimension in a array

			padding[ largestDim ] = overlap;

			System.out.println( "tilesize: " + Arrays.toString( tileSize ) );

			// Create the tiled view
			TiledView< FloatType > tiledView = new TiledView<>( im, tileSize, padding );

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

		progressWindow.setProgressBarMax( nTiles );
		progressWindow.setProgressBarValue( 0 );
		doneTileCount = 0;

		while ( cursor.hasNext() ) {
			RandomAccessibleInterval< FloatType > tile = cursor.next();
			//uiService.show(tile);
			if ( multithreading ) {
				futures.add(
						threadService.run( new Callable< RandomAccessibleInterval< FloatType > >() {

							@Override
							public RandomAccessibleInterval< FloatType > call() {
								progressWindow.addLog(
										"Processing tile " + ( doneTileCount + 1 ) + " / " + nTiles + ".." );
								return executeGraphWithPadding( tile );
							}
						} ) );
			} else {
				try {
					threadService.invoke( new Runnable() {

						@Override
						public void run() {
							progressWindow.addLog(
									"Processing tile " + ( doneTileCount + 1 ) + " / " + nTiles + ".." );
							RandomAccessibleInterval< FloatType > tileExecuted =
									executeGraphWithPadding( tile );
							if ( tileExecuted != null ) {

								long[] negPaddingPlus = new long[ tileExecuted.numDimensions() ];
								for ( int i = 0; i < negPadding.length; i++ ) {
									negPaddingPlus[ i ] = negPadding[ i ];
								}
								tileExecuted =
										Views.zeroMin(
												Views.expandZero( tileExecuted, negPaddingPlus ) );

//								ImageJ ij = new ImageJ();
//								ij.ui().show( tileExecuted );
								doneTileCount++;
								progressWindow.setProgressBarValue( doneTileCount );
								results.add( tileExecuted );
							}
						}

					} );
				} catch ( InvocationTargetException | InterruptedException exc ) {
					// TODO Auto-generated catch block
					exc.printStackTrace();
					progressWindow.setCurrentStepFail();
					return new ArrayList<>();
				}
			}
		}

		for ( Future< RandomAccessibleInterval< FloatType > > future : futures ) {
			RandomAccessibleInterval< FloatType > tileExecuted = null;
			try {
				tileExecuted = future.get();
			} catch ( InterruptedException | ExecutionException exc ) {
				exc.printStackTrace();
				for ( Future< RandomAccessibleInterval< FloatType > > otherfuture : futures ) {
					if ( !otherfuture.isDone() ) {
						otherfuture.cancel( true );
					}
				}
				progressWindow.setCurrentStepFail();
				return new ArrayList<>();
			}
			if ( tileExecuted != null ) {

				long[] negPaddingPlus = new long[ tileExecuted.numDimensions() ];
				for ( int i = 0; i < negPadding.length; i++ ) {
					negPaddingPlus[ i ] = negPadding[ i ];
				}
				tileExecuted =
						Views.zeroMin(
								Views.expandZero( tileExecuted, negPaddingPlus ) );

				doneTileCount++;
				progressWindow.setProgressBarValue( doneTileCount );
				results.add( tileExecuted );
			}

		}
		progressWindow.setCurrentStepDone();
		return results;
	}

	private List< RandomAccessibleInterval< FloatType > >
			postprocess( List< RandomAccessibleInterval< FloatType > > results ) {

		if ( results.size() > 0 ) {

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

//			ImageJ ij = new ImageJ();
//			ij.ui().show( "_result", result );
//			ij.ui().show( "_expandedresult", expandedresult );

			int lastdim = fittedResult.numDimensions() - 1;

			if ( fittedResult.dimension(
					lastdim ) > 0 ) { return splitChannels( fittedResult, lastdim ); }

			progressWindow.setCurrentStepFail();
			return new ArrayList<>();
		}

		progressWindow.setCurrentStepFail();
		return new ArrayList<>();
	}

	public List< RandomAccessibleInterval< FloatType > >
			run( final int nTiles, final int blockMultiple, final int overlap ) {

		try {

			TiledView< FloatType > tiledView = preprocess( nTiles, blockMultiple, overlap );

			List< RandomAccessibleInterval< FloatType > > results = runModel( tiledView );

			return postprocess( results );

		} catch ( Error | Exception e ) {
			e.printStackTrace();
			progressWindow.setCurrentStepFail();
		}
		return new ArrayList<>();
	}

	private static < T extends RealType< T > > int
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

	private static void
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

		long[] minint = new long[ img.numDimensions() ];
		long[] maxint = new long[ img.numDimensions() ];

		for ( int i = 0; i < minint.length; i++ ) {
			for ( int j = 0; j < minint.length; j++ ) {
				minint[ j ] = img.min( j );
				maxint[ j ] = img.max( j );
			}
			minint[ channelDim ] = i;
			maxint[ channelDim ] = i;
			res.add( Views.zeroMin( Views.interval( img, minint, maxint ) ) );
		}

		return res;
	}

	private static void replaceNegativesWithMissingIndices( int[] arr ) {
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

	private static RandomAccessibleInterval< FloatType >
			expandDimToSize( RandomAccessibleInterval< FloatType > im, int d, long size ) {
		final int n = im.numDimensions();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		im.min( min );
		im.max( max );
		max[ d ] += ( size - im.dimension( d ) );
		return Views.interval( Views.extendMirrorDouble( im ), new FinalInterval( min, max ) );
	}

	private RandomAccessibleInterval< FloatType >
			executeGraphWithPadding( final RandomAccessibleInterval< FloatType > tile ) {

		Tensor inputTensor = DatasetConverter.datasetToTensor( tile, mappingIn );
		if ( inputTensor != null ) {
			Tensor outputTensor = TensorFlowRunner.executeGraph(
					model,
					inputTensor,
					bridge.getInputTensorInfo(),
					bridge.getOutputTensorInfo() );
			if ( outputTensor != null ) { return DatasetConverter.tensorToDataset(
					outputTensor,
					mappingOut ); }
		}
		return null;
	}

}

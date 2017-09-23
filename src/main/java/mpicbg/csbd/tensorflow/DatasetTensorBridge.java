package mpicbg.csbd.tensorflow;

import net.imagej.Dataset;
import net.imagej.axis.Axes;

import org.tensorflow.Shape;
import org.tensorflow.Tensor;

public class DatasetTensorBridge {

	// STATICS
	public static int UNSET = -1;
	public static int X = 0;
	public static int Y = 1;
	public static int Z = 2;
	public static int C = 3;
	public static int T = 4;

	private final Dataset dataset;
	private Shape initialInputTensorShape;

	private final String[] datasetDimNames;
	private final int[] datasetDimIndices;
	private final long[] datasetDimLengths;
	private final int[] dimMapping = { UNSET, UNSET, UNSET, UNSET, UNSET };
	private boolean mappingInitialized = false;

	public DatasetTensorBridge( final Dataset image ) {
		dataset = image;
		final int maxdim = 5;
		datasetDimNames = new String[ maxdim ];
		datasetDimNames[ X ] = "X";
		datasetDimNames[ Y ] = "Y";
		datasetDimNames[ Z ] = "Z";
		datasetDimNames[ C ] = "C";
		datasetDimNames[ T ] = "T";
		datasetDimIndices = new int[ maxdim ];
		datasetDimIndices[ X ] = image.dimensionIndex( Axes.X );
		datasetDimIndices[ Y ] = image.dimensionIndex( Axes.Y );
		datasetDimIndices[ Z ] = image.dimensionIndex( Axes.Z );
		datasetDimIndices[ C ] = image.dimensionIndex( Axes.CHANNEL );
		datasetDimIndices[ T ] = image.dimensionIndex( Axes.TIME );
		datasetDimLengths = new long[ maxdim ];
		datasetDimLengths[ X ] = image.getWidth();
		datasetDimLengths[ Y ] = image.getHeight();
		datasetDimLengths[ Z ] = image.getDepth();
		datasetDimLengths[ C ] = image.getChannels();
		datasetDimLengths[ T ] = image.getFrames();
	}

	public long getDatasetDimLengthFromTFIndex( final int tfIndex5D ) {
		if ( dimMapping[ tfIndex5D ] < 0 ) { return 1; }
		return datasetDimLengths[ dimMapping[ tfIndex5D ] ];
	}

	public int getDatasetDimIndexByTFIndex( final int tfIndex5D ) {
		if ( dimMapping[ tfIndex5D ] < 0 ) { return -1; }
		return datasetDimIndices[ dimMapping[ tfIndex5D ] ];
	}

	public boolean isMappingInitialized() {
		return mappingInitialized;
	}

	public void setMappingInitialized( final boolean mappingInitialized ) {
		this.mappingInitialized = mappingInitialized;
	}

	public void setMappingDefaults() {
		System.out.println( "setmappingdefaults" );
		setMappingInitialized( true );
		if ( initialInputTensorShape.numDimensions() == 5 ) {
			dimMapping[ 0 ] = T;
			dimMapping[ 1 ] = Z;
			dimMapping[ 2 ] = Y;
			dimMapping[ 3 ] = X;
			dimMapping[ 4 ] = C;
		} else {
			if ( initialInputTensorShape.numDimensions() == 4 ) {
				// If all is 1, we take this one
				if ( dataset.getChannels() <= 1 && dataset.getFrames() == 1 && dataset.getDepth() == 1 ) {
					dimMapping[ 0 ] = UNSET;
					dimMapping[ 1 ] = Z;
					dimMapping[ 2 ] = Y;
					dimMapping[ 3 ] = X;
					dimMapping[ 4 ] = C;
					// Otherwise if channels > 1 we make it depend on frames...
				} else if ( dataset.getChannels() > 1 && dataset.getFrames() > 1 ) {
					dimMapping[ 0 ] = UNSET;
					dimMapping[ 1 ] = T;
					dimMapping[ 2 ] = Y;
					dimMapping[ 3 ] = X;
					dimMapping[ 4 ] = C;
					// ... depth size ...
				} else if ( dataset.getChannels() > 1 && dataset.getDepth() > 1 ) {
					dimMapping[ 0 ] = UNSET;
					dimMapping[ 1 ] = Z;
					dimMapping[ 2 ] = Y;
					dimMapping[ 3 ] = X;
					dimMapping[ 4 ] = C;
					// And in all other cases we do this:
				} else {
					dimMapping[ 0 ] = UNSET;
					dimMapping[ 1 ] = Z;
					dimMapping[ 2 ] = Y;
					dimMapping[ 3 ] = X;
					dimMapping[ 4 ] = C;
				}
			}
		}
	}

	public void setInputTensorShape( final Shape shape ) {
		initialInputTensorShape = shape;
	}

	public Shape getInitialInputTensorShape() {
		return initialInputTensorShape;
	}

	public int numDimensions() {
		return datasetDimNames.length;
	}

	public long getDatasetDimLength( final int index ) {
		return datasetDimLengths[ index ];
	}

	public String getDatasetDimName( final int index ) {
		return datasetDimNames[ index ];
	}

	public int getMapping( final int tfIndex5D ) {
		return dimMapping[ tfIndex5D ];
	}

	public void setMapping( final int tfIndex5D, final int mapping ) {
		dimMapping[ tfIndex5D ] = mapping;
	}

	public void setMappingInputTensorDim( final int inputTensorDim, final int mapping ) {
		dimMapping[ inputTensorDim + dimMapping.length - initialInputTensorShape.numDimensions() ] =
				mapping;
	}

	public float[][][][][] createTFArray5D() {
//    	System.out.println("create 5d tf array with dims: " + getDatasetDimLengthFromTFIndex(0) + " " + getDatasetDimLengthFromTFIndex(1) + " " + getDatasetDimLengthFromTFIndex(2) + " " + getDatasetDimLengthFromTFIndex(3) + " " + getDatasetDimLengthFromTFIndex(4));
		return new float[ ( int ) getDatasetDimLengthFromTFIndex(
				0 ) ][ ( int ) getDatasetDimLengthFromTFIndex(
						1 ) ][ ( int ) getDatasetDimLengthFromTFIndex(
								2 ) ][ ( int ) getDatasetDimLengthFromTFIndex(
										3 ) ][ ( int ) getDatasetDimLengthFromTFIndex( 4 ) ];
	}


	public boolean complete() {
		return initialInputTensorShape != null && dataset != null;
	}

	public void printMapping() {
		System.out.println( "--------------" );
		System.out.print( "mapping:" );
		for ( final int i : dimMapping ) {
			System.out.print( " " + i );
		}
		System.out.println();
		System.out.print( "datasetDimIndices:" );
		for ( final int i : datasetDimIndices ) {
			System.out.print( " " + i );
		}
		System.out.println();
		System.out.println( "--------------" );
	}

	/*
	 * moves Z dimension in mapping to the first position and set it to -1
	 * example:
	 * mapping: [T,Z,Y,X,C] -> [-1,T,Y,X,C]
	 * mapping: [T,Y,X,C,Z] -> [-1,T,Y,X,C]
	 *
	 */
	public boolean removeZFromMapping() {
		boolean shift = false;
		printMapping();
		for ( int i = numDimensions() - 1; i >= 0; i-- ) {
			if ( dataset.dimensionIndex( Axes.Z ) == getMapping( i ) ) {
				shift = true;
			}
			if ( shift ) {
				if ( i > 0 ) {
					setMapping( i, getMapping( i - 1 ) );
				} else {
					setMapping( i, -1 );
				}

			}
		}
		printMapping();
		return shift;
	}

	public float[][][][][] createTFArray5D( final Tensor output_t ) {
		final int[] dims = { 1, 1, 1, 1, 1 };
		final int diff = dims.length - output_t.numDimensions();
		for ( int i = 0; i < output_t.numDimensions(); i++ ) {
			dims[ diff + i ] = ( int ) output_t.shape()[ i ];
		}
//		System.out.println("create 5D tf output array with dims: " + dims[0] + " " + dims[1] + " "
//						+ dims[2] + " " + dims[3] + " " + dims[4]);
		return new float[ dims[ 0 ] ][ dims[ 1 ] ][ dims[ 2 ] ][ dims[ 3 ] ][ dims[ 4 ] ];
	}

	/*
	 * create dataset from output tensor shape of model
	 * the output dataset should have the same dimensions as the input dataset
	 * if a dimension is reduced by the graph, the dimension will still be
	 * present but have size 1
	 * this looks more complicated than it should, but i tried quite some
	 * simpler versions without success
	 */
	public Dataset createDatasetFromTFDims( final long[] tfdims ) {

//		for(int i = 0; i < tfdims.length; i++){
//			System.out.println("tfdims " + i + ": " + tfdims[i]);
//		}

		final long[] _dims = { -1, -1, -1, -1, -1 };

//		printMapping();

		/*
		 * iterate over possible image dimensions and check whether they exist
		 * in input dataset
		 */
		for ( int i = 0; i < datasetDimIndices.length; i++ ) {
			final int dataset_i = datasetDimIndices[ i ];
			if ( dataset_i >= 0 ) {
				/*
				 * dimension exists in input dataset
				 */
				for ( int j = 0; j < dimMapping.length; j++ ) {
					if ( dimMapping[ j ] == dataset_i ) {
						/*
						 * tf mapping for dimension found
						 */

//						System.out.println("found mapping, " + i + ": " + i + " j: " + j + " tfindex: " + findIndexNotNegative(dimMapping, j));

						/*
						 * in dimMapping, unused dimensions are filled with "-1"
						 * tfdims contains the real (existing) dimensions of the
						 * tensor shape and ..
						 * .. should have the same length as dimMapping entries
						 * >= 0
						 */
						_dims[ dataset_i ] =
								tfdims[ getIndexByIndexIgnoreNegatives( dimMapping, j ) ];
					}
				}
				/*
				 * dimension exists in input dataset, but is not mapped
				 * the output dataset should also have this dimension, but set
				 * size to 1
				 */
				if ( _dims[ dataset_i ] == -1 ) {
					_dims[ dataset_i ] = 1;
				}
			}

		}

//		for(int i = 0; i < _dims.length; i++){
//			System.out.println("_dataset output dim " + i + ": " + _dims[i]);
//		}

		/*
		 * create final dimension array for output dataset
		 * remove negative entries from _dim to match input and output dataset
		 * dimensions
		 */

		final long[] dims = new long[ dataset.numDimensions() ];

		int j = 0;
		for ( int i = 0; i < _dims.length; i++ ) {
			if ( _dims[ i ] > 0 ) {
				dims[ j ] = _dims[ i ];
				j++;
			}
		}

//		for(int i = 0; i < dims.length; i++){
//			System.out.println("dataset output dim " + i + ": " + dims[i]);
//		}
//
//		System.out.println("dims length: " + dims.length);
//		System.out.println("dataset length: " + dataset.numDimensions());
		final Dataset img_out = dataset.factory().create( dims, dataset.firstElement() );
//		System.out.println("img dims length: " + img_out.numDimensions());
		return img_out;
	}

	/*
	 * get index of value in searcharray but do not count negative entries
	 * example:
	 * searcharray: [-1, -1, 0, 1, 2], val: 0, returns 2
	 * searcharray: [-1, -1, 0, 1, 2], val: 2, returns 4
	 *
	 */
	private int getIndexByIndexIgnoreNegatives( final int[] searcharray, final int val ) {
		int count = 0;
		for ( int i = 0; i < searcharray.length; i++ ) {
			if ( i == val ) { return count; }
			if ( searcharray[ i ] >= 0 ) {
				count++;
			}
		}
		return -1;
	}

}

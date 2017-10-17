package mpicbg.csbd.tensorflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

import org.tensorflow.framework.TensorShapeProto;

public class DatasetTensorBridge {

	// STATICS
	public static int UNSET = -1;
	public static int X = 0;
	public static int Y = 1;
	public static int Z = 2;
	public static int C = 3;
	public static int T = 4;

	private final Dataset dataset;
	private TensorShapeProto inputTensorShape, outputTensorShape;

	private final String[] datasetDimNames;
	private final int[] datasetDimIndices, datasetIndicesDim;
	private final long[] datasetDimLengths;
	private final int[] dimMapping = { UNSET, UNSET, UNSET, UNSET, UNSET };
	private final int[] tfMapping = { UNSET, UNSET, UNSET, UNSET, UNSET };
	private boolean mappingInitialized = false;

	public DatasetTensorBridge( final Dataset image ) {

		//check if image has unknown dimensions. if yes, assign unused dimensions
		assignUnknownDimensions( image );

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
		datasetIndicesDim = new int[ maxdim ];
		for ( int i = 0; i < maxdim; i++ ) {
			datasetIndicesDim[ i ] = -1;
		}
		for ( int i = 0; i < maxdim; i++ ) {
			if ( datasetDimIndices[ i ] >= 0 ) {
				datasetIndicesDim[ datasetDimIndices[ i ] ] = i;
			}
		}
		datasetDimLengths = new long[ maxdim ];
		datasetDimLengths[ X ] = image.getWidth();
		datasetDimLengths[ Y ] = image.getHeight();
		datasetDimLengths[ Z ] = image.getDepth();
		datasetDimLengths[ C ] = image.getChannels();
		datasetDimLengths[ T ] = image.getFrames();
	}

	private void assignUnknownDimensions( Dataset image ) {
		AxisType[] axes = Axes.knownTypes();
		List< AxisType > unusedAxes = new ArrayList<>();
		List< Integer > unknownIndices = new ArrayList<>();
		for ( int j = 0; j < axes.length; j++ ) {
			boolean knownAxis = false;
			for ( int i = 0; i < image.numDimensions(); i++ ) {
				if ( image.axis( i ).type() == axes[ j ] ) {
					knownAxis = true;
					break;
				}
			}
			if ( !knownAxis ) unusedAxes.add( axes[ j ] );
		}

		for ( int i = 0; i < image.numDimensions(); i++ ) {
			boolean knownAxis = false;
			for ( int j = 0; j < axes.length; j++ ) {
				if ( image.axis( i ).type() == axes[ j ] ) {
					knownAxis = true;
					break;
				}
			}
			if ( !knownAxis ) unknownIndices.add( i );
		}

		for ( int i = 0; i < unknownIndices.size() && i < unusedAxes.size(); i++ ) {
			image.axis( unknownIndices.get( i ) ).setType( unusedAxes.get( i ) );
		}

	}

	public long getDatasetDimLengthFromTFIndex( final int tfIndex5D ) {
		if ( dimMapping[ tfIndex5D ] < 0 ) { return 1; }
		return datasetDimLengths[ dimMapping[ tfIndex5D ] ];
	}

	public int getDatasetDimIndexByTFIndex( final int tfIndex5D ) {
		if ( dimMapping[ tfIndex5D ] < 0 ) { return -1; }
		return datasetDimIndices[ dimMapping[ tfIndex5D ] ];
	}

	public int getTfIndexByDatasetDim( final int datasetDim ) {
		if ( datasetIndicesDim[ datasetDim ] < 0 ) return -1;
		return tfMapping[ datasetIndicesDim[ datasetDim ] ];
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
		if ( inputTensorShape.getDimCount() == 5 ) {
			dimMapping[ 0 ] = T;
			dimMapping[ 1 ] = Z;
			dimMapping[ 2 ] = Y;
			dimMapping[ 3 ] = X;
			dimMapping[ 4 ] = C;
		} else {
			if ( inputTensorShape.getDimCount() == 4 ) {
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
		for ( int i = 0; i < dimMapping.length; i++ ) {
			if ( dimMapping[ i ] >= 0 ) {
				tfMapping[ dimMapping[ i ] ] = i;
			}
		}
		printMapping();
	}

	public void setInputTensorShape( final TensorShapeProto shape ) {
		inputTensorShape = shape;
		String shapetxt = "[";
		for ( int i = 0; i < shape.getDimCount(); i++ ) {
			if ( i != 0 ) {
				shapetxt += ", ";
			}
			shapetxt += shape.getDim( i ).getSize();
		}
		System.out.println( "DatasetTensorBridge::setInputTensorShape: " + shapetxt + "]" );
	}

	public void setOutputTensorShape( final TensorShapeProto shape ) {
		outputTensorShape = shape;
		String shapetxt = "[";
		for ( int i = 0; i < shape.getDimCount(); i++ ) {
			if ( i != 0 ) {
				shapetxt += ", ";
			}
			shapetxt += shape.getDim( i ).getSize();
		}
		System.out.println( "DatasetTensorBridge::setOutputTensorShape: " + shapetxt + "]" );
	}

	public TensorShapeProto getAbstractInputTensorShape() {
		return inputTensorShape;
	}

	public TensorShapeProto getAbstractOutputTensorShape() {
		return outputTensorShape;
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

	public int[] getMapping() {
		int[] res = dimMapping.clone();
		for ( int i = 0; i < res.length; i++ ) {
			if ( res[ i ] < 0 ) res[ i ] = 0;
		}
		return res;
	}

	public int getMapping( final int tfIndex5D ) {
		return dimMapping[ tfIndex5D ];
	}

	public void setMapping( final int tfIndex5D, final int mapping ) {
		dimMapping[ tfIndex5D ] = mapping;
		for ( int i = 0; i < dimMapping.length; i++ ) {
			if ( dimMapping[ i ] >= 0 ) {
				tfMapping[ dimMapping[ i ] ] = i;
			}
		}
	}

	public void setMappingInputTensorDim( final int inputTensorDim, final int mapping ) {
		dimMapping[ inputTensorDim + dimMapping.length - inputTensorShape.getDimCount() ] =
				mapping;
		for ( int i = 0; i < dimMapping.length; i++ ) {
			if ( dimMapping[ i ] >= 0 ) {
				tfMapping[ dimMapping[ i ] ] = i;
			}
		}
	}

	public boolean complete() {
		return inputTensorShape != null && dataset != null;
	}

	public void printMapping() {
		System.out.println( "--------------" );
		System.out.println( "datasetDimIndices:" + Arrays.toString( datasetDimIndices ) );
		System.out.println( "datasetIndicesDim:" + Arrays.toString( datasetIndicesDim ) );
		System.out.println( "dimMapping:" + Arrays.toString( dimMapping ) );
		System.out.println( "tfMapping:" + Arrays.toString( tfMapping ) );
		System.out.println( "--------------" );
	}

	public void handleDimensionReduction() {
		if ( inputTensorShape.getDimCount() == outputTensorShape.getDimCount() + 1 ) {
			removeZFromMapping();
		}
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

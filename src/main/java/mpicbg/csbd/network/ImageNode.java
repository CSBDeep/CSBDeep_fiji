package mpicbg.csbd.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;

import mpicbg.csbd.util.ArrayHelper;

public class ImageNode {
	
	// I do not use 
	AxisType[] availableAxes = { Axes.X, Axes.Y, Axes.Z, Axes.TIME, Axes.CHANNEL };
	
	private String name;
	private List< AxisType > nodeAxes = new ArrayList<>();
	private long[] nodeShape;
	private List< Long > nodePadding = new ArrayList<>();
	private List<Integer > finalMapping = new ArrayList<>();
	private Dataset dataset;
	private boolean mappingInitialized = false;
	private boolean reducedZ = false;
	
	public ImageNode() {
		
	}
	
	public void initialize(final Dataset dataset) {
		long[] dims = new long[dataset.numDimensions()];
		dataset.dimensions( dims );
		System.out.println( "Dataset dimensions: " + Arrays.toString( dims ) );
		this.dataset = dataset;
	}
	
	public void setNodeShape(final long[] shape) {
		nodeShape = shape;
	}
	
	public void initializeNodeMapping() {
		initializeNodeMapping(nodeShape);
	}
	
	public void initializeNodeMapping(final long[] shape) {
		nodeAxes.clear();
		for(int i = 0; i < shape.length; i++) {
			nodeAxes.add( null );
		}
	}
	
	public long[] getNodeShape() {
		return nodeShape;
	}
	
	public void setName( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public int[] getMapping() {
		return ArrayHelper.toIntArray( finalMapping );
	}
	
	public void generateMapping() {
		
		finalMapping.clear();
		
		for ( int i = 0; i < nodeShape.length; i++ ) {
			finalMapping.add(getNodeDimByDatasetDim( i ));
		}
				
		//if a dimension is not set, assign an unused dimension
		ArrayHelper.replaceNegativeIndicesWithUnusedIndices( finalMapping );
	}
	
	public AxisType getDimType( final int dim ) {
		if ( dataset.numDimensions() > dim ) { 
			return dataset.axis( dim ).type(); 
		}
		return null;
	}
	
	public Dataset getDataset() {
		return dataset;
	}
	
	public AxisType getLargestDatasetDim() {
		// Get the largest dimension and its size
		AxisType largestDim = null;
		long largestSize = 0;
		for ( int d = 0; d < getDataset().numDimensions(); d++ ) {
			final long dimSize = getDataset().dimension( d );
			if ( getDimType( d ).isXY() && dimSize > largestSize ) {
				largestSize = dimSize;
				largestDim = getDimType( d );
			}
		}
		return largestDim;
	}
	
	public void initMapping() {
		if ( !isMappingInitialized() ) {
			setMappingDefaults();
		}
	}
	
	public void setMappingDefaults() {
		System.out.println( "setmappingdefaults" );
		setMappingInitialized( true );
		int tensorDimCount = nodeShape.length;
		if ( tensorDimCount == 5 ) {
			setNodeAxis( 0, Axes.TIME );
			setNodeAxis( 1, Axes.Z );
			setNodeAxis( 2, Axes.Y );
			setNodeAxis( 3, Axes.X );
			setNodeAxis( 4, Axes.CHANNEL );
		} else {
			if ( tensorDimCount == 4 ) {
				setNodeAxis( 1, Axes.Y );
				setNodeAxis( 2, Axes.X );
				if ( dataset.dimension( Axes.Z ) > 1 ) {
					setNodeAxis( 0, Axes.Z );
					if ( dataset.dimension( Axes.CHANNEL ) > 1 ) {
						setNodeAxis( 3, Axes.CHANNEL );
					} else {
						setNodeAxis( 3, Axes.TIME );
					}
				} else {
					if ( dataset.dimension( Axes.CHANNEL ) > 1 ) {
						setNodeAxis( 0, Axes.CHANNEL );
						setNodeAxis( 3, Axes.TIME );
					} else {
						setNodeAxis( 0, Axes.TIME );
						setNodeAxis( 3, Axes.CHANNEL );
					}
				}
			}
		}
		printMapping();
	}

	public void resetMapping() {
		nodeAxes.clear();
	}
	
	public int numDimensions() {
		return dataset.numDimensions();
	}

	public long getDatasetDimSize( final int knownAxesIndex ) {
		if ( availableAxes.length > knownAxesIndex ) { 
			return dataset.dimension( dataset.dimensionIndex( availableAxes[knownAxesIndex])); 
		}
		return 1;
	}

	public String getDatasetDimName( final AxisType axis ) {
		return axis.getLabel().substring( 0, 1 );
	}

	public String getDatasetDimName( final int knownAxesIndex ) {
		if ( availableAxes.length > knownAxesIndex ) {
			return getDatasetDimName( availableAxes[knownAxesIndex] );
		}
		return "not found";
	}

	public boolean removeZFromMapping(Dataset initialDataset) {
		System.out.println( "REMOVING Z" );
		int datasetIndex = initialDataset.dimensionIndex( Axes.Z );
		if(!reducedZ) {
			if ( nodeAxes.contains( Axes.Z ) ) {
				nodeAxes.remove( Axes.Z );
				nodePadding.remove( datasetIndex );
				reducedZ = true;
			}
			printMapping();
		}
		return reducedZ;
	}

	public void permuteInputAxes( final int dim1, final int dim2 ) {
		final CalibratedAxis a1 = dataset.axis( dim1 );
		final CalibratedAxis a2 = dataset.axis( dim2 );
		dataset.setAxis( a1, dim2 );
		dataset.setAxis( a2, dim1 );
	}
	
	public void setMappingInitialized( final boolean mappingInitialized ) {
		this.mappingInitialized = mappingInitialized;
	}
	
	public boolean isMappingInitialized() {
		return mappingInitialized;
	}

	public void printMapping() {
		if(dataset != null) {
    		AxisType[] axes = new AxisType[dataset.numDimensions()];
    		for(int i = 0; i < dataset.numDimensions(); i++) {
    			axes[i] = dataset.axis( i ).type();
    		}
    		System.out.println( "datasetAxes:" + Arrays.toString( axes ) );
		}
		System.out.println( "nodeAxes:" + nodeAxes.toString() );
		System.out.println( "mapping:" + finalMapping.toString() );
		System.out.println( "--------------" );
	}
	
	public Long getDatasetDimSizeFromNodeDim( final int nodeDim ) {
		Integer index = getDatasetDimIndexByTFIndex( nodeDim );
		if ( index != null ) {
			return (long) index;
		}
		return ( long ) 1;
	}

	public Integer getDatasetDimIndexByTFIndex( final int nodeDim ) {
		if ( nodeAxes.size() > nodeDim ) {
			AxisType axis = nodeAxes.get( nodeDim );
			if(dataset.axis( axis ) != null) {
				return dataset.dimensionIndex( axis );
			}
		}
		return null;
	}

	public String getDatasetDimNameByTFIndex( final int nodeDim ) {
		if ( nodeAxes.size() > nodeDim) {
			return getDatasetDimName( nodeAxes.get( nodeDim ) ); 
		}
		return null;
	}

	public Integer getNodeDimByDatasetDim( final int datasetDim ) {
		if ( dataset.numDimensions() > datasetDim ) {
			return nodeAxes.indexOf( dataset.axis( datasetDim ).type() );
		}
		return -1;
	}

	public Integer getNodeDimByAxis( final AxisType type ) {
		if ( nodeAxes.contains( type ) ) { return nodeAxes.indexOf( type ); }
		return -1;
	}

	public AxisType getAxisByDatasetDim( final int datasetDim ) {
		if ( dataset.numDimensions() > datasetDim ) { 
			return dataset.axis( datasetDim ).type(); 
		}
		return null;
	}
	
	public void setNodeAxis( final int index, final AxisType axisType ) {
		if(nodeAxes.size() > index) {
			nodeAxes.set( index, axisType );
		}
	}
	
	public AxisType getNodeAxis( final int index ) {
		return nodeAxes.get( index );
	}
	
	public void setNodeAxisByKnownAxesIndex( final int nodeDim, final int knownAxesIndex ) {
		if ( knownAxesIndex < availableAxes.length && nodeDim < nodeAxes.size() ) {
			nodeAxes.set( nodeDim, availableAxes[ knownAxesIndex ] );
		}
	}

	public void setMapping( AxisType[] newmapping ) {
		setMappingInitialized( true );
		nodeAxes.clear();
		for ( int i = 0; i < newmapping.length; i++ ) {
			nodeAxes.add( newmapping[ i ] );
		}
		System.out.println( "Setting manual mapping:" );
		printMapping();		
	}
	
	public List<AxisType> getNodeAxes() {
		return nodeAxes;
	}

	public long getLargestDimSize() {
		return dataset.dimension( getLargestDatasetDim() );
	}

	public int getLargestDimIndex() {
		// Get the largest dimension and its size
		int largestDim = -1;
		long largestSize = 0;
		for ( int i = 0; i < dataset.numDimensions(); i++) {
			final long dimSize = getDataset().dimension( i );
			if (dataset.axis( i ).type().isXY() && dimSize > largestSize ) {
				largestSize = dimSize;
				largestDim = i;
			}
		}
		return largestDim;
	}
	
	public long[] getNodePadding() {
		return nodePadding.stream().mapToLong(l -> l).toArray();
	}
	
	public void setNodePadding(long[] padding) {
		nodePadding.clear();
		for(int i = 0; i < padding.length; i++) {
			nodePadding.add( padding[i] );
		}
	}

	public long getDatasetDimension( int index ) {
		return dataset.dimension( index );
	}

	public long getDatasetDimension( AxisType axisType ) {
		return dataset.dimension( axisType );
	}

}

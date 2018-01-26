package mpicbg.csbd.network;

import java.util.ArrayList;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

import mpicbg.csbd.util.ArrayHelper;

public class ImageNode {
	
	AxisType[] availableAxes = { Axes.X, Axes.Y, Axes.Z, Axes.TIME, Axes.CHANNEL };
	
	private String name;
	private List< AxisType > datasetAxes = new ArrayList<>();
	private List< AxisType > nodeAxes = new ArrayList<>();
	private long[] nodeShape;
	private List< Long > nodePadding = new ArrayList<>();
	private List<Integer > finalMapping = new ArrayList<>();
	private Dataset dataset;
	private boolean mappingInitialized = false;
	private boolean reducedZ = false;
	
	public ImageNode() {
		
	}
	
	public void initialize(Dataset dataset) {
		this.dataset = dataset;
		datasetAxes.clear();
		for(int i = 0; i < dataset.numDimensions(); i++){
			datasetAxes.add( null );
		}
		for ( int i = 0; i < availableAxes.length; i++ ) {
			final AxisType axis = availableAxes[ i ];
			int index = dataset.dimensionIndex( axis );
			if(index >= 0)
				datasetAxes.set( dataset.dimensionIndex( axis ), axis );
		}
	}
	
	public void setNodeShape(long[] shape) {
		nodeShape = shape;
	}
	
	public void initializeNodeMapping() {
		initializeNodeMapping(nodeShape);
	}
	
	public void initializeNodeMapping(long[] shape) {
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
	
//	public void setMapping( int[] mapping ) {
//		this.mapping = mapping;
//	}
	
//	public int[] getMapping() {
////		return mapping;
//		int[] res = new int[nodeShape.length];
//		int i = 0;
//		for(AxisType axis : nodeAxes) {
//			res[i] = datasetAxes.indexOf( axis );
//			i++;
//		}
//		return res;
//	}
	
	public int[] getMapping() {
		return ArrayHelper.toIntArray( finalMapping );
	}
	
	public void generateMapping() {
		
		finalMapping.clear();
		
		for ( int i = 0; i < nodeShape.length; i++ ) {
			finalMapping.add(getTfIndexByDatasetDim( i ));
		}
				
		//if a dimension is not set, assign an unused dimension
		ArrayHelper.replaceNegativeIndicesWithUnusedIndices( finalMapping );
	}
	
	public AxisType getDimType( final int dim ) {
		if ( datasetAxes.size() > dim ) { 
			return datasetAxes.get( dim ); 
		}
		return null;
	}
	
	public Dataset getDataset() {
		return dataset;
	}
	
	public AxisType getLargestDim() {
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
	
	public void setMappingDefaults(int tensorDimCount) {
		System.out.println( "setmappingdefaults" );
		setMappingInitialized( true );
		if ( tensorDimCount == 5 ) {
			nodeAxes.set( 0, Axes.TIME );
			nodeAxes.set( 1, Axes.Z );
			nodeAxes.set( 2, Axes.Y );
			nodeAxes.set( 3, Axes.X );
			nodeAxes.set( 4, Axes.CHANNEL );
		} else {
			if ( tensorDimCount == 4 ) {
				nodeAxes.set( 2, Axes.Y );
				nodeAxes.set( 3, Axes.X );
				if ( dataset.dimension( Axes.Z ) > 1 ) {
					nodeAxes.set( 1, Axes.Z );
					if ( dataset.dimension( Axes.CHANNEL ) > 1 ) {
						nodeAxes.set( 4, Axes.CHANNEL );
					} else {
						nodeAxes.set( 4, Axes.TIME );
					}
				} else {
					if ( dataset.dimension( Axes.CHANNEL ) > 1 ) {
						nodeAxes.set( 1, Axes.CHANNEL );
						nodeAxes.set( 4, Axes.TIME );
					} else {
						nodeAxes.set( 1, Axes.TIME );
						nodeAxes.set( 4, Axes.CHANNEL );
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
		return datasetAxes.size();
	}

	public long getDatasetDimSize( final int knownAxesIndex ) {
		if ( availableAxes.length > knownAxesIndex ) { 
			return dataset.dimension( datasetAxes.indexOf( availableAxes[knownAxesIndex])); 
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

	public boolean removeZFromMapping() {
		System.out.println( "REMOVING Z" );
		printMapping();
		if(!reducedZ) {
			if ( nodeAxes.contains( Axes.Z ) ) {
				nodeAxes.remove( Axes.Z );
				nodePadding.remove( datasetAxes.indexOf( Axes.Z ) );
				datasetAxes.remove( Axes.Z );
				reducedZ = true;
			}
			printMapping();
		}
		return reducedZ;
	}

	public void permuteInputAxes( final int dim1, final int dim2 ) {
		final AxisType a1 = datasetAxes.get( dim1 );
		final AxisType a2 = datasetAxes.get( dim2 );
		datasetAxes.set( dim2, a1 );
		datasetAxes.set( dim1, a2 );
	}
	
	public void setMappingInitialized( final boolean mappingInitialized ) {
		this.mappingInitialized = mappingInitialized;
	}
	
	public boolean isMappingInitialized() {
		return mappingInitialized;
	}

	public void printMapping() {
		System.out.println( "axes:" + datasetAxes.toString() );
		System.out.println( "axesTF:" + nodeAxes.toString() );
		System.out.println( "mapping:" + finalMapping.toString() );
		System.out.println( "--------------" );
	}
	
	public Long getDatasetDimSizeFromTFIndex( final int tfIndex5D ) {
		Integer index = getDatasetDimIndexByTFIndex( tfIndex5D );
		if ( index != null ) {
			return (long) index;
		}
		return ( long ) 1;
	}

	public Integer getDatasetDimIndexByTFIndex( final int tfIndex5D ) {
		if ( nodeAxes.size() > tfIndex5D ) {
			AxisType axis = nodeAxes.get( tfIndex5D );
			if(datasetAxes.contains( axis )) {
				return datasetAxes.indexOf( axis );
			}
		}
		return null;
	}

	public String getDatasetDimNameByTFIndex( final int tfIndex5D ) {
		if ( nodeAxes.size() > tfIndex5D) {
			return getDatasetDimName( nodeAxes.get( tfIndex5D ) ); 
		}
		return null;
	}

	public Integer getTfIndexByDatasetDim( final int datasetDim ) {
		if ( datasetAxes.size() > datasetDim ) { 
			return nodeAxes.indexOf( datasetAxes.get( datasetDim ) ); 
		}
		return -1;
	}

	public Integer getTfIndexByDimType( final AxisType type ) {
		if ( nodeAxes.contains( type ) ) { return nodeAxes.indexOf( type ); }
		return -1;
	}

	public AxisType getDimTypeByDatasetDim( final int datasetDim ) {
		if ( datasetAxes.size() > datasetDim ) { 
			return datasetAxes.get( datasetDim ); 
		}
		return null;
	}
	
	public void setTFMapping( final int index, final AxisType axisType ) {
		nodeAxes.set( index, axisType );
	}
	
	public AxisType getTFMapping( final int index ) {
		return nodeAxes.get( index );
	}
	
	public void setTFMappingByKnownAxesIndex( final int tfIndex5D, final int knownAxesIndex ) {
		if ( knownAxesIndex < availableAxes.length && tfIndex5D < nodeAxes.size() ) {
			nodeAxes.set( tfIndex5D, availableAxes[ knownAxesIndex ] );
		}
	}

	public void setMapping( AxisType[] newmapping ) {
		resetMapping();
		for ( int i = 0; i < newmapping.length; i++ ) {
			setTFMapping( i, newmapping[ i ] );
		}
		printMapping();		
	}
	
	public List<AxisType> getNodeAxes() {
		return nodeAxes;
	}
	
	public List<AxisType> getDatasetAxes() {
		return datasetAxes;
	}

	public long getLargestDimSize() {
		return dataset.dimension( getLargestDim() );
	}

	public int getLargestDimIndex() {
		// Get the largest dimension and its size
		int largestDim = -1;
		long largestSize = 0;
		int i = 0;
		for ( AxisType axis : datasetAxes) {
			final long dimSize = getDataset().dimension( axis );
			if (axis.isXY() && dimSize > largestSize ) {
				largestSize = dimSize;
				largestDim = i;
			}
			i++;
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
		return dataset.dimension( datasetAxes.get( index ) );
	}

}

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
package mpicbg.csbd.network.tensorflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

import org.tensorflow.framework.TensorInfo;

import mpicbg.csbd.util.DatasetHelper;

public class DatasetTensorBridge {

	private final Dataset dataset;

//	AxisType[] axes = Axes.knownTypes();
	AxisType[] axes = { Axes.X, Axes.Y, Axes.Z, Axes.TIME, Axes.CHANNEL };
	private Map< AxisType, Integer > axisDataset;
	private Map< AxisType, Integer > axisTF;
	private Map< AxisType, Long > axisSize;

	private boolean mappingInitialized = false;
	private boolean reducedZ = false;

	public DatasetTensorBridge( final Dataset image ) {

		//check if image has unknown dimensions. if yes, assign unused dimensions
		DatasetHelper.assignUnknownDimensions( image );

		dataset = image;

		axisDataset = new HashMap<>();
		axisTF = new HashMap<>();
		axisSize = new HashMap<>();

		for ( int i = 0; i < axes.length; i++ ) {
			final AxisType axis = axes[ i ];
			axisDataset.put( axis, image.dimensionIndex( axis ) );
			axisTF.put( axis, -1 );
			axisSize.put( axis, image.dimension( axis ) );
		}
		System.out.println( "DatasetTensorBridge initialized" );
		printMapping();

	}

	@Override
	public DatasetTensorBridge clone() {
		final DatasetTensorBridge _clone = new DatasetTensorBridge( dataset );
		_clone.axisTF = ( Map< AxisType, Integer > ) ( ( HashMap ) this.axisTF ).clone();
		_clone.axisDataset = ( Map< AxisType, Integer > ) ( ( HashMap ) this.axisDataset ).clone();
		_clone.axisSize = ( Map< AxisType, Long > ) ( ( HashMap ) this.axisSize ).clone();
		return _clone;
	}

	public Long getDatasetDimSizeFromTFIndex( final int tfIndex5D ) {
		if ( axisTF.containsValue(
				tfIndex5D ) ) { return axisSize.get( getKeyByValue( axisTF, tfIndex5D ) ); }
		return ( long ) 1;
	}

	public Integer getDatasetDimIndexByTFIndex( final int tfIndex5D ) {
		if ( axisTF.containsValue(
				tfIndex5D ) ) { return axisDataset.get( getKeyByValue( axisTF, tfIndex5D ) ); }
		return null;
	}

	public String getDatasetDimNameByTFIndex( final int tfIndex5D ) {
		if ( axisTF.containsValue(
				tfIndex5D ) ) { return getDatasetDimName( getKeyByValue( axisTF, tfIndex5D ) ); }
		return null;
	}

	public Integer getTfIndexByDatasetDim( final int datasetDim ) {
		if ( axisDataset.containsValue(
				datasetDim ) ) { return axisTF.get( getKeyByValue( axisDataset, datasetDim ) ); }
		return -1;
	}

	public Integer getTfIndexByDimType( final AxisType type ) {
		if ( axisTF.containsKey( type ) ) { return axisTF.get( type ); }
		return -1;
	}

	public AxisType getDimTypeByDatasetDim( final int datasetDim ) {
		if ( axisDataset.containsValue(
				datasetDim ) ) { return getKeyByValue( axisDataset, datasetDim ); }
		return null;
	}

	public int getOutputDimByInputDim( final int datasetDim ) {
		if ( reducedZ && axisDataset.containsKey( Axes.Z ) ) {
			if ( datasetDim >= axisDataset.get( Axes.Z ) ) { return datasetDim - 1; }
		}
		return datasetDim;
	}

	public boolean isMappingInitialized() {
		return mappingInitialized;
	}

	public void setMappingInitialized( final boolean mappingInitialized ) {
		this.mappingInitialized = mappingInitialized;
	}

	public void resetMapping() {
		axisTF.clear();
	}

	public void setTFMappingByKnownAxesIndex( final int tfIndex5D, final int knownAxesIndex ) {
		if ( knownAxesIndex < axes.length ) {
			axisTF.put( axes[ knownAxesIndex ], tfIndex5D );
		}
	}

	public void setTFMapping( final int index, final AxisType axisType ) {
		axisTF.put( axisType, index );
	}

	public int numDimensions() {
		return axes.length;
	}

	public long getDatasetDimSize( final int knownAxesIndex ) {
		if ( axes.length > knownAxesIndex ) { return axisSize.get(
				axes[ knownAxesIndex ] ); }
		return 1;
	}

	public String getDatasetDimName( final AxisType axis ) {
		return axis.getLabel().substring( 0, 1 );
	}

	public String getDatasetDimName( final int knownAxesIndex ) {
		return getDatasetDimName( axes[ knownAxesIndex ] );
	}

	public void printMapping() {
		System.out.println( "--------------" );
		System.out.println( "axisDataset:" + axisDataset.toString() );
		System.out.println( "axisSize:" + axisSize.toString() );
		System.out.println( "axisTF:" + axisTF.toString() );
		System.out.println( "--------------" );
	}

	public boolean removeZFromMapping() {
		printMapping();
		if ( axisTF.containsKey( Axes.Z ) ) {
			final int tfindex = axisTF.get( Axes.Z );
			if ( tfindex >= 0 ) {
				axisTF.put( Axes.Z, -1 );
				reducedZ = true;
				for ( int i = tfindex + 1; i < numDimensions(); i++ ) {
					if ( axisTF.containsValue( i ) ) {
						axisTF.put( getKeyByValue( axisTF, i ), i - 1 );
					}
				}
			}
		}
		printMapping();
		return reducedZ;
	}

	public static < T, E > Set< T > getKeysByValue( final Map< T, E > map, final E value ) {
		final Set< T > keys = new HashSet<>();
		for ( final Entry< T, E > entry : map.entrySet() ) {
			if ( Objects.equals( value, entry.getValue() ) ) {
				keys.add( entry.getKey() );
			}
		}
		return keys;
	}

	public static < T, E > T getKeyByValue( final Map< T, E > map, final E value ) {
		for ( final Entry< T, E > entry : map.entrySet() ) {
			if ( Objects.equals( value, entry.getValue() ) ) { return entry.getKey(); }
		}
		return null;
	}

	public void permuteInputAxes( final int dim1, final int dim2 ) {
		final AxisType a1 = getKeyByValue( axisDataset, dim1 );
		final AxisType a2 = getKeyByValue( axisDataset, dim2 );
		axisDataset.put( a1, dim2 );
		axisDataset.put( a2, dim1 );
	}

}

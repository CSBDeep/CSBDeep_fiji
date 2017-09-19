package mpicbg.csbd.tensorflow;

import net.imagej.Dataset;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

import org.tensorflow.Tensor;

import mpicbg.csbd.normalize.Normalizer;

public class DefaultDatasetConverter implements DatasetConverter {

	@Override
	public Dataset tensorToDataset( Tensor output_t, DatasetTensorBridge bridge ) {
		
		if(output_t != null){
			/*
			 * create 5D array from output tensor, unused dimensions will
			 * have size 1
			 */
			final float[][][][][] outputarr = bridge.createTFArray5D( output_t );

			for ( int i = 0; i < output_t.numDimensions(); i++ ) {
				System.out.println( "output dim " + i + ": " + output_t.shape()[ i ] );
			}

			if ( output_t.numDimensions() == bridge.getInitialInputTensorShape().numDimensions() - 1 ) {
				//model reduces dim by 1
				//assume z gets reduced -> move it to front and ignore first dimension
				/*
				 * model reduces dim by 1
				 * assume z gets reduced -> move it to front and ignore
				 * first dimension
				 */
				System.out.println( "model reduces dimension, z dimension reduction assumed" );
				bridge.removeZFromMapping();
			}

			// .. :-/
			if ( output_t.numDimensions() == 5 ) {
				output_t.copyTo( outputarr );
			} else {
				if ( output_t.numDimensions() == 4 ) {
					output_t.copyTo( outputarr[ 0 ] );
				} else {
					if ( output_t.numDimensions() == 3 ) {
						output_t.copyTo( outputarr[ 0 ][ 0 ] );
					}
				}
			}

			return arrayToDataset( outputarr, output_t.shape(), bridge );
	
		}
	
		return null;
	}
	
	protected Dataset arrayToDataset( final float[][][][][] outputarr, final long[] shape, DatasetTensorBridge bridge ) {

		final Dataset img_out = bridge.createDatasetFromTFDims( shape );

		//write ouput dataset and undo normalization

		final Cursor< RealType< ? > > cursor = img_out.localizingCursor();
		while ( cursor.hasNext() ) {
			final int[] pos = { 0, 0, 0, 0, 0 };
			final RealType< ? > val = cursor.next();
			for ( int i = 0; i < pos.length; i++ ) {
				final int imgIndex = bridge.getDatasetDimIndexByTFIndex( i );
				if ( imgIndex >= 0 ) {
					pos[ i ] = cursor.getIntPosition( imgIndex );
				}
			}
//			System.out.println("pos " + pos[0] + " " + pos[1] + " " + pos[2] + " " + pos[3] + " " + pos[4]);
			val.setReal( outputarr[ pos[ 0 ] ][ pos[ 1 ] ][ pos[ 2 ] ][ pos[ 3 ] ][ pos[ 4 ] ] );

		}

		return img_out;

	}

	@Override
	public Tensor datasetToTensor( Dataset image, DatasetTensorBridge bridge, Normalizer normalizer ) {
		return arrayToTensor( datasetToArray( image, bridge, normalizer ), bridge );
	}
	

	protected float[][][][][] datasetToArray( final Dataset d, DatasetTensorBridge bridge, Normalizer normalizer ) {

		final float[][][][][] inputarr = bridge.createTFArray5D();

		final int[] lookup = new int[ 5 ];
		for ( int i = 0; i < lookup.length; i++ ) {
			lookup[ i ] = bridge.getDatasetDimIndexByTFIndex( i );
		}
		/*
		 * create 5D array from dataset (unused dimensions get size 1)
		 */

		//copy input data to array

		final Cursor< RealType< ? > > cursor = d.localizingCursor();
		if ( normalizer.isActive() ) {
			
			while ( cursor.hasNext() ) {
				final float val = cursor.next().getRealFloat();
				final int[] pos = { 0, 0, 0, 0, 0 };
				for ( int i = 0; i < pos.length; i++ ) {
					if ( lookup[ i ] >= 0 ) {
						pos[ i ] = cursor.getIntPosition( lookup[ i ] );
					}
				}
				inputarr[ pos[ 0 ] ][ pos[ 1 ] ][ pos[ 2 ] ][ pos[ 3 ] ][ pos[ 4 ] ] = normalizer.normalize(val);

			}
		} else {
			while ( cursor.hasNext() ) {
				final float val = cursor.next().getRealFloat();
				final int[] pos = { 0, 0, 0, 0, 0 };
				for ( int i = 0; i < pos.length; i++ ) {
					if ( lookup[ i ] >= 0 ) {
						pos[ i ] = cursor.getIntPosition( lookup[ i ] );
					}
				}
				inputarr[ pos[ 0 ] ][ pos[ 1 ] ][ pos[ 2 ] ][ pos[ 3 ] ][ pos[ 4 ] ] = val;
			}
		}

		return inputarr;
	}

	protected Tensor arrayToTensor( final float[][][][][] array, DatasetTensorBridge bridge ) {
		if ( bridge.getInitialInputTensorShape().numDimensions() == 4 ) { return Tensor.create(
				array[ 0 ] ); }
		return Tensor.create( array );
	}


}

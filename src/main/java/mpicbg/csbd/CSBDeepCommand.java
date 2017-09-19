/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;

import net.imagej.Dataset;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.io.http.HTTPLocation;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

public class CSBDeepCommand< T extends RealType< T > > {

	@Parameter( visibility = ItemVisibility.MESSAGE )
	protected String header;

	@Parameter( label = "input data", type = ItemIO.INPUT, initializer = "processDataset" )
	protected Dataset input;

	@Parameter
	protected TensorFlowService tensorFlowService;
	
	@Parameter
	protected LogService log;

	@Parameter
	protected UIService uiService;

	@Parameter( type = ItemIO.OUTPUT )
	protected Dataset outputImage;

	@Parameter( visibility = ItemVisibility.MESSAGE )
	protected String normtext = "Normalization";
//    @Parameter(label = "Normalize image")
	protected boolean normalizeInput = true;
	@Parameter
	protected float percentileBottom = 0.1f;
	@Parameter
	protected float percentileTop = 0.9f;
	@Parameter
	protected float min = 0;
	@Parameter
	protected float max = 100;
	@Parameter( label = "Clamp normalization" )
	protected boolean clamp = true;

	protected float percentileBottomVal, percentileTopVal;
	
	protected String modelfileUrl;
	protected String modelName;
	protected String modelfileName;
	protected String inputNodeName;
	protected String outputNodeName;

	protected SignatureDef sig;

	protected Graph graph;
	protected SavedModelBundle model;
	protected DatasetTensorBridge bridge;
	protected boolean hasSavedModel = true;
	protected boolean processedDataset = false;
	
	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	protected static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	public CSBDeepCommand() {
		System.loadLibrary( "tensorflow_jni" );
	}

	protected boolean loadModelInputShape( final String inputName ) {

//		System.out.println("loadModelInputShape");

		if ( getGraph() != null ) {
			final Operation input_op = getGraph().operation( inputName );
			if ( input_op != null ) {
				bridge.setInputTensorShape( input_op.output( 0 ).shape() );
				bridge.setMappingDefaults();
				return true;
			}
			System.out.println( "input node with name " + inputName + " not found" );
		}
		return false;
	}

	/*
	 * model can be imported via graphdef or savedmodel, depending on that the
	 * execution graph has different origins
	 */
	protected Graph getGraph() {
		if ( hasSavedModel && ( model == null ) ) { return null; }
		return hasSavedModel ? model.graph() : graph;
	}

	/** Executed whenever the {@link #input} parameter changes. */
	protected void processDataset() {

		if ( !processedDataset ) {
			if ( input != null ) {
				bridge = new DatasetTensorBridge( input );
				processedDataset = true;
			}
		}

	}
	

	/*
	 * model can be imported via graphdef or savedmodel
	 */
	protected boolean loadGraph() throws MalformedURLException, URISyntaxException {

//		System.out.println("loadGraph");

		final HTTPLocation source = new HTTPLocation( modelfileUrl );
		hasSavedModel = false;
		try {
			graph = tensorFlowService.loadGraph( source, modelName, modelfileName );
		} catch ( TensorFlowException | IOException e ) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	protected void modelChanged() {

//		System.out.println("modelChanged");

		processDataset();

		if ( input == null ) { return; }

		try {
			if ( loadGraph() ) {

				if ( hasSavedModel ) {
					// Extract names from the model signature.
					// The strings "input", "probabilities" and "patches" are meant to be
					// in sync with the model exporter (export_saved_model()) in Python.
					try {
						sig = MetaGraphDef.parseFrom( model.metaGraphDef() ).getSignatureDefOrThrow(
								DEFAULT_SERVING_SIGNATURE_DEF_KEY );
					} catch ( final InvalidProtocolBufferException e ) {
//					e.printStackTrace();
						hasSavedModel = false;
					}
					if ( sig != null && sig.isInitialized() ) {
						if ( sig.getInputsCount() > 0 ) {
							inputNodeName = sig.getInputsMap().keySet().iterator().next();
						}
						if ( sig.getOutputsCount() > 0 ) {
							outputNodeName = sig.getOutputsMap().keySet().iterator().next();
						}
					}
				}

				loadModelInputShape( inputNodeName );
				
			}
		} catch ( MalformedURLException | URISyntaxException exc ) {
			exc.printStackTrace();
		}
	}
	
	public void run() {
		
		if ( input == null ) { return; }
		modelChanged();

		if ( bridge != null) {
			if ( bridge.getInitialInputTensorShape() != null ) {
				if ( !bridge.isMappingInitialized() ) {
					bridge.setMappingDefaults();
				}
			}
		}
		_run();
	}
	
	public void runWithMapping(int[] mapping){
		
		if ( input == null ) { return; }
		modelChanged();

		if ( bridge != null) {
			if ( bridge.getInitialInputTensorShape() != null ) {
				for(int i = 0; i < mapping.length; i++){
					bridge.setMapping( i, mapping[i] );
				}
			}
		}
		_run();
	}

	private void _run() {

		if ( normalizeInput ) {
			final float[] ps =
					percentiles( input, new float[] { percentileBottom, percentileTop } );
			percentileBottomVal = ps[ 0 ];
			percentileTopVal = ps[ 1 ];
			testNormalization();
		}

		try (
				final Tensor image = arrayToTensor( datasetToArray( input ) );) {
			outputImage = executeGraph( getGraph(), image );
			if ( outputImage != null ) {
				outputImage.setName( "CSBDeepened_" + input.getName() );
				uiService.show( outputImage );
			}
		}

//		uiService.show(arrayToDataset(datasetToArray(input)));

	}

	private void testNormalization() {
		final Dataset dcopy = ( Dataset ) input.copy();
		final Cursor< RealType< ? > > cursor = dcopy.cursor();
		System.out.println( "percentiles: " + percentileBottomVal + " -> " + percentileTopVal );
		final float factor = ( max - min ) / ( percentileTopVal - percentileBottomVal );
		System.out.println( "factor: " + factor );
		if ( clamp ) {
			while ( cursor.hasNext() ) {
				final float val = cursor.next().getRealFloat();
				cursor.get().setReal(
						Math.max(
								min,
								Math.min( max, ( val - percentileBottomVal ) * factor + min ) ) );
			}
		} else {
			while ( cursor.hasNext() ) {
				final float val = cursor.next().getRealFloat();
				cursor.get().setReal( Math.max( 0, ( val - percentileBottomVal ) * factor + min ) );
			}
		}
		dcopy.setName( "normalized_" + input.getName() );
		uiService.show( dcopy );
	}

	/*
	 * runs graph on input tensor
	 * converts result tensor to dataset
	 */
	private Dataset executeGraph( final Graph g, final Tensor image ) {

		System.out.println( "executeInceptionGraph" );

		try (
				Session s = new Session( g );) {

//			int size = s.runner().feed(inputNodeName, image).fetch(outputNodeName).run().size();
//			System.out.println("output array size: " + size);

			Tensor output_t = null;

			/*
			 * check if keras_learning_phase node has to be set
			 */
			if ( graph.operation( "dropout_1/keras_learning_phase" ) != null ) {
				final Tensor learning_phase = Tensor.create( false );
				try {
					/*
					 * execute graph
					 */
					final Tensor output_t2 = s.runner().feed( inputNodeName, image ).feed(
							"dropout_1/keras_learning_phase",
							learning_phase ).fetch( outputNodeName ).run().get( 0 );
					output_t = output_t2;
				} catch ( final Exception e ) {
					e.printStackTrace();
				}
			} else {
				try {
					/*
					 * execute graph
					 */
					final Tensor output_t2 = s.runner().feed( inputNodeName, image ).fetch(
							outputNodeName ).run().get( 0 );
					output_t = output_t2;
				} catch ( final Exception e ) {
					e.printStackTrace();
				}
			}

			if ( output_t != null ) {
				System.out.println(
						"Output tensor with " + output_t.numDimensions() + " dimensions" );

				if ( output_t.numDimensions() == 0 ) {
					showError( "Output tensor has no dimensions" );
					return null;
				}

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

				return arrayToDataset( outputarr, output_t.shape() );
			}
			return null;

		} catch ( final Exception e ) {
			System.out.println( "could not create output dataset" );
			e.printStackTrace();
		}
		return null;
	}
	
	protected static float[] percentiles( final Dataset d, final float[] percentiles ) {
		final Cursor< RealType< ? > > cursor = d.cursor();
		int items = 1;
		int i = 0;
		for ( ; i < d.numDimensions(); i++ ) {
			items *= d.dimension( i );
		}
		final float[] values = new float[ items ];
		i = 0;
		while ( cursor.hasNext() ) {
			cursor.fwd();
			values[ i ] = cursor.get().getRealFloat();
			i++;
		}

		Util.quicksort( values );

		final float[] res = new float[ percentiles.length ];
		for ( i = 0; i < percentiles.length; i++ ) {
			res[ i ] = values[ Math.min(
					values.length - 1,
					Math.max( 0, Math.round( ( values.length - 1 ) * percentiles[ i ] ) ) ) ];
		}

		return res;
	}

	protected float[][][][][] datasetToArray( final Dataset d ) {

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
		if ( normalizeInput ) {
			final float factor = ( max - min ) / ( percentileTopVal - percentileBottomVal );
			if ( clamp ) {
				while ( cursor.hasNext() ) {
					final float val = cursor.next().getRealFloat();
					final int[] pos = { 0, 0, 0, 0, 0 };
					for ( int i = 0; i < pos.length; i++ ) {
						if ( lookup[ i ] >= 0 ) {
							pos[ i ] = cursor.getIntPosition( lookup[ i ] );
						}
					}
					inputarr[ pos[ 0 ] ][ pos[ 1 ] ][ pos[ 2 ] ][ pos[ 3 ] ][ pos[ 4 ] ] =
							Math.max(
									min,
									Math.min( max, ( val - percentileBottomVal ) * factor + min ) );

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
					inputarr[ pos[ 0 ] ][ pos[ 1 ] ][ pos[ 2 ] ][ pos[ 3 ] ][ pos[ 4 ] ] =
							Math.max( 0, ( val - percentileBottomVal ) * factor + min );

				}
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

	protected Tensor arrayToTensor( final float[][][][][] array ) {
		if ( bridge.getInitialInputTensorShape().numDimensions() == 4 ) { return Tensor.create(
				array[ 0 ] ); }
		return Tensor.create( array );
	}

	protected Dataset arrayToDataset( final float[][][][][] outputarr, final long[] shape ) {

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

	public void showError( final String errorMsg ) {
		JOptionPane.showMessageDialog(
				null,
				errorMsg,
				"Error",
				JOptionPane.ERROR_MESSAGE );
	}

}

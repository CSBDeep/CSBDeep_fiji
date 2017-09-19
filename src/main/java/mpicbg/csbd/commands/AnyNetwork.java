/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd.commands;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.ui.MappingDialog;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Any network", headless = true )
public class AnyNetwork< T extends RealType< T > > implements Command, Cancelable {

	@Parameter( visibility = ItemVisibility.MESSAGE )
	private final String header = "This command removes noise from your images.";

	@Parameter( label = "input data", type = ItemIO.INPUT, initializer = "processDataset" )
	private Dataset input;

	@Parameter( label = "Import model", callback = "modelChanged", initializer = "modelInitialized", persist = false )
	private File modelfile;
	private final String modelfileKey = "modelfile-anynetwork";

	@Parameter( label = "Input node name", callback = "inputNodeNameChanged", initializer = "inputNodeNameChanged" )
	private String inputNodeName = "input";

	@Parameter( label = "Output node name", persist = false )
	private String outputNodeName = "output";

	@Parameter( label = "Adjust image <-> tensorflow mapping", callback = "openTFMappingDialog" )
	private Button changeTFMapping;

	@Parameter
	private TensorFlowService tensorFlowService;

	@Parameter
	private mpicbg.csbd.tensorflow.TensorFlowService tensorFlowService2;

	@Parameter
	private LogService log;

	@Parameter
	private UIService uiService;

	@Parameter
	private OpService opService;

	@Parameter
	private PrefService prefService;

	@Parameter( type = ItemIO.OUTPUT )
	private Dataset outputImage;

	@Parameter( visibility = ItemVisibility.MESSAGE )
	private String normtext = "Normalization";
//    @Parameter(label = "Normalize image")
	private boolean normalizeInput = true;
	@Parameter
	private float percentileBottom = 0.1f;
	@Parameter
	private float percentileTop = 0.9f;
	@Parameter
	private float min = 0;
	@Parameter
	private float max = 100;
	@Parameter( label = "Clamp normalization" )
	private boolean clamp = true;

	private float percentileBottomVal, percentileTopVal;

	private Graph graph;
	private SavedModelBundle model;
	private SignatureDef sig;
	private DatasetTensorBridge bridge;
	private boolean hasSavedModel = true;
	private boolean processedDataset = false;

	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	private static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	public AnyNetwork() {
		/*
		 * a failed attempt to get GPU support
		 */
//    	try {
//			System.loadLibrary("tensorflow_jni");
//		} catch (IOException exc) {
//			System.err.println("cannot import tensorflow gpu lib");
//			exc.printStackTrace();
//		}
	}

	/*
	 * model can be imported via graphdef or savedmodel
	 */
	protected boolean loadGraph() {

//		System.out.println("loadGraph");

		if ( modelfile == null ) {
			System.out.println( "Cannot load graph from null File" );
			return false;
		}

		final FileLocation source = new FileLocation( modelfile );
		hasSavedModel = true;
		try {
			model = tensorFlowService.loadModel( source, modelfile.getName() );
		} catch ( TensorFlowException | IOException e ) {
			try {
				graph = tensorFlowService2.loadGraph( modelfile );
//				graph = tensorFlowService.loadGraph(source, "", "");
				hasSavedModel = false;
			} catch ( final IOException e2 ) {
				e2.printStackTrace();
				return false;
			}
		}
		return true;
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

	/** Executed whenever the {@link #modelfile} parameter is initialized. */
	protected void modelInitialized() {
		final String p_modelfile = prefService.get( modelfileKey, "" );
		if ( p_modelfile != "" ) {
			modelfile = new File( p_modelfile );
		}
		modelChanged();
	}

	/** Executed whenever the {@link #modelfile} parameter changes. */
	protected void modelChanged() {

//		System.out.println("modelChanged");

		if ( modelfile != null ) {
			savePreferences();
		}

		processDataset();

		if ( input == null ) { return; }

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

			inputNodeNameChanged();
		}
	}

	/** Executed whenever the {@link #inputNodeName} parameter changes. */
	protected void inputNodeNameChanged() {

//		System.out.println("inputNodeNameChanged");

		loadModelInputShape( inputNodeName );

		if ( bridge != null ) {
			if ( bridge.getInitialInputTensorShape() != null ) {
				if ( !bridge.isMappingInitialized() ) {
					bridge.setMappingDefaults();
				}
			}
		}

	}

	protected void openTFMappingDialog() {

		processDataset();

		if ( bridge.getInitialInputTensorShape() == null ) {
			modelChanged();
		}

		MappingDialog.create( bridge, sig );
	}

	@Override
	public void run() {

		savePreferences();

		if ( input == null ) { return; }

		if ( getGraph() == null ) {
			modelChanged();
		}

		if ( bridge != null && bridge.getInitialInputTensorShape() == null ) {
			inputNodeNameChanged();
		}

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

	private void savePreferences() {
		prefService.put( modelfileKey, modelfile.getAbsolutePath() );

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

	private static float[] percentiles( final Dataset d, final float[] percentiles ) {
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

	private float[][][][][] datasetToArray( final Dataset d ) {

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

	private Tensor arrayToTensor( final float[][][][][] array ) {
		if ( bridge.getInitialInputTensorShape().numDimensions() == 4 ) { return Tensor.create(
				array[ 0 ] ); }
		return Tensor.create( array );
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

	private Dataset arrayToDataset( final float[][][][][] outputarr, final long[] shape ) {

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

	/**
	 * This main function serves for development purposes.
	 * It allows you to run the plugin immediately out of
	 * your integrated development environment (IDE).
	 *
	 * @param args
	 *            whatever, it's ignored
	 * @throws Exception
	 */
	public static void main( final String... args ) throws Exception {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch( args );

		// ask the user for a file to open
		final File file = ij.ui().chooseFile( null, "open" );

		if ( file != null && file.exists() ) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open( file.getAbsolutePath() );

			// show the image
			ij.ui().show( dataset );

			// invoke the plugin
			ij.command().run( AnyNetwork.class, true );
		}

	}

	public void showError( final String errorMsg ) {
		JOptionPane.showMessageDialog(
				null,
				errorMsg,
				"Error",
				JOptionPane.ERROR_MESSAGE );
	}

	@Override
	public boolean isCanceled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void cancel( final String reason ) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getCancelReason() {
		// TODO Auto-generated method stub
		return null;
	}

}

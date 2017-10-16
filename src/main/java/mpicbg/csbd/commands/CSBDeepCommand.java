/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd.commands;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;

import net.imagej.Dataset;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.io.http.HTTPLocation;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.tensorflow.DatasetConverter;
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.tensorflow.DefaultDatasetConverter;

public class CSBDeepCommand< T extends RealType< T > > extends PercentileNormalizer {

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
	
	@Parameter
	protected ThreadService threadService;

	@Parameter( type = ItemIO.OUTPUT )
	protected Dataset outputImage;

	@Parameter( label="Number of tiles", min="1" )
	protected int nTiles = 1;

	@Parameter( label="Overlap between tiles", min="0", stepSize="16")
	protected int overlap = 32;

	protected String modelFileUrl;
	protected String modelName;
	protected String inputNodeName = "input";
	protected String outputNodeName = "output";

	protected SignatureDef sig;

	protected SavedModelBundle model;
	protected DatasetTensorBridge bridge;
	protected boolean processedDataset = false;

	private final DatasetConverter<T> datasetConverter = new DefaultDatasetConverter<>();

	private static final String MODEL_TAG = "serve";
	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	protected static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	public CSBDeepCommand() {
		System.out.println("CSBDeepCommand constructor");
		try {
			System.loadLibrary( "tensorflow_jni" );
		} catch (UnsatisfiedLinkError e) {
			System.out.println("Couldn't load tensorflow from library path. Using CPU version from jar file.");
		}
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
	 * model can be imported via savedmodel
	 */
	protected boolean loadModel() throws MalformedURLException, URISyntaxException {

//		System.out.println("loadGraph");

		final HTTPLocation source = new HTTPLocation( modelFileUrl );
		try {
			model = tensorFlowService.loadModel( source, modelName, MODEL_TAG );
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
			if ( loadModel() ) {

				// Extract names from the model signature.
				// The strings "input", "probabilities" and "patches" are meant to be
				// in sync with the model exporter (export_saved_model()) in Python.
				try {
					sig = MetaGraphDef.parseFrom( model.metaGraphDef() ).getSignatureDefOrThrow(
							DEFAULT_SERVING_SIGNATURE_DEF_KEY );
				} catch ( final InvalidProtocolBufferException e ) {
//					e.printStackTrace();
				}
				if ( sig != null && sig.isInitialized() ) {
					if ( sig.getInputsCount() > 0 ) {
						inputNodeName = sig.getInputsMap().keySet().iterator().next();
						if(bridge != null){
							bridge.setInputTensorShape( sig.getInputsOrThrow( inputNodeName ).getTensorShape() );
						}
					}
					if ( sig.getOutputsCount() > 0 ) {
						outputNodeName = sig.getOutputsMap().keySet().iterator().next();
						if(bridge != null){
							bridge.setOutputTensorShape( sig.getOutputsOrThrow( outputNodeName ).getTensorShape() );
						}
					}
					if (bridge != null && !bridge.isMappingInitialized() ) {
						bridge.setMappingDefaults();
					}
				}

			}
		} catch ( MalformedURLException | URISyntaxException exc ) {
			exc.printStackTrace();
		}
	}

	public void run() {

		if ( input == null ) { return; }
		modelChanged();
		_run();
	}

	public void runWithMapping( final int[] mapping ) {

		if ( input == null ) { return; }
		modelChanged();

		if ( bridge != null ) {
			if ( bridge.getInitialInputTensorShape() != null ) {
				for ( int i = 0; i < mapping.length; i++ ) {
					bridge.setMapping( i, mapping[ i ] );
				}
			}
		}
		_run();
	}

	private void _run() {

		prepareNormalization( input );
		testNormalization( input, uiService );
		
		RandomAccessibleInterval<FloatType> tiledPrediction = TiledPredictionUtil.tiledPrediction((RandomAccessibleInterval) input.getImgPlus(),
				nTiles, 32, overlap, datasetConverter, bridge, this, model, sig, inputNodeName, outputNodeName, threadService);
		if(tiledPrediction != null){
			uiService.show(tiledPrediction);			
		}
		// TODO remove comment and add tiled prediction
//		try (
//				final Tensor image = datasetConverter.datasetToTensor( input, bridge, this );) {
//			outputImage = datasetConverter.tensorToDataset(
//					TensorFlowRunner.executeGraph(
//							getGraph(),
//							image,
//							inputNodeName,
//							outputNodeName ),
//					bridge );
//			if ( outputImage != null ) {
//				outputImage.setName( "CSBDeepened_" + input.getName() );
//				uiService.show( outputImage );
//			}
//		}

//		uiService.show(arrayToDataset(datasetToArray(input)));

	}

	public void showError( final String errorMsg ) {
		JOptionPane.showMessageDialog(
				null,
				errorMsg,
				"Error",
				JOptionPane.ERROR_MESSAGE );
	}

}

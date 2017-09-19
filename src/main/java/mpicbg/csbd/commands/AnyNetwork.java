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
import net.imglib2.type.numeric.RealType;

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
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.tensorflow.DatasetConverter;
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.tensorflow.DefaultDatasetConverter;
import mpicbg.csbd.tensorflow.TensorFlowRunner;
import mpicbg.csbd.ui.MappingDialog;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Any network", headless = true )
public class AnyNetwork< T extends RealType< T > > extends PercentileNormalizer
		implements
		Command,
		Cancelable {

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

	private Graph graph;
	private SavedModelBundle model;
	private SignatureDef sig;
	private DatasetTensorBridge bridge;
	private boolean hasSavedModel = true;
	private boolean processedDataset = false;
	private final DatasetConverter datasetConverter = new DefaultDatasetConverter();

	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	private static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	public AnyNetwork() {
		System.loadLibrary( "tensorflow_jni" );
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

		TensorFlowRunner.loadModelInputShape( getGraph(), inputNodeName, bridge );

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

		prepareNormalization( input );
		testNormalization( input, uiService );

		try (
				final Tensor image = datasetConverter.datasetToTensor( input, bridge, this );) {
			outputImage = datasetConverter.tensorToDataset(
					TensorFlowRunner.executeGraph(
							getGraph(),
							image,
							inputNodeName,
							outputNodeName ),
					bridge );
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

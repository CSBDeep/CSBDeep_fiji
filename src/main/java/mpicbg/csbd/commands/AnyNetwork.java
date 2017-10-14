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
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

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
import org.tensorflow.SavedModelBundle;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.tensorflow.DatasetConverter;
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.tensorflow.DefaultDatasetConverter;
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

	@Parameter( label = "Import model (.zip)", callback = "modelChanged", initializer = "modelInitialized", persist = false )
	private File modelFile;
	private final String modelFileKey = "modelfile-anynetwork";

	private String inputNodeName = "input";
	private String outputNodeName = "output";

	@Parameter( label = "Adjust image <-> tensorflow mapping", callback = "openTFMappingDialog" )
	private Button changeTFMapping;

	@Parameter( label="Number of tiles", min="1" )
	protected int nTiles = 1;

	@Parameter( label="Overlap between tiles", min="0", stepSize="16")
	protected int overlap = 32;

	@Parameter
	private TensorFlowService tensorFlowService;

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

	private SavedModelBundle model;
	private SignatureDef sig;
	private DatasetTensorBridge bridge;
	private boolean processedDataset = false;
	private final DatasetConverter<T> datasetConverter = new DefaultDatasetConverter<>();


	// Same as the tag used in export_saved_model in the Python code.
	private static final String MODEL_TAG = "serve";
	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	private static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	public AnyNetwork() {
		try {
			System.loadLibrary( "tensorflow_jni" );
		} catch (UnsatisfiedLinkError e) {
			System.out.println("Couldn't load tensorflow from library path. Using CPU version from jar file.");
		}
	}

	/*
	 * model can be imported via savedmodel
	 */
	protected boolean loadModel() {

//		System.out.println("loadGraph");

		if ( modelFile == null ) {
			System.out.println( "Cannot load graph from null File" );
			return false;
		}

		final FileLocation source = new FileLocation( modelFile );
		try {
			model = tensorFlowService.loadModel( source, source.getName(), MODEL_TAG );
		} catch ( TensorFlowException | IOException e ) {
			e.printStackTrace();
			return false;
		}
		return true;
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

	/** Executed whenever the {@link #modelFile} parameter is initialized. */
	protected void modelInitialized() {
		final String p_modelfile = prefService.get( modelFileKey, "" );
		if ( p_modelfile != "" ) {
			modelFile = new File( p_modelfile );
			modelChanged();
		}
	}
	
	/** Executed whenever the {@link #modelFile} parameter changes. */
	protected void modelChanged() {

//		System.out.println("modelChanged");

		if ( modelFile != null ) {
			savePreferences();
		}

		processDataset();

		if ( input == null ) { return; }

		if ( loadModel() ) {

			// Extract names from the model signature.
			// The strings "input", "probabilities" and "patches" are meant to be
			// in sync with the model exporter (export_saved_model()) in Python.
			try {
				sig = MetaGraphDef.parseFrom( model.metaGraphDef() ).getSignatureDefOrThrow(
						DEFAULT_SERVING_SIGNATURE_DEF_KEY );
			} catch ( final InvalidProtocolBufferException e ) {
				e.printStackTrace();
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

		if ( model == null ) {
			modelChanged();
		}

		prepareNormalization( input );
		testNormalization( input, uiService );

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
		RandomAccessibleInterval<FloatType> tiledPrediction =
				TiledPredictionUtil.tiledPrediction((RandomAccessibleInterval) input.getImgPlus(),
						nTiles,
						32,
						overlap,
						datasetConverter,
						bridge,
						this,
						model,
						sig,
						inputNodeName,
						outputNodeName);
		uiService.show(tiledPrediction);
//		uiService.show(arrayToDataset(datasetToArray(input)));

	}

	private void savePreferences() {
		prefService.put( modelFileKey, modelFile.getAbsolutePath() );
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

		
//		// Tests
//		final ImgFactory< UnsignedByteType > factory = new ArrayImgFactory<>();
//		final Img< UnsignedByteType > img = IO.openImgs( "/Users/bw/Pictures/Lenna.png", factory, new UnsignedByteType() ).get( 0 ).getImg();
//		
//		ImageJFunctions.show(img);
//		
//		// Create a tiled view on it
//		TiledView<UnsignedByteType> tiledView = TiledView.createFromBlocksPerDim(img, new long[]{ 3, 3, 1 });
//		
//		// Take a middle part
//		TiledViewRandomAccess<UnsignedByteType> randomAccess = tiledView.randomAccess();
//		randomAccess.setPosition(1,0);
//		randomAccess.setPosition(1,1);
//		
//		RandomAccessibleInterval<UnsignedByteType> part = randomAccess.get();
//		RandomAccessibleInterval<UnsignedByteType> expanded = Views.expand(part, new OutOfBoundsMirrorFactory<>(Boundary.DOUBLE), new long[]{ 20, 20, 0 });
//		//RandomAccessibleInterval<UnsignedByteType> expanded = Views.expandBorder(part, new long[]{ 20, 20, 0 });
//		
//		ImageJFunctions.show(expanded);
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

/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd.commands;

import com.google.protobuf.InvalidProtocolBufferException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.Cancelable;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.io.http.HTTPLocation;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.ui.CSBDeepProgress;

public class CSBDeepCommand< T extends RealType< T > > extends PercentileNormalizer< T >
		implements
		Cancelable,
		Initializable,
		ActionListener {

	@Parameter( label = "input data", type = ItemIO.INPUT, initializer = "processDataset" )
	protected Dataset input;

	@Parameter
	protected TensorFlowService tensorFlowService;

	@Parameter
	protected LogService log;

	@Parameter
	protected UIService uiService;

	@Parameter
	protected DatasetService datasetService;

	@Parameter( type = ItemIO.OUTPUT )
	protected Dataset outputImage;

	@Parameter( label = "Number of tiles", min = "1" )
	protected int nTiles = 1;

	@Parameter( label = "Overlap between tiles", min = "0", stepSize = "16" )
	protected int overlap = 32;

	protected String modelFileUrl;
	protected String modelName;
	protected String inputNodeName = "input";
	protected String outputNodeName = "output";

	protected SignatureDef sig;

	protected SavedModelBundle model;
	protected DatasetTensorBridge bridge;
	protected boolean processedDataset = false;
	private boolean useTensorFlowGPU = true;

	CSBDeepProgress progressWindow;

	ExecutorService pool = Executors.newSingleThreadExecutor();
	List< TiledPrediction > predictions = new ArrayList<>();

	private static final String MODEL_TAG = "serve";
	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	protected static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	@Override
	public void initialize() {
		System.out.println( "CSBDeepCommand constructor" );
		try {
			System.loadLibrary( "tensorflow_jni" );
		} catch ( UnsatisfiedLinkError e ) {
			useTensorFlowGPU = false;
			System.out.println(
					"Couldn't load tensorflow from library path. Using CPU version from jar file." );
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

		File file = new File( modelFileUrl );
		Location source;
		if ( !file.exists() ) {
			source = new HTTPLocation( modelFileUrl );
		} else {
			source = new FileLocation( file );
		}
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
						if ( bridge != null ) {
							bridge.setInputTensor( sig.getInputsOrThrow( inputNodeName ) );
						}
					}
					if ( sig.getOutputsCount() > 0 ) {
						outputNodeName = sig.getOutputsMap().keySet().iterator().next();
						if ( bridge != null ) {
							bridge.setOutputTensor( sig.getOutputsOrThrow( outputNodeName ) );
						}
					}
					if ( bridge != null && !bridge.isMappingInitialized() ) {
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
		initGui();
		initModel();
		progressWindow.setStepStart( CSBDeepProgress.STEP_PREPROCRESSING );
		executeModel( normalizeInput() );
	}

	public void setMapping( final AxisType[] mapping ) {
		if ( bridge != null ) {
			if ( bridge.getInputTensorInfo() != null ) {
				bridge.resetMapping();
				for ( int i = 0; i < mapping.length; i++ ) {
					bridge.setTFMapping( i, mapping[ i ] );
				}
				bridge.printMapping();
			}
		}
	}

	public void runWithMapping( final AxisType[] mapping ) {

		if ( input == null ) { return; }
		modelChanged();

		setMapping( mapping );
		initGui();
		initModel();
		progressWindow.setStepStart( CSBDeepProgress.STEP_PREPROCRESSING );
		executeModel( normalizeInput() );
	}

	protected void initGui() {
		progressWindow = CSBDeepProgress.create( useTensorFlowGPU, false );
		progressWindow.getCancelBtn().addActionListener( this );

	}

	protected void initModel() {
		progressWindow.setStepStart( CSBDeepProgress.STEP_LOADMODEL );

		progressWindow.addLog( "Loading model " + modelName + ".. " );

		if ( input == null ) { return; }

		if ( model == null ) {
			modelChanged();
			if ( model == null ) {
				progressWindow.setCurrentStepFail();
				return;
			}
		}

		progressWindow.setCurrentStepDone();

	}

	protected RandomAccessibleInterval< FloatType > normalizeInput() {
		progressWindow.addLog( "Preparing normalization.. " );
		prepareNormalization( ( IterableInterval ) input.getImgPlus() );

		progressWindow.addLog(
				"Displaying normalized test image.." );
		testNormalization( input, uiService );

		progressWindow.addLog(
				"Normalize (" + percentileBottom + " - " + percentileTop + " -> " + min + " - " + max + "] .. " );

		RandomAccessibleInterval< FloatType > normalizedInput = normalizeImage(
				( RandomAccessibleInterval ) input.getImgPlus() );
		return normalizedInput;

	}

	protected void executeModel( RandomAccessibleInterval< FloatType > modelInput ) {

		List< RandomAccessibleInterval< FloatType > > result = null;
		try {
			TiledPrediction prediction =
					new TiledPrediction( modelInput, bridge, model, progressWindow, nTiles, 32, overlap );
			predictions.add( prediction );
			result = pool.submit( prediction ).get();
		} catch ( ExecutionException exc ) {
			progressWindow.setCurrentStepFail();
			exc.printStackTrace();
		} catch ( InterruptedException exc ) {
			progressWindow.addError( "Process canceled." );
			progressWindow.setCurrentStepFail();
		}

		if ( result != null ) {
			if ( result.size() > 0 ) {
				progressWindow.addLog( "Displaying result image.." );
				uiService.show( "result", result.get( 0 ) );
				progressWindow.addLog( "Displaying control image.." );
				uiService.show( "control", result.get( 1 ) );
				progressWindow.addLog( "All done!" );
				progressWindow.setCurrentStepDone();
			}
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
	public void cancel( String reason ) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getCancelReason() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if ( e.getSource().equals( progressWindow.getCancelBtn() ) ) {
			for ( TiledPrediction prediction : predictions ) {
				prediction.cancel();
			}
			pool.shutdownNow();
			progressWindow.addError( "Process canceled." );
			progressWindow.setCurrentStepFail();
		}
	}

}

/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2018 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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
package mpicbg.csbd.commands;

import com.google.protobuf.InvalidProtocolBufferException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

import org.scijava.Cancelable;
import org.scijava.Initializable;
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
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.ui.CSBDeepProgress;
import mpicbg.csbd.ui.MappingDialog;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Generic network", headless = true )
public class GenericNetwork< T extends RealType< T > > extends PercentileNormalizer< T >
		implements
		Command,
		Cancelable,
		Initializable,
		ActionListener {

	private static final String OUTPUT_NAME = "output_";

	@Parameter( label = "input data", type = ItemIO.INPUT, initializer = "processDataset" )
	private Dataset input;

	@Parameter( visibility = ItemVisibility.MESSAGE )
	protected String normtext = "Normalization";
	@Parameter
	protected boolean _normalizeInput = true;
	@Parameter
	protected float _percentileBottom = 0.03f;
	@Parameter
	protected float _percentileTop = 0.998f;

	protected float _min = 0;
	protected float _max = 1;

	@Parameter( label = "Clamp normalization" )
	protected boolean _clamp = false;

	@Parameter( label = "Import model (.zip)", callback = "modelChanged", initializer = "modelInitialized", persist = false )
	private File modelFile;
	private final String modelFileKey = "modelfile-anynetwork";

	private String inputNodeName = "input";
	private String outputNodeName = "output";

	@Parameter( label = "Adjust image <-> tensorflow mapping", callback = "openTFMappingDialog" )
	private Button changeTFMapping;

	@Parameter( label = "Number of tiles", min = "1" )
	protected int nTiles = 1;

	@Parameter( label = "Overlap between tiles", min = "0", stepSize = "16" )
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

	@Parameter
	private DatasetService datasetService;

	@Parameter( type = ItemIO.OUTPUT )
	protected List< Dataset > resultDatasets;

	private SavedModelBundle model;
	private SignatureDef sig;
	private DatasetTensorBridge bridge;
	private boolean processedDataset = false;
	private boolean useTensorFlowGPU = true;
	protected int blockMultiple = 32;

	CSBDeepProgress progressWindow;

	ExecutorService pool = Executors.newCachedThreadPool();

	// Same as the tag used in export_saved_model in the Python code.
	private static final String MODEL_TAG = "serve";
	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	private static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";

	@Override
	public void initialize() {
		try {
			System.loadLibrary( "tensorflow_jni" );
		} catch ( final UnsatisfiedLinkError e ) {
			useTensorFlowGPU = false;
			System.out.println(
					"Couldn't load tensorflow from library path. Using CPU version from jar file." );
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
	}

	protected void openTFMappingDialog() {

		processDataset();

		if ( bridge.getInputTensorInfo() == null ) {
			modelChanged();
		}

		MappingDialog.create( bridge );
	}

	@Override
	public void run() {

		progressWindow = CSBDeepProgress.create( useTensorFlowGPU );

		progressWindow.getCancelBtn().addActionListener( this );

		progressWindow.setStepStart( CSBDeepProgress.STEP_LOADMODEL );

		savePreferences();

		progressWindow.addLog( "Loading model " + modelFile.getName() + ".. " );

		if ( input == null ) { return; }

		if ( model == null ) {
			modelChanged();
			if ( model == null ) {
				progressWindow.setCurrentStepFail();
				return;
			}
		}

		progressWindow.setCurrentStepDone();
		progressWindow.setStepStart( CSBDeepProgress.STEP_PREPROCRESSING );

		progressWindow.addLog( "Preparing normalization.. " );

		normalizeInput = _normalizeInput;
		percentileBottom = _percentileBottom;
		percentileTop = _percentileTop;
		min = _min;
		max = _max;
		clamp = _clamp;

		prepareNormalization( ( IterableInterval ) input.getImgPlus() );

		progressWindow.addLog(
				"Displaying normalized test image.." );
		testNormalization( input, uiService );

		progressWindow.addLog(
				"Normalize (" + percentileBottom + " - " + percentileTop + " -> " + min + " - " + max + "] .. " );

		final RandomAccessibleInterval< FloatType > normalizedInput = normalizeImage(
				( RandomAccessibleInterval ) input.getImgPlus() );

		executeModel( normalizedInput );

	}

	private void executeModel( final RandomAccessibleInterval< FloatType > normalizedInput ) {

		List< RandomAccessibleInterval< FloatType > > result = null;
		try {
			result = pool
					.submit(
							new TiledPrediction( normalizedInput, bridge, model, progressWindow, nTiles, blockMultiple, overlap ) )
					.get();
		} catch ( final ExecutionException exc ) {
			exc.printStackTrace();

			// We expect it to be an out of memory exception and
			// try it again with more tiles.
			nTiles *= 2;
			// Check if the number of tiles is to large already
			if ( Arrays.stream( Intervals.dimensionsAsLongArray( normalizedInput ) ).max().getAsLong() / nTiles < blockMultiple ) {
				progressWindow.setCurrentStepFail();
				return;
			}
			progressWindow.addError( "Out of memory exception occurred. Trying with " + nTiles + " tiles..." );
			progressWindow.addRounds( 1 );
			progressWindow.setNextRound();
			executeModel( normalizedInput );
			return;

		} catch ( final InterruptedException exc ) {
			progressWindow.addError( "Process canceled." );
			progressWindow.setCurrentStepFail();
		}
		resultDatasets = new ArrayList<>();
		if ( result != null ) {
			// TODO Remove i < 2 when the project network isn't expanded in the channel dimension anymore
			for ( int i = 0; i < result.size() && i < 2; i++ ) {
				progressWindow.addLog( "Displaying result image " + i + ".." );
				resultDatasets.add( wrapIntoDataset( OUTPUT_NAME + i, result.get( i ) ) );
			}
		}
		if ( !resultDatasets.isEmpty() ) {
			progressWindow.addLog( "All done!" );
			progressWindow.setCurrentStepDone();
		} else {
			progressWindow.setCurrentStepFail();
		}
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
			ij.command().run( GenericNetwork.class, true );
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
		return false;
	}

	@Override
	public void cancel( final String reason ) {}

	@Override
	public String getCancelReason() {
		return null;
	}

	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( progressWindow.getCancelBtn() ) ) {
			pool.shutdownNow();
			progressWindow.setCurrentStepFail();
		}
	}

	/** TODO remove: copy from CSBDeepCommand */
	protected < U extends RealType< U > & NativeType< U > > Dataset wrapIntoDataset( final String name, final RandomAccessibleInterval< U > img ) {

		//TODO convert back to original format to be able to save and load it (float 32 bit does not load in Fiji)

		final Dataset dataset = datasetService.create( new ImgPlus<>( ImgView.wrap( img, new ArrayImgFactory<>() ) ) );
		dataset.setName( name );
		for ( int i = 0; i < dataset.numDimensions(); i++ ) {
			dataset.setAxis( input.axis( bridge.getOutputDimByInputDim( i ) ), i );
		}
		return dataset;
	}
}

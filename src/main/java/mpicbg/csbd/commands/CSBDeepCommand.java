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
package mpicbg.csbd.commands;

import com.google.protobuf.InvalidProtocolBufferException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
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

	protected static String[] OUTPUT_NAMES = { "result" };

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

	@Parameter( type = ItemIO.OUTPUT )
	protected List< Dataset > resultDatasets;

	protected String modelFileUrl;
	protected String modelName;
	protected String inputNodeName = "input";
	protected String outputNodeName = "output";
	protected int blockMultiple = 32;

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
		System.out.println( "Loading tensorflow jni from library path..." );
		try {
			System.loadLibrary( "tensorflow_jni" );
		} catch ( final UnsatisfiedLinkError e ) {
			useTensorFlowGPU = false;
			System.out.println(
					"Couldn't load tensorflow from library path:" );
			System.out.println( e.getMessage() );
			System.out.println( "If the problem is CUDA related. Make sure CUDA and cuDNN are in the LD_LIBRARY_PATH." );
			System.out.println( "The current library path is: LD_LIBRARY_PATH=" + System.getenv("LD_LIBRARY_PATH"));
			System.out.println( "Using CPU version from jar file." );
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

		final File file = new File( modelFileUrl );
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
		progressWindow = CSBDeepProgress.create( useTensorFlowGPU );
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
				"Normalize (" + percentileBottom + " - " + percentileTop + " -> " + min + " - " + max + "] .. " );

		final RandomAccessibleInterval< FloatType > normalizedInput = normalizeImage(
				( RandomAccessibleInterval ) input.getImgPlus() );

		return normalizedInput;

	}

	protected void executeModel( final RandomAccessibleInterval< FloatType > modelInput ) {

		List< RandomAccessibleInterval< FloatType > > result = null;
		try {
			final TiledPrediction prediction =
					new TiledPrediction( modelInput, bridge, model, progressWindow, nTiles, blockMultiple, overlap );
			predictions.add( prediction );
			result = pool.submit( prediction ).get();
		} catch ( final ExecutionException exc ) {
			exc.printStackTrace();

			// We expect it to be an out of memory exception and
			// try it again with more tiles.
			nTiles *= 2;
			// Check if the number of tiles is to large already
			if ( Arrays.stream( Intervals.dimensionsAsLongArray( modelInput ) ).max().getAsLong() / nTiles < blockMultiple ) {
				progressWindow.setCurrentStepFail();
				return;
			}
			progressWindow.addError( "Out of memory exception occurred. Trying with " + nTiles + " tiles..." );
			progressWindow.addRounds( 1 );
			progressWindow.setNextRound();
			executeModel( modelInput );
			return;

		} catch ( final InterruptedException exc ) {
			progressWindow.addError( "Process canceled." );
			progressWindow.setCurrentStepFail();
		}

		resultDatasets = new ArrayList<>();
		for ( int i = 0; i < result.size() && i < OUTPUT_NAMES.length; i++ ) {
			progressWindow.addLog( "Displaying " + OUTPUT_NAMES[ i ] + " image.." );
			resultDatasets.add( wrapIntoDataset( OUTPUT_NAMES[ i ], result.get( i ) ) );
		}
		if ( !resultDatasets.isEmpty() ) {
			progressWindow.addLog( "All done!" );
			progressWindow.setCurrentStepDone();
		} else {
			progressWindow.setCurrentStepFail();
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
	public void cancel( final String reason ) {

	}

	@Override
	public String getCancelReason() {
		return null;
	}

	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( progressWindow.getCancelBtn() ) ) {

			//TODO this is not yet fully working. The tile that is currently computed does not stop.
			pool.shutdownNow();
			progressWindow.addError( "Process canceled." );
			progressWindow.setCurrentStepFail();
		}
	}

	protected static void printDim( final String title, final RandomAccessibleInterval< FloatType > img ) {
		final long[] dims = new long[ img.numDimensions() ];
		img.dimensions( dims );
		System.out.println( title + ": " + Arrays.toString( dims ) );
	}

	protected < U extends RealType< U > & NativeType< U > > Dataset wrapIntoDataset( final String name, final RandomAccessibleInterval< U > img ) {

		//TODO convert back to original format to be able to save and load it (float 32 bit does not load in Fiji)

		final Dataset dataset = datasetService.create( new ImgPlus<>( ImgView.wrap( img, new ArrayImgFactory<>() ) ) );
		dataset.setName( name );
		for ( int i = 0; i < dataset.numDimensions(); i++ ) {
			dataset.setAxis( input.axis( bridge.getOutputDimByInputDim( i ) ), i );
		}
		return dataset;
	}

	protected static void validateInput(
			final Dataset dataset,
			final String formatDesc,
			final OptionalLong... expectedDims ) throws IOException {
		if ( dataset.numDimensions() != expectedDims.length ) { throw new IOException( "Can not process " + dataset
				.numDimensions() + "D images.\nExpected format: " + formatDesc ); }
		for ( int i = 0; i < expectedDims.length; i++ ) {
			if ( expectedDims[ i ].isPresent() && expectedDims[ i ].getAsLong() != dataset
					.dimension( i ) ) { throw new IOException( "Can not process image. Dimension " + i + " musst be of size " + expectedDims[ i ]
							.getAsLong() + ".\nExpected format: " + formatDesc ); }
		}
	}
}

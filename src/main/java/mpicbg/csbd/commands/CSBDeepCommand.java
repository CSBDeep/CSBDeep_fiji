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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import net.imagej.display.DatasetView;
import net.imagej.display.DefaultDatasetView;
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
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.network.tensorflow.TensorFlowNetwork;
import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.prediction.TiledPrediction;
import mpicbg.csbd.ui.CSBDeepProgress;
import mpicbg.csbd.util.DatasetHelper;

public class CSBDeepCommand< T extends RealType< T > > extends PercentileNormalizer< T >
		implements
		Cancelable,
		Initializable,
		ActionListener {

	protected static String[] OUTPUT_NAMES = { "result" };

	/* extracted from the dataset view */
	protected Dataset input;

	@Parameter( label = "input data", type = ItemIO.INPUT, initializer = "processDataset" )
	protected DatasetView datasetView;

	@Parameter
	protected LogService log;

	@Parameter
	protected DatasetService datasetService;

	@Parameter( label = "Number of tiles", min = "1" )
	protected int nTiles = 8;

	@Parameter( label = "Overlap between tiles", min = "0", stepSize = "16" )
	protected int overlap = 32;

	@Parameter( type = ItemIO.BOTH, label = "result" )
	protected List< DatasetView > resultDatasets;

	protected String modelFileUrl;
	protected String modelName;
	protected String inputNodeName = "input";
	protected String outputNodeName = "output";
	protected int blockMultiple = 32;

	protected boolean processedDataset = false;

	CSBDeepProgress progressWindow;

	ExecutorService pool = Executors.newSingleThreadExecutor();
	List< TiledPrediction > predictions = new ArrayList<>();
	
	Network network = new TensorFlowNetwork();

	@Override
	public void initialize() {
		network.loadLibrary();
	}

	/** Executed whenever the {@link #input} parameter changes. */
	protected void processDataset() {
		
		input = datasetView.getData();
		DatasetHelper.assignUnknownDimensions( input );
		if(network.isInitialized()){
			network.setInput( input );
		}

	}
	
	protected void loadNetworkFile() throws FileNotFoundException {
		network.loadModel( modelFileUrl, modelName );
	}
	
	protected void initializeNetwork() {
		network.loadInputNode(inputNodeName, input);
		network.loadOutputNode(outputNodeName);
		network.initMapping();
	}
	
	protected boolean noInputData() {
		return input == null;
	}
	
	protected boolean noModel() {
		return !network.isInitialized();
	}

	protected void loadNetwork() {

		try {

			loadNetworkFile();
			initializeNetwork();

		} catch ( FileNotFoundException exc1 ) {

			exc1.printStackTrace();

		}

	}

	public void run() {

		if ( noInputData() )
			return;

		initGui();
		
		loadFinalModelStep();
		
		processDataset();
		
		progressWindow.setStepStart( CSBDeepProgress.STEP_PREPROCRESSING );
		executeModel( normalizeInput() );
	}

	public void setMapping( final AxisType[] mapping ) {
		if ( network.getInputNode() != null ) {
			network.getInputNode().setMapping( mapping );
		}
	}

	public void runWithMapping( final AxisType[] mapping ) {

		if ( noInputData() )
			return;
		
		initGui();
		
		loadFinalModelStep();
		
		processDataset();

		setMapping( mapping );
		
		progressWindow.setStepStart( CSBDeepProgress.STEP_PREPROCRESSING );
		executeModel( normalizeInput() );
	}

	protected void initGui() {
		progressWindow = CSBDeepProgress.create( network.isSupportingGPU() );
		progressWindow.getCancelBtn().addActionListener( this );

	}

	protected void loadFinalModelStep() {
		progressWindow.setStepStart( CSBDeepProgress.STEP_LOADMODEL );

		progressWindow.addLog( "Loading model " + modelName + ".. " );

		if ( noModel() ) {
			loadNetwork();
			if ( noModel() ) {
				progressWindow.setCurrentStepFail();
				return;
			}
		}

		progressWindow.setCurrentStepDone();

	}

	protected RandomAccessibleInterval< FloatType > normalizeInput() {

		progressWindow.addLog(
				"Normalize (" + percentileBottom + " - " + percentileTop + " -> " + min + " - " + max + "] .. " );

		return normalize(( RandomAccessibleInterval ) input.getImgPlus() );
	}

	protected void executeModel( final RandomAccessibleInterval< FloatType > modelInput ) {
		
		processDataset();

		List< RandomAccessibleInterval< FloatType > > result = null;
		try {
			final TiledPrediction prediction =
					new TiledPrediction( modelInput, network, progressWindow, nTiles, blockMultiple, overlap );
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

		if(resultDatasets == null) {
			resultDatasets = new ArrayList<>();
		}else {
			resultDatasets.clear();
		}
		
		if(result != null){
			for ( int i = 0; i < result.size() && i < OUTPUT_NAMES.length; i++ ) {
				progressWindow.addLog( "Displaying " + OUTPUT_NAMES[ i ] + " image.." );
				resultDatasets.add( wrapIntoDatasetView( OUTPUT_NAMES[ i ], result.get( i ) ) );
			}
			if ( !resultDatasets.isEmpty() ) {
				progressWindow.addLog( "All done!" );
				progressWindow.setCurrentStepDone();
			} else {
				progressWindow.setCurrentStepFail();
			}			
		}
	}

	public static void showError( final String errorMsg ) {
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

	protected < U extends RealType< U > & NativeType< U > > DatasetView wrapIntoDatasetView(
			final String name,
			final RandomAccessibleInterval< U > img ) {
		DefaultDatasetView resDatasetView = new DefaultDatasetView();
		Dataset d = wrapIntoDataset( name, img );
		resDatasetView.setContext( d.getContext() );
		resDatasetView.initialize( d );
		resDatasetView.rebuild();

		// Set LOT
		for ( int i = 0; i < datasetView.getColorTables().size(); i++ ) {
			resDatasetView.setColorTable( datasetView.getColorTables().get( i ), i );
		}
		return resDatasetView;
	}

	protected < U extends RealType< U > & NativeType< U > > Dataset wrapIntoDataset( final String name, final RandomAccessibleInterval< U > img ) {
		
		long[] imgdim = new long[img.numDimensions()];
		img.dimensions( imgdim );

		//TODO convert back to original format to be able to save and load it (float 32 bit does not load in Fiji)
		final Dataset dataset = datasetService.create( new ImgPlus<>( ImgView.wrap( img, new ArrayImgFactory<>() ) ) );
		dataset.setName( name );
		for ( int i = 0; i < dataset.numDimensions(); i++ ) {
			dataset.setAxis( input.axis( network.getOutputNode().getDimType( i ) ).get(), i );
		}
		// NB: Doesn't work somehow
//		int compositeChannelCount = input.getCompositeChannelCount();
//		dataset.setCompositeChannelCount( compositeChannelCount );
		return dataset;
	}

	public static void validateInput(
			final Dataset dataset,
			final String formatDesc,
			final OptionalLong... expectedDims ) throws IOException {
		DatasetHelper.validate(dataset, formatDesc, expectedDims);
	}
}

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import mpicbg.csbd.ui.CSBDeepProgress;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Deconvolution - Microtubules", headless = true )
public class NetTubulin< T extends RealType< T > > extends CSBDeepCommand< T >
		implements
		Command {

	private static final int BLOCK_MULTIPLE = 4;

	@Parameter( label = "Batch size", min = "1" )
	protected int batchSize = 10;

	@Override
	public void initialize() {

		super.initialize();

		modelFileUrl = "http://csbdeep.bioimagecomputing.com/model-tubulin.zip";
		modelName = "net_tubulin";

	}

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
			ij.command().run( NetTubulin.class, true );
		}

	}

	@Override
	public void run() {
		Exception prevException = null;
		try {
			try {
				validateInput(
						input,
						"3D image with dimension order X-Y-T",
						OptionalLong.empty(),
						OptionalLong.empty(),
						OptionalLong.empty() );
			} catch ( final IOException e ) {
				prevException = e;
				validateInput( input, "2D image with dimension order X-Y", OptionalLong.empty(), OptionalLong.empty() );
			}
			runModel();
		} catch ( final IOException e ) {
			showError( prevException.getMessage() + "\nOR\n" + e.getMessage() );
		}
	}

	private void runModel() {
		if ( input == null ) { return; }
		modelChanged();

		final AxisType[] mapping = { Axes.TIME, Axes.Y, Axes.X, Axes.CHANNEL };
		setMapping( mapping );

		initGui();

		initModel();
		progressWindow.setStepStart( CSBDeepProgress.STEP_PREPROCRESSING );

		// Normalize the input
		progressWindow.addLog( "Normalize input.. " );
		final RandomAccessibleInterval< FloatType > normalizedInput = normalizeInput();

		// Outputs
		final List< RandomAccessibleInterval< FloatType > > result = new ArrayList<>();

		runBatches( normalizedInput, result, normalizedInput.numDimensions() == 3 );

		resultDatasets = new ArrayList<>();
		for ( int i = 0; i < result.size() && i < OUTPUT_NAMES.length; i++ ) {
			progressWindow.addLog( "Displaying " + OUTPUT_NAMES[ i ] + " image.." );
			resultDatasets.add( wrapIntoDatasetView( OUTPUT_NAMES[ i ], Views.dropSingletonDimensions( result.get( i ) ) ) );
		}
		if ( !resultDatasets.isEmpty() ) {
			progressWindow.addLog( "All done!" );
			progressWindow.setCurrentStepDone();
		} else {
			progressWindow.setCurrentStepFail();
		}
	}

	private void runBatches(
			final RandomAccessibleInterval< FloatType > rotated,
			final List< RandomAccessibleInterval< FloatType > > result,
			final boolean useBatch ) {

		result.clear();

		try {

			TiledPrediction tiledPrediction;
			if ( useBatch ) {
				tiledPrediction = new BatchedTiledPrediction( rotated, bridge, model, progressWindow, nTiles, BLOCK_MULTIPLE, overlap, batchSize );
			} else {
				tiledPrediction = new TiledPrediction( rotated, bridge, model, progressWindow, nTiles, BLOCK_MULTIPLE, overlap );
			}
			tiledPrediction.setDropSingletonDims( false );
			result.addAll( pool.submit( tiledPrediction ).get() );

		} catch ( RejectedExecutionException | InterruptedException exc ) {
			return;
		} catch ( final ExecutionException exc ) {
			exc.printStackTrace();

			// We expect it to be an out of memory exception and
			// try it again with a smaller batch size.
			batchSize /= 2;
			// Check if the batch size is at 1 already
			if ( batchSize < 1 ) {
				progressWindow.setCurrentStepFail();
				return;
			}
			progressWindow.addError( "Out of memory exception occurred. Trying with batch size: " + batchSize );
			progressWindow.addRounds( 1 );
			progressWindow.setNextRound();
			runBatches( rotated, result, useBatch );
			return;
		}

	}
}

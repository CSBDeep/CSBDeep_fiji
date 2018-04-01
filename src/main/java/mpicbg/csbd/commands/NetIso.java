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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.tiling.BatchedTiling;
import mpicbg.csbd.util.task.DefaultOutputProcessor;
import mpicbg.csbd.util.task.InputProcessor;
import mpicbg.csbd.util.task.OutputProcessor;

@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Isotropic Reconstruction - Retina", headless = true )
public class NetIso< T extends RealType< T > > extends CSBDeepCommand< T > implements Command {

	@Parameter( label = "Scale Z", min = "1", max = "15" )
	protected float scale = 10.2f;

	@Parameter( label = "Batch size", min = "1" )
	protected int batchSize = 10;

	final ExecutorService pool = Executors.newWorkStealingPool();

	@Override
	public void initialize() {

		super.initialize();

		modelFileUrl = "http://csbdeep.bioimagecomputing.com/model-iso.zip";
		modelName = "net_iso";

	}

	@Override
	protected void initTiling() {
		final int batchDim = network.getInputNode().getDatasetDimIndexByTFIndex( 0 );
		final int channelDim = network.getInputNode().getDatasetDimensionIndex( Axes.CHANNEL );
		tiling = new BatchedTiling( nTiles, 4, overlap, batchSize, batchDim, channelDim );
	}

	@Override
	protected InputProcessor initInputProcessor() {
		return new IsoInputProcessor();
	}

	@Override
	protected OutputProcessor initOutputProcessor() {
		return new IsoOutputProcessor();
	}

	private class IsoInputProcessor extends DefaultTask implements InputProcessor {

		@Override
		public List< RandomAccessibleInterval< FloatType > > run( final Dataset input ) {

			setStarted();

			final List< RandomAccessibleInterval< FloatType > > output = new ArrayList<>();

			final RandomAccessibleInterval< FloatType > inputRai =
					( RandomAccessibleInterval< FloatType > ) input.getImgPlus();

			final int dimX = input.dimensionIndex( Axes.X );
			final int dimY = input.dimensionIndex( Axes.Y );
			final int dimZ = input.dimensionIndex( Axes.Z );

			final RandomAccessibleInterval< FloatType > upsampled =
					upsample( inputRai, dimZ, scale );

			printDim( "upsampled:", upsampled );

			final RandomAccessibleInterval< FloatType > rotated0 =
					Views.permute( upsampled, dimX, dimZ );

			final RandomAccessibleInterval< FloatType > rotated1 =
					Views.permute( rotated0, dimY, dimZ );

			final RandomAccessibleInterval< FloatType > rotated0_applied =
					ArrayImgs.floats( Intervals.dimensionsAsLongArray( rotated0 ) );
			final RandomAccessibleInterval< FloatType > rotated1_applied =
					ArrayImgs.floats( Intervals.dimensionsAsLongArray( rotated1 ) );
			copy( rotated0, rotated0_applied );
			copy( rotated1, rotated1_applied );

			output.add( rotated0_applied );
			output.add( rotated1_applied );

			printDim( "rotated0_applied", rotated0_applied );
			printDim( "rotated1_applied", rotated1_applied );

			setFinished();

			return output;

		}

	}

	@Override
	public void run() {
//		network.setDoDimensionReduction( true, Axes.CHANNEL );
		try {
			validateInput(
					datasetView.getData(),
					"4D image with dimension order X-Y-C-Z and two channels",
					OptionalLong.empty(),
					OptionalLong.empty(),
					OptionalLong.of( 2 ),
					OptionalLong.empty() );
			final AxisType[] mapping = { Axes.Z, Axes.Y, Axes.X, Axes.CHANNEL };
			setMapping( mapping );
			super.run();
		} catch ( final IOException e ) {
			showError( e.getMessage() );
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		pool.shutdown();
	}

	@Override
	protected boolean handleOutOfMemoryError() {
		batchSize /= 2;
		if ( batchSize < 1 ) { return false; }
		return true;
	}

	private class IsoOutputProcessor extends DefaultOutputProcessor {

		@Override
		public List< DatasetView > run(
				final List< RandomAccessibleInterval< FloatType > > result,
				final DatasetView datasetView,
				final Network network ) {
			setStarted();

			final List< DatasetView > output = new ArrayList<>();

			final RandomAccessibleInterval< FloatType > _result0 = result.get( 0 );
			final RandomAccessibleInterval< FloatType > _result1 = result.get( 1 );

			final List< RandomAccessibleInterval< FloatType > > result0 =
					splitByLastNodeDim( _result0, network );
			final List< RandomAccessibleInterval< FloatType > > result1 =
					splitByLastNodeDim( _result1, network );

			for ( int i =
					0; i + 1 < result0.size() && i + 1 < result1.size() && i / 2 < OUTPUT_NAMES.length; i +=
							2 ) {
				//prediction for ZY rotation
				RandomAccessibleInterval< FloatType > res0_pred =
						Views.stack( result0.get( i ), result0.get( i + 1 ) );

				//prediction for ZX rotation
				RandomAccessibleInterval< FloatType > res1_pred =
						Views.stack( result1.get( i ), result1.get( i + 1 ) );

				printDim( "res0_pred", res0_pred );
				printDim( "res1_pred", res1_pred );

				//force the CHANNEL dim back to its original location
				final int outputChannelDim =
						network.getOutputNode().getDatasetDimensionIndex( Axes.CHANNEL );
				for ( int dim = outputChannelDim + 1; dim < res1_pred.numDimensions(); dim++ ) {
					res0_pred = Views.permute( res0_pred, dim, dim - 1 );
					res1_pred = Views.permute( res1_pred, dim, dim - 1 );
				}

				printDim( "res0_pred", res0_pred );
				printDim( "res1_pred", res1_pred );

				final int dimX = datasetView.getData().dimensionIndex( Axes.X );
				final int dimY = datasetView.getData().dimensionIndex( Axes.Y );
				final int dimZ = datasetView.getData().dimensionIndex( Axes.Z );

				// rotate output stacks back
				res0_pred = Views.permute( res0_pred, dimX, dimZ );
				res1_pred = Views.permute( res1_pred, dimY, dimZ );
				res1_pred = Views.permute( res1_pred, dimX, dimZ );

				printDim( "res0_pred rotated back", res0_pred );
				printDim( "res1_pred rotated back", res1_pred );

				log( "Merge output stacks.." );

				// Calculate the geometric mean of the two predictions
				final RandomAccessibleInterval< FloatType > prediction =
						ArrayImgs.floats( Intervals.dimensionsAsLongArray( res0_pred ) );
				pointwiseGeometricMean(
						res0_pred,
						res1_pred,
						prediction );
				printDim( "prediction", prediction );

				output.add(
						wrapIntoDatasetView(
								OUTPUT_NAMES[ i / 2 ],
								prediction,
								datasetView,
								network ) );
			}

			setFinished();

			return output;
		}
	}

	/**
	 * Scales the given dimension by the given scale and uses linear
	 * interpolation for the missing values.
	 *
	 * NOTE: This method will return very fast because the scaling is not
	 * applied in this method. The scaling is only applied on access of pixel
	 * values.
	 *
	 * NOTE: The resulting dimension length will not match old_dimension_length
	 * * scale. But the min and max of the image are mapped to the scaled
	 * positions and used to define the new interval.
	 *
	 * @param normalizedInput
	 *            Input image
	 * @param dim
	 *            Dimension number to scale
	 * @param scale
	 *            Scale of the dimension
	 * @return The scaled image
	 */
	private < U extends NumericType< U > > RandomAccessibleInterval< U > upsample(
			final RandomAccessibleInterval< U > normalizedInput,
			final int dim,
			final float scale ) {
		final int n = normalizedInput.numDimensions();

		// Interpolate
		final RealRandomAccessible< U > interpolated =
				Views.interpolate(
						Views.extendBorder( normalizedInput ),
						new NLinearInterpolatorFactory<>() );

		// Affine transformation to scale the Z axis
		final double[] scales =
				IntStream.range( 0, n ).mapToDouble( i -> i == dim ? scale : 1 ).toArray();
		final AffineGet scaling = new Scale( scales );

		// Scale min and max to create an interval afterwards
		final double[] targetMin = new double[ n ];
		final double[] targetMax = new double[ n ];
		scaling.apply( Intervals.minAsDoubleArray( normalizedInput ), targetMin );
		scaling.apply( Intervals.maxAsDoubleArray( normalizedInput ), targetMax );

		// Apply the transformation
		final RandomAccessible< U > scaled = RealViews.affine( interpolated, scaling );
		return Views.interval(
				scaled,
				Arrays.stream( targetMin ).mapToLong( d -> ( long ) Math.ceil( d ) ).toArray(),
				Arrays.stream( targetMax ).mapToLong( d -> ( long ) Math.floor( d ) ).toArray() );
	}

	/**
	 * Copies one image into another. Used to apply the transformations.
	 *
	 * @param in
	 * @param out
	 */
	private < U extends Type< U > > void copy(
			final RandomAccessibleInterval< U > in,
			final RandomAccessibleInterval< U > out ) {
		final long[] blockSize = computeBlockSize( in );

		final TiledView< U > tiledViewIn = new TiledView<>( in, blockSize );
		final TiledView< U > tiledViewOut = new TiledView<>( out, blockSize );

		final ExecutorService pool = Executors.newWorkStealingPool();
		final List< Future< ? > > futures = new ArrayList<>();

		final Cursor< RandomAccessibleInterval< U > > tileCurser =
				Views.iterable( tiledViewIn ).cursor();
		final RandomAccess< RandomAccessibleInterval< U > > tileRandomAccess =
				tiledViewOut.randomAccess();

		while ( tileCurser.hasNext() ) {
			// Get current tiles
			tileCurser.fwd();
			tileRandomAccess.setPosition( tileCurser );
			final RandomAccessibleInterval< U > tileIn = tileCurser.get();
			final RandomAccessibleInterval< U > tileOut = tileRandomAccess.get();

			// Add loop for current tiles to pool
			futures.add( pool.submit( () -> {
				final Cursor< U > c = Views.iterable( tileIn ).cursor();
				final RandomAccess< U > r = tileOut.randomAccess();
				while ( c.hasNext() ) {
					c.fwd();
					r.setPosition( c );
					r.get().set( c.get() );
				}
			} ) );

		}

		// NB: This code is much nicer but prints to stderr.
		// This will be fixed after https://github.com/imglib/imglib2/pull/193 is in the release

//		LoopBuilder.setImages( tiledViewIn, tiledViewOut ).forEachPixel(
//				( inTile, outTile ) -> {
//					final LoopBuilder< BiConsumer< U, U > > loop = LoopBuilder.setImages( inTile, outTile );
//					futures.add( pool.submit( () -> {
//						loop.forEachPixel( ( i, o ) -> o.set( i ) );
//					} ) );
//				} );

		for ( final Future< ? > f : futures ) {
			try {
				f.get();
			} catch ( InterruptedException | ExecutionException e ) {
				e.printStackTrace();
			}
		}
		pool.shutdown();
	}

	private < U extends RealType< U >, V extends RealType< V >, W extends RealType< W > > void
			pointwiseGeometricMean(
					final RandomAccessibleInterval< U > in1,
					final RandomAccessibleInterval< V > in2,
					final RandomAccessibleInterval< W > out ) {
		final long[] blockSize = computeBlockSize( in1 );

		final TiledView< U > tiledViewIn1 = new TiledView<>( in1, blockSize );
		final TiledView< V > tiledViewIn2 = new TiledView<>( in2, blockSize );
		final TiledView< W > tiledViewOut = new TiledView<>( out, blockSize );

//		final ExecutorService pool = Executors.newWorkStealingPool();
		final List< Future< ? > > futures = new ArrayList<>();

		futures.clear();

		final Cursor< RandomAccessibleInterval< U > > tileCursorIn1 =
				Views.iterable( tiledViewIn1 ).cursor();
		final RandomAccess< RandomAccessibleInterval< V > > tileRandomAccessIn2 =
				tiledViewIn2.randomAccess();
		final RandomAccess< RandomAccessibleInterval< W > > tileRandomAccessOut =
				tiledViewOut.randomAccess();

		while ( tileCursorIn1.hasNext() ) {
			// Set positions
			tileCursorIn1.fwd();
			tileRandomAccessIn2.setPosition( tileCursorIn1 );
			tileRandomAccessOut.setPosition( tileCursorIn1 );

			// Get tiles
			final RandomAccessibleInterval< U > tileIn1 = tileCursorIn1.get();
			final RandomAccessibleInterval< V > tileIn2 = tileRandomAccessIn2.get();
			final RandomAccessibleInterval< W > tileOut = tileRandomAccessOut.get();

			// Add loop for current tile to pool
			futures.add( pool.submit( () -> {
				final Cursor< U > i1 = Views.iterable( tileIn1 ).cursor();
				final RandomAccess< V > i2 = tileIn2.randomAccess();
				final RandomAccess< W > o = tileOut.randomAccess();
				while ( i1.hasNext() ) {
					i1.fwd();
					i2.setPosition( i1 );
					o.setPosition( i1 );
					o.get().setReal(
							Math.sqrt( i1.get().getRealFloat() * i2.get().getRealFloat() ) );
				}
			} ) );
		}

		// NB: This code is much nicer but prints to stderr.
		// This will be fixed after https://github.com/imglib/imglib2/pull/193 is in the release

//		LoopBuilder.setImages( tiledViewIn1, tiledViewIn2, tiledViewOut ).forEachPixel(
//				( in1Tile, in2Tile, outTile ) -> {
//					final LoopBuilder< TriConsumer< U, V, W > > loop = LoopBuilder.setImages( in1Tile, in2Tile, outTile );
//					futures.add( pool.submit( () -> {
//						loop.forEachPixel(
//								( i1, i2, o ) -> {
//									o.setReal( Math.sqrt( i1.getRealFloat() * i2.getRealFloat() ) );
//								} );
//					} ) );
//				} );

		for ( final Future< ? > f : futures ) {
			try {
				f.get();
			} catch ( InterruptedException | ExecutionException e ) {
				e.printStackTrace();
			}
		}
		pool.shutdown();
	}

	private long[] computeBlockSize( final RandomAccessibleInterval< ? > in ) {
		final int threads = Runtime.getRuntime().availableProcessors();

		final long[] blockSize = Intervals.dimensionsAsLongArray( in );
		int tmpMax = 0;
		for ( int i = 0; i < blockSize.length; i++ ) {
			tmpMax = blockSize[ i ] > blockSize[ tmpMax ] ? i : tmpMax;
		}
		final int max = tmpMax;
		IntStream.range( 0, blockSize.length ).forEach(
				( i ) -> blockSize[ i ] = i == max ? ( long ) Math.ceil(
						blockSize[ i ] * 1.0 / threads ) : blockSize[ i ] );
		return blockSize;
	}

	public static void main( final String[] args ) throws IOException {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch( args );

		// ask the user for a file to open
//		final File file = ij.ui().chooseFile( null, "open" );
		final File file =
				new File( "/home/random/Development/imagej/plugins/CSBDeep-data/net_iso/input-2.tif" );

		if ( file != null && file.exists() ) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open( file.getAbsolutePath() );

			// show the image
			ij.ui().show( dataset );

			// invoke the plugin
			ij.command().run( NetIso.class, true );
		}
	}
}

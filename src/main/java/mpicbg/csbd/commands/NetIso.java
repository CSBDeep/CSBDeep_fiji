package mpicbg.csbd.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.IntStream;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import mpicbg.csbd.ui.CSBDeepProgress;

@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Iso", headless = true )
public class NetIso< T extends RealType< T > > extends CSBDeepCommand< T > implements Command {

	@Parameter( label = "Scale Z", min = "1", max = "15" )
	protected float scale = 10.2f;

	@Override
	public void initialize() {

		super.initialize();

		modelFileUrl = "http://csbdeep.bioimagecomputing.com/model-iso.zip";
		modelName = "net_iso";

	}

	public static void main( String[] args ) throws IOException {
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
			ij.command().run( NetIso.class, true );
		}
	}

	@Override
	public void run() {

		if ( input == null ) { return; }
		modelChanged();

		AxisType[] mapping = { Axes.Z, Axes.Y, Axes.X, Axes.CHANNEL };
		setMapping( mapping );

		int dimChannel = input.dimensionIndex( Axes.CHANNEL );
		int dimX = input.dimensionIndex( Axes.X );
		int dimY = input.dimensionIndex( Axes.Y );
		int dimZ = input.dimensionIndex( Axes.Z );

		initGui();

		initModel();
		progressWindow.setNumRounds( 2 );
		progressWindow.setStepStart( CSBDeepProgress.STEP_PREPROCRESSING );

		progressWindow.addLog( "Normalize input.. " );

		int n = input.numDimensions();

		// ========= NORMALIZATION
		// TODO maybe there is a better solution than splitting the image, normalizing each channel and combining it again.
		IntervalView< T > channel0 =
				Views.hyperSlice( input.typedImg( ( T ) input.firstElement() ), dimChannel, 0 );
		IntervalView< T > channel1 =
				Views.hyperSlice( input.typedImg( ( T ) input.firstElement() ), dimChannel, 1 );

		prepareNormalization( channel0 );
		Img< FloatType > normalizedChannel0 = normalizeImage( channel0 );

		prepareNormalization( channel1 );
		Img< FloatType > normalizedChannel1 = normalizeImage( channel1 );

		RandomAccessibleInterval< FloatType > normalizedInput = Views.permute(
				Views.stack( normalizedChannel0, normalizedChannel1 ),
				n - 1,
				dimChannel );

		progressWindow.addLog( "Upsampling.." );

		// ========= UPSAMPLING
		RealRandomAccessible< FloatType > interpolated =
				Views.interpolate(
						Views.extendBorder( normalizedInput ),
						new NLinearInterpolatorFactory<>() );

		// Affine transformation to scale the Z axis
		double s = scale;
		double[] scales = IntStream.range( 0, n ).mapToDouble( i -> i == dimZ ? s : 1 ).toArray();
		AffineGet scaling = new Scale( scales );

		// Scale min and max to create an interval afterwards
		double[] targetMin = new double[ n ];
		double[] targetMax = new double[ n ];
		scaling.apply( Intervals.minAsDoubleArray( normalizedInput ), targetMin );
		scaling.apply( Intervals.maxAsDoubleArray( normalizedInput ), targetMax );

		// Apply the transformation
		RandomAccessible< FloatType > scaled = RealViews.affine( interpolated, scaling );
		RandomAccessibleInterval< FloatType > upsampled = Views.interval(
				scaled,
				Arrays.stream( targetMin ).mapToLong( d -> ( long ) Math.ceil( d ) ).toArray(),
				Arrays.stream( targetMax ).mapToLong( d -> ( long ) Math.floor( d ) ).toArray() );

		// ========== ROTATION

		progressWindow.addLog( "Rotate around Y.." );

		// Create the first rotated image
		RandomAccessibleInterval< FloatType > rotated0 = Views.permute( upsampled, dimX, dimZ );
		progressWindow.addLog( "Rotate around X.." );

		// Create the second rotated image
		RandomAccessibleInterval< FloatType > rotated1 = Views.permute( rotated0, dimY, dimZ );

		List< RandomAccessibleInterval< FloatType > > result0 = new ArrayList<>();
		List< RandomAccessibleInterval< FloatType > > result1 = new ArrayList<>();

		runBatches( rotated0, rotated1, result0, result1 );

		if ( result0.size() == 4 && result1.size() == 4 ) {

			//prediction for ZY rotation
			RandomAccessibleInterval< FloatType > res0_pred =
					Views.stack( result0.get( 0 ), result0.get( 1 ) );

			//prediction for ZX rotation
			RandomAccessibleInterval< FloatType > res1_pred =
					Views.stack( result1.get( 0 ), result1.get( 1 ) );

			//control for ZY rotation
			RandomAccessibleInterval< FloatType > res0_control =
					Views.stack( result0.get( 2 ), result0.get( 3 ) );

			//control for ZX rotation
			RandomAccessibleInterval< FloatType > res1_control =
					Views.stack( result1.get( 2 ), result1.get( 3 ) );

			// rotate output stacks back

			//TODO the rotation dimensions are not dynamic yet, we should use variables

			progressWindow.addLog( "Rotate output stacks back to original orientation.." );

			printDim( "res0_pred", res0_pred );
			printDim( "res1_pred", res1_pred );

			res0_pred = Views.permute( res0_pred, 0, 2 );
			res0_control = Views.permute( res0_control, 0, 2 );

			res1_pred = Views.permute( res1_pred, 1, 2 );
			res1_pred = Views.permute( res1_pred, 0, 2 );
			res1_control = Views.permute( res1_control, 1, 2 );
			res1_control = Views.permute( res1_control, 0, 2 );

			printDim( "res0_pred rotated back", res0_pred );
			printDim( "res1_pred rotated back", res1_pred );

			progressWindow.addLog( "Merge output stacks.." );

			// Calculate the geometric mean of the two predictions
			RandomAccessibleInterval< FloatType > prediction =
					ArrayImgs.floats( Intervals.dimensionsAsLongArray( res0_pred ) );
			pointwiseGeometricMean(
					Views.iterable( res0_pred ),
					res1_pred,
					prediction );
			printDim( "prediction", prediction );
			resultDataset = wrapIntoDataset("result", Views.permute( prediction, 2, 3 ));

			// Calculate the geometric mean of the two control outputs
			RandomAccessibleInterval< FloatType > control =
					ArrayImgs.floats( Intervals.dimensionsAsLongArray( res0_pred ) );
			pointwiseGeometricMean(
					Views.iterable( res0_control ),
					res1_control,
					control );
			controlDataset = wrapIntoDataset("control", Views.permute( control, 2, 3 ));

			progressWindow.addLog( "All done!" );
			progressWindow.setCurrentStepDone();
		} else {
			progressWindow.setCurrentStepFail();
		}
	}

	private void runBatches(
			RandomAccessibleInterval< FloatType > rotated0,
			RandomAccessibleInterval< FloatType > rotated1,
			List< RandomAccessibleInterval< FloatType > > result0,
			List< RandomAccessibleInterval< FloatType > > result1 ) {

		result0.clear();
		result1.clear();

		try {

			//TODO check for OOM
			// in case of memory issue, do something like this:

			//nTiles *= 2;
			//progressWindow.addRounds(2);
			//progressWindow.setNextRound();
			//runBatches(rotated0, rotated1, result0, result1);

			result0.addAll( pool.submit(
					new BatchedTiledPrediction( rotated0, bridge, model, progressWindow, nTiles, 4, overlap ) ).get() );

			progressWindow.setNextRound();

			result1.addAll( pool.submit(
					new BatchedTiledPrediction( rotated1, bridge, model, progressWindow, nTiles, 4, overlap ) ).get() );

		} catch ( RejectedExecutionException | InterruptedException exc ) {
			return;
		} catch ( ExecutionException exc ) {
			exc.printStackTrace();
			progressWindow.setCurrentStepFail();
		}

	}

	private static < T extends RealType< T >, U extends RealType< U >, V extends RealType< V > >
			void pointwiseGeometricMean(
					IterableInterval< T > in1,
					RandomAccessibleInterval< U > in2,
					RandomAccessibleInterval< V > out ) {
		Cursor< T > i1 = in1.cursor();
		RandomAccess< U > i2 = in2.randomAccess();
		RandomAccess< V > o = out.randomAccess();

		while ( i1.hasNext() ) {
			i1.fwd();
			i2.setPosition( i1 );
			o.setPosition( i1 );
			o.get().setReal( Math.sqrt( i1.get().getRealFloat() * i2.get().getRealFloat() ) );
		}
	}

}

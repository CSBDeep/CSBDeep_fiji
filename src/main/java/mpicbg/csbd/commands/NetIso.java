package mpicbg.csbd.commands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
		// TODO super.run() normalizes the whole image at once. But for this
		// command the image has to be normalized per channel. I think there
		// should be a smarter solution than implementing the whole run() method
		// here...

		// Below is copied from CSBDeepCommand.run()
		if ( input == null ) { return; }
		modelChanged();

//		// Set the mapping TODO there is probably a smarter way...
//		int dimChannel = input.dimensionIndex( Axes.CHANNEL );
//		int dimX = input.dimensionIndex( Axes.X );
//		int dimY = input.dimensionIndex( Axes.Y );
//		int dimZ = input.dimensionIndex( Axes.Z );
//		bridge.setTFMappingByKnownAxesIndex( 0, dimZ );
//		bridge.setTFMappingByKnownAxesIndex( 1, dimChannel );
//		bridge.setTFMappingByKnownAxesIndex( 2, dimY );
//		bridge.setTFMappingByKnownAxesIndex( 3, dimX );
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

		// =============================================================================
		// The normalization needs to be done per channel

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

		displayAsDataset( "Normalized image", normalizedInput );

		progressWindow.addLog( "Upsampling." );

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

		displayAsDataset( "upsampled", upsampled );

		// ========== ROTATION

		progressWindow.addLog( "Rotate around Y.." );

		// Create the first rotated image
		RandomAccessibleInterval< FloatType > rotated0 = Views.permute( upsampled, dimX, dimZ );
//		rotated0 = Views.permute( rotated0, dimX, dimY );

//		uiService.show( "upsampled", upsampled );

		progressWindow.addLog( "Rotate around X.." );

		// Create the second rotated image
		RandomAccessibleInterval< FloatType > rotated1 = Views.permute( rotated0, dimY, dimZ );

		displayAsDataset( "Input ZY", rotated0 );
		displayAsDataset( "Input ZX", rotated1 );

		List< RandomAccessibleInterval< FloatType > > result0 = null;
		List< RandomAccessibleInterval< FloatType > > result1 = null;
		try {
			// ============================================================================================
			// TODO tiled prediction is here not applicable.
			// We need to tile in the current Z dimension without overlap. The Z
			// dimension will be mapped to the TensorFlow batch dimension and
			// the model will process multiple 2D images with 2 channels which
			// correspond to the slices of the 3D image.

			// TODO the progess window will show 100% after the first part
//			bridge.permuteInputAxes( dimX, dimZ );
			result0 = pool.submit(
					new BatchedTiledPrediction( rotated0, bridge, model, progressWindow, nTiles, 4, 0 ) ).get();
			progressWindow.setNextRound();
//			DatasetTensorBridge bridge1 = bridge.clone();
//			bridge1.permuteInputAxes( dimY, dimZ );
			result1 = pool.submit(
					new BatchedTiledPrediction( rotated1, bridge, model, progressWindow, nTiles, 4, 0 ) ).get();
			// ============================================================================================
		} catch ( ExecutionException exc ) {
			progressWindow.setCurrentStepFail();
			exc.printStackTrace();
		} catch ( InterruptedException exc ) {
			progressWindow.addError( "Process canceled." );
			progressWindow.setCurrentStepFail();
		}

		if ( result0 != null && result1 != null ) {
			if ( result0.size() == 4 && result1.size() == 4 ) {

				// TODO the result doesn't get split by the channels because the
				// channels are not in the last dimension
				// Also, we don't want to split by each channel but split the
				// output image with 4 channel into 2 image with 2 channels
				// each. Because the 4 channels are: pred_ch0, pred_ch1,
				// control_ch0, control_ch1.
				// If this is done somehow... the following code should work

//				----------------------------------- I tried to use Ops
//				uiService.show(result0.get(0));
//				IterableInterval< FloatType > product = op.math().multiply(Views.flatIterable(result0.get(0)), result1.get(0));
//				Iterable<FloatType> mean = op.map(product, new UnaryRealTypeMath.Sqrt<FloatType, FloatType>());
//				RandomAccessibleInterval< FloatType > output = ArrayImgs.floats(Intervals.dimensionsAsLongArray(result0.get(0)));
//				op.map(output, result0.get(0), Views.flatIterable(result1.get(0)), new AbstractBinaryComputerOp<FloatType, FloatType, FloatType>() {
//					...
//				});

				printDim( "result0.get( 0 )", result0.get( 0 ) );

				RandomAccessibleInterval< FloatType > res0_pred =
						Views.stack( result0.get( 0 ), result0.get( 1 ) );
				RandomAccessibleInterval< FloatType > res1_pred =
						Views.stack( result1.get( 0 ), result1.get( 1 ) );
				RandomAccessibleInterval< FloatType > res0_control =
						Views.stack( result0.get( 2 ), result0.get( 3 ) );
				RandomAccessibleInterval< FloatType > res1_control =
						Views.stack( result1.get( 2 ), result1.get( 3 ) );

				printDim( "res0_pred", res0_pred );
				printDim( "res1_pred", res1_pred );
//
				res0_pred = Views.permute( res0_pred, 0, 2 );
				res0_control = Views.permute( res0_control, 0, 2 );

				res1_pred = Views.permute( res1_pred, 1, 2 );
				res1_pred = Views.permute( res1_pred, 0, 2 );
				res1_control = Views.permute( res1_control, 1, 2 );
				res1_control = Views.permute( res1_control, 0, 2 );

				printDim( "res0_pred rotated back", res0_pred );
				printDim( "res1_pred rotated back", res1_pred );

//				res1_control = Views.permute( res1_control, 1, 2 );
//				bridge.permuteInputAxes( dimX, dimZ );

				// Calculate the geometric mean of the two predictions
				RandomAccessibleInterval< FloatType > prediction =
						ArrayImgs.floats( Intervals.dimensionsAsLongArray( res0_pred ) );
				pointwiseGeometricMean(
						Views.iterable( res0_pred ),
						res1_pred,
						prediction );

				printDim( "prediction", prediction );

				// Calculate the geometric mean of the two control outputs
				RandomAccessibleInterval< FloatType > control =
						ArrayImgs.floats( Intervals.dimensionsAsLongArray( res0_pred ) );
				pointwiseGeometricMean(
						Views.iterable( res0_control ),
						res1_control,
						control );

				progressWindow.addLog( "Displaying result image.." );
				displayAsDataset( "result", Views.permute( prediction, 2, 3 ) );
				displayAsDataset( "control", Views.permute( control, 2, 3 ) );
				displayAsDataset(
						"result ZY",
						Views.permute( Views.permute( prediction, 0, 2 ), 2, 3 ) );
				displayAsDataset(
						"result ZX",
						Views.permute(
								Views.permute( Views.permute( prediction, 0, 2 ), 1, 2 ),
								2,
								3 ) );
				progressWindow.addLog( "All done!" );
				progressWindow.setCurrentStepDone();
			} else {
				progressWindow.addError( "TiledPrediction returned no result data." );
				progressWindow.setCurrentStepFail();
			}
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

	private static void printDim( String title, RandomAccessibleInterval< FloatType > img ) {
		long[] dims = new long[ img.numDimensions() ];
		img.dimensions( dims );
		System.out.println( title + ": " + Arrays.toString( dims ) );
	}

	private void displayAsDataset( String title, RandomAccessibleInterval< FloatType > img ) {
		Dataset dataset = datasetService.create( img );
		for ( int i = 0; i < input.numDimensions(); i++ ) {
			dataset.setAxis( input.axis( i ), i );
		}
		uiService.show( title, dataset );
	}
}

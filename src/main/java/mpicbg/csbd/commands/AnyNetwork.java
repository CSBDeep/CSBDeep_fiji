/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

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

import com.google.protobuf.InvalidProtocolBufferException;

import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.tensorflow.DatasetConverter;
import mpicbg.csbd.tensorflow.DatasetTensorBridge;
import mpicbg.csbd.tensorflow.DefaultDatasetConverter;
import mpicbg.csbd.tensorflow.TensorFlowRunner;
import mpicbg.csbd.ui.MappingDialog;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imagej.tensorflow.TensorFlowService;
import net.imagej.tensorflow.Tensors;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.ArrangedView;
import net.imglib2.view.CombinedView;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.TiledView;
import net.imglib2.view.Views;

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

	@Parameter( label = "Graph file name (.pb)", callback = "modelChanged", initializer = "graphInitialized", persist = false )
	protected String graphFileName = "";
	private final String graphFileKey = "graphfile-anynetwork";

	@Parameter( label = "Input node name", callback = "inputNodeNameChanged", initializer = "inputNodeNameChanged" )
	private String inputNodeName = "input";

	@Parameter( label = "Output node name", persist = false )
	private String outputNodeName = "output";

	@Parameter( label = "Adjust image <-> tensorflow mapping", callback = "openTFMappingDialog" )
	private Button changeTFMapping;

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

	private Graph graph;
	private SavedModelBundle model;
	private SignatureDef sig;
	private DatasetTensorBridge bridge;
	private boolean hasSavedModel = true;
	private boolean processedDataset = false;
	private final DatasetConverter<T> datasetConverter = new DefaultDatasetConverter<>();

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
	 * model can be imported via graphdef or savedmodel
	 */
	protected boolean loadGraph() {

//		System.out.println("loadGraph");

		if ( modelFile == null ) {
			System.out.println( "Cannot load graph from null File" );
			return false;
		}

		final FileLocation source = new FileLocation( modelFile );
		hasSavedModel = true;
		try {
			model = tensorFlowService.loadModel( source, source.getName(), graphFileName );
		} catch ( TensorFlowException | IOException e ) {
			hasSavedModel = false;
			try {
				graph = tensorFlowService.loadGraph( source, source.getName(), graphFileName );
			} catch ( final IOException e2 ) {
//				e2.printStackTrace();
				System.err.println(
						"Could not find graph file \"" + graphFileName + "\" in " + modelFile.getAbsolutePath() );
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

	/** Executed whenever the {@link #modelFile} parameter is initialized. */
	protected void modelInitialized() {
		final String p_modelfile = prefService.get( modelFileKey, "" );
		if ( p_modelfile != "" ) {
			modelFile = new File( p_modelfile );
			modelChanged();
		}
	}

	/** Executed whenever the {@link #modelFile} parameter is initialized. */
	protected void graphInitialized() {
		final String p_graphfile = prefService.get( graphFileKey, "" );
		if ( p_graphfile != "" && modelFile != null ) {
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
		RandomAccessibleInterval<FloatType> tiledPrediction = tiledPrediction(input, 4);
		uiService.show(tiledPrediction);
//		uiService.show(arrayToDataset(datasetToArray(input)));

	}

	private RandomAccessibleInterval<FloatType> tiledPrediction(Dataset im, int nTiles) {
		return tiledPrediction((RandomAccessibleInterval) im.getImgPlus(), nTiles);
	}

	private RandomAccessibleInterval<FloatType> tiledPrediction(RandomAccessibleInterval<T> im, int nTiles) { // TODO output type
		// Get the dimensions of the image
		long[] shape = new long[im.numDimensions()];
		im.dimensions(shape);

		// Get the largest dimension and its size
		int largestDim = 0;
		long largestSize = 0;
		for (int d = 0; d < im.numDimensions(); d++) {
			if (shape[d] > largestSize) {
				largestSize = shape[d];
				largestDim = d;
			}
		}

		// Calculate the blocksize to use
		double blockwidthIdeal = largestSize / (double) nTiles;
		long blockwidth = (long) (Math.ceil(blockwidthIdeal / 32.0) * 32); // TODO(benjamin) remove magic numbers
		long[] blockSize = Arrays.copyOf(shape, im.numDimensions());
		blockSize[largestDim] = blockwidth;

		// Expand the image to fit the blocksize
		im = expandDimToSize(im, largestDim, blockwidth * nTiles);
		printDim("After expand", im);

		// Put the padding per dimension in a array
		long[] padding = new long[im.numDimensions()];
		padding[largestDim] = 32; // TODO remove magic number

		// Create the tiled view
		TiledView<T> tiledView = new TiledView<>(im, blockSize, padding);
		Cursor<RandomAccessibleInterval<T>> cursor = Views.iterable(tiledView).cursor();

		// Set padding to negative to remove it later
		long[] negPadding = padding.clone();
		negPadding[largestDim] = - padding[largestDim];

		// Loop over the tiles and execute the prediction
		List<RandomAccessibleInterval<FloatType>> results = new ArrayList<>();
		while (cursor.hasNext()) {
			RandomAccessibleInterval<T> tile = cursor.next();
			//uiService.show(tile);
			RandomAccessibleInterval<FloatType> tileExecuted = executeGraphWithPadding(tile);
			// Remove padding
			tileExecuted = Views.zeroMin(Views.expandZero(tileExecuted, negPadding));
			//uiService.show(tileExecuted);
			results.add(tileExecuted);
		}
		// Arrange and combine the tiles again
		long[] grid = new long[results.get(0).numDimensions()];
		for (int i = 0; i < grid.length; i++) {
			grid[i] = i == largestDim ? nTiles : 1;
		}
		RandomAccessibleInterval<FloatType> result = new CombinedView<FloatType>(new ArrangedView<>(results, grid));
		return expandDimToSize(result, largestDim, shape[largestDim]);
	}
	
	private <U extends RealType<U>> RandomAccessibleInterval<U> expandDimToSize(RandomAccessibleInterval<U> im, int d, long size) {
		final int n = im.numDimensions();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		im.min( min );
		im.max( max );
		max[ d ] += (size - im.dimension(d));
		return Views.interval(Views.extendMirrorDouble(im), new FinalInterval(min, max));
	}

	private RandomAccessibleInterval<FloatType> executeGraphWithPadding(final RandomAccessibleInterval<T> input) {
		Tensor inputTensor = datasetConverter.datasetToTensor(input, bridge, this);
		Tensor outputTensor = TensorFlowRunner.executeGraph(
													getGraph(),
													inputTensor,
													inputNodeName,
													outputNodeName );
		return datasetConverter.tensorToDataset(outputTensor, bridge);
	}

	// TODO remove
	private static void printDim(String name, RandomAccessibleInterval<?> im) {
		System.out.print(name + ": [ ");
		for (int i = 0; i < im.numDimensions(); i++) {
			System.out.print(im.dimension(i) + " ");
		}
		System.out.println("]");
	}

	private void savePreferences() {
		prefService.put( modelFileKey, modelFile.getAbsolutePath() );
		prefService.put( graphFileKey, graphFileName );

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

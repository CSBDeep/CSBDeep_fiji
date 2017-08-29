/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import com.google.protobuf.InvalidProtocolBufferException;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

/**
 */
@Plugin(type = Command.class, menuPath = "Plugins>CSBDeep", headless = true)
public class CSBDeep<T extends RealType<T>> implements Command, Previewable, Cancelable {
	
	@Parameter(visibility = ItemVisibility.MESSAGE)
	private String header = "This command removes noise from your images.";

    @Parameter(label = "input data", type = ItemIO.INPUT, callback = "imageChanged", initializer = "imageChanged")
    private Dataset input = null;
    
    @Parameter(label = "Normalize image")
	private boolean normalizeInput = true;
    
    @Parameter(label = "Import model", callback = "modelChanged", initializer = "modelChanged")
    private File modelfile = null;
    
    @Parameter(label = "Input node name", callback = "inputNodeNameChanged", initializer = "inputNodeNameChanged")
    private String inputNodeName = "input_1";
    
    @Parameter(label = "Output node name", persist = false)
    private String outputNodeName = "output";
    
    @Parameter(label = "Adjust image <-> tensorflow mapping", callback = "openTFMappingDialog")
	private Button changeTFMapping;
    
    @Parameter
	private TensorFlowService tensorFlowService;
    
    @Parameter
	private mpicbg.csbd.TensorFlowService tensorFlowService2;
    
    @Parameter
	private LogService log;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;
    
    @Parameter(type = ItemIO.OUTPUT)
    private Dataset outputImage;
    
    @Parameter
    private double percentile = 0.9;
    @Parameter
    private double min = 0;
    @Parameter
    private double max = 100;
    private Graph graph = null;
    private SavedModelBundle model = null;
    private SignatureDef sig = null;
    private DatasetTensorBridge bridge = null;
    private boolean hasSavedModel = true;
    
	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	private static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY = "serving_default";
    
    public CSBDeep(){
//    	modelChanged();
    }
    	
	@Override
	public void preview() {
//		imageChanged();
//		modelChanged();
	}
	
	protected boolean loadGraph(){
		
//		System.out.println("loadGraph");
		
		if(modelfile == null){
			System.out.println("Cannot load graph from null File");
			return false;
		}
		
		final FileLocation source = new FileLocation(modelfile);
		hasSavedModel = true;
		try {
			model = tensorFlowService.loadModel(source, modelfile.getName());
		} catch (TensorFlowException | IOException e) {
			try {
				graph = tensorFlowService2.loadGraph(modelfile);
//				graph = tensorFlowService.loadGraph(source, "", "");
				hasSavedModel = false;
			} catch (final IOException e2) {
				e2.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	protected boolean loadModelInputShape(final String inputName){
		
//		System.out.println("loadModelInputShape");
		
		if(getGraph() != null){
			final Operation input_op = getGraph().operation(inputName);
			if(input_op != null){
				bridge.setInputTensorShape(input_op.output(0).shape());
				return true;			
			}
			System.out.println("input node with name " + inputName + " not found");			
		}
		return false;
	}
	
	protected Graph getGraph(){
		if(hasSavedModel && (model == null)){
			return null;
		}
		return hasSavedModel ? model.graph() : graph;
	}
    
    /** Executed whenever the {@link #input} parameter changes. */
	protected void imageChanged() {
		
//		System.out.println("imageChanged");
		
		if(input != null) {
			bridge = new DatasetTensorBridge(input);
		}
		
	}
	
    /** Executed whenever the {@link #modelfile} parameter changes. */
	protected void modelChanged() {
		
//		System.out.println("modelChanged");
		
		imageChanged();
		if(loadGraph()){
			
			if(hasSavedModel){
				// Extract names from the model signature.
				// The strings "input", "probabilities" and "patches" are meant to be
				// in sync with the model exporter (export_saved_model()) in Python.
				try {
					sig = MetaGraphDef.parseFrom(model.metaGraphDef())
						.getSignatureDefOrThrow(DEFAULT_SERVING_SIGNATURE_DEF_KEY);
				} catch (final InvalidProtocolBufferException e) {
//					e.printStackTrace();
					hasSavedModel = false;
				}
				if(sig != null && sig.isInitialized()){
					if(sig.getInputsCount() > 0){
						inputNodeName = sig.getInputsMap().keySet().iterator().next();					
					}
					if(sig.getOutputsCount() > 0){
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
		
		loadModelInputShape(inputNodeName);
		
		if(bridge.getInputTensorShape() != null){
			if(!bridge.isMappingInitialized()){
				bridge.setMappingDefaults();
			}
		}
	}
	
	protected void openTFMappingDialog() {
		
		imageChanged();
		
		if(bridge.getInputTensorShape() == null){
			modelChanged();
		}
		
		MappingDialog.create(bridge, sig);
	}

	@Override
    public void run() {
		
//		System.out.println("run");
		
//		Dataset input_norm = input.duplicateBlank();
		
//		opService.run(PercentileNormalization.class, input_norm.getImgPlus(), input.getImgPlus(), percentile);
//		uiService.show(input_norm);
		
		if(graph == null){
			modelChanged();
		}

		try (
			final Tensor image = arrayToTensor(datasetToArray(input));
		)
		{
			outputImage = executeGraph(getGraph(), image);	
			uiService.show(outputImage);
		}
		
//		uiService.show(arrayToDataset(datasetToArray(input)));
		
    }
	
	private float[][][][][] datasetToArray(final Dataset d) {
				
		final float[][][][][] inputarr = bridge.createFakeTFArray();

		//copy input data to array
		
		final Cursor<T> cursor = (Cursor<T>) d.localizingCursor();
		while( cursor.hasNext() )
		{
			final int[] pos = {0,0,0,0,0};
			final T val = cursor.next();
			for(int i = 0; i < pos.length; i++){
				final int imgIndex = bridge.getDatasetDimIndexByTFIndex(i);
				if(imgIndex >= 0){
					pos[i] = cursor.getIntPosition(imgIndex);
				}
			}
			final float fval = val.getRealFloat();
//			System.out.println("pos " + pos[0] + " " + pos[1] + " " + pos[2] + " " + pos[3] + " " + pos[4]);
			inputarr[pos[0]][pos[1]][pos[2]][pos[3]][pos[4]] = fval;
			
		}
		
		return inputarr;
	}
	
	private Tensor arrayToTensor(final float[][][][][] array){
		if(bridge.getInputTensorShape().numDimensions() == 4){
			return Tensor.create(array[0]);
		}		
		return Tensor.create(array);
	}
	
	private Dataset executeGraph(final Graph g, final Tensor image)
		{	
		
		System.out.println("executeInceptionGraph");
		
		try (
				Session s = new Session(g);
//				
//				System.out.println
		) {
			
//			int size = s.runner().feed(inputNodeName, image).fetch(outputNodeName).run().size();
//			System.out.println("output array size: " + size);
			Tensor output_t = null;
			if(graph.operation("dropout_1/keras_learning_phase") != null){
				final Tensor learning_phase = Tensor.create(false);
				try{
					final Tensor output_t2 = s.runner().feed(inputNodeName, image).feed("dropout_1/keras_learning_phase", learning_phase).fetch(outputNodeName).run().get(0);
					output_t = output_t2;
				}
				catch(final Exception e){
					e.printStackTrace();
				}
			}else{
				try{
					final Tensor output_t2 = s.runner().feed(inputNodeName, image).fetch(outputNodeName).run().get(0);
					output_t = output_t2;
				}
				catch(final Exception e){
					e.printStackTrace();
				}
			}
			
			if(output_t != null){
				System.out.println("Output tensor with " + output_t.numDimensions() + " dimensions");
				
				if(output_t.numDimensions() == 0){
					showError("Output tensor has no dimensions");
					return null;
				}
				
				final float[][][][][] outputarr = bridge.createFakeTFArray(output_t);
				
				for(int i = 0; i < output_t.numDimensions(); i++){
					System.out.println("output dim " + i + ": " + output_t.shape()[i]);
				}
				
				if(output_t.numDimensions() -1 == bridge.getInputTensorShape().numDimensions()){
					//model reduces dim by 1
					//assume z gets reduced -> move it to front and ignore first dimension
					System.out.println("model reduces dimension, z dimension reduction assumed");
					bridge.moveZMappingToFront();
				}
				
				if(output_t.numDimensions() == 5){
					output_t.copyTo(outputarr);
				}else{
					if(output_t.numDimensions() == 4){
						output_t.copyTo(outputarr[0]);					
					}else{
						if(output_t.numDimensions() == 3){
							output_t.copyTo(outputarr[0][0]);
						}
					}
				}
				
				return arrayToDataset(outputarr, output_t.shape());	
			}
			return null;
			
			
		}
		catch (final Exception e) {
			System.out.println("could not create output dataset");
			e.printStackTrace();
		}
		return null;
	}
	
	private Dataset arrayToDataset(final float[][][][][] outputarr, final long[] shape){
		
		final Dataset img_out = bridge.createFromTFDims(shape);
		
		//write ouput dataset and undo normalization
		
		final Cursor<T> cursor = (Cursor<T>) img_out.localizingCursor();
		while( cursor.hasNext() )
		{
			final int[] pos = {0,0,0,0,0};
			final T val = cursor.next();
			for(int i = 0; i < pos.length; i++){
				final int imgIndex = bridge.getDatasetDimIndexByTFIndex(i);
				if(imgIndex >= 0){
					pos[i] = cursor.getIntPosition(imgIndex);
				}
			}
//			System.out.println("pos " + pos[0] + " " + pos[1] + " " + pos[2] + " " + pos[3] + " " + pos[4]);
			val.setReal(outputarr[pos[0]][pos[1]][pos[2]][pos[3]][pos[4]]);
			
		}

		return img_out;
		
	}

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();

        // ask the user for a file to open
        final File file = ij.ui().chooseFile(null, "open");
        
        if(file.exists()){
            // load the dataset
            final Dataset dataset = ij.scifio().datasetIO().open(file.getAbsolutePath());

            // show the image
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(CSBDeep.class, true);
        }    	

    }
    
    public void showError(final String errorMsg) {
    	JOptionPane.showMessageDialog(null, errorMsg, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isCanceled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void cancel(final String reason) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCancelReason() {
		// TODO Auto-generated method stub
		return null;
	}

}

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

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

/**
 */
@Plugin(type = Command.class, menuPath = "Plugins>CSBDeep", headless = true)
public class CSBDeep<T extends RealType<T>> implements Command, Previewable, Cancelable {
	
	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String header = "This command removes noise from your images.";

    @Parameter(label = "input data", type = ItemIO.INPUT, callback = "imageChanged", initializer = "imageChanged")
    private Dataset input = null;
    
    @Parameter(label = "Normalize image")
	private boolean normalizeInput = true;
    
    @Parameter(label = "Import model", callback = "modelChanged", initializer = "modelChanged")
    private File model = null;
    
    @Parameter(label = "Input node name", callback = "inputNodeNameChanged", initializer = "inputNodeNameChanged")
    private String inputNodeName = "input_1";
    
    @Parameter(label = "Adjust image <-> tensorflow mapping", callback = "openTFMappingDialog")
	private Button changeTFMapping;
    
    @Parameter
	private TensorFlowService tensorFlowService;
    
    @Parameter
	private LogService log;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;
    
    @Parameter(type = ItemIO.OUTPUT)
    private Dataset outputImage;
    
    private double min;
    private double max;
    private Graph graph = null;
    private DatasetTensorBridge bridge = null;
    
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
		
		if(model == null){
			System.out.println("Cannot load graph from null File");
			return false;
		}
		try {
			this.graph = tensorFlowService.loadGraph(model);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	protected boolean loadModelInputShape(String inputName){
		
//		System.out.println("loadModelInputShape");
		
		Operation input_op = graph.operation(inputName);
		if(input_op != null){
			bridge.setTensorShape(input_op.output(0).shape());
			return true;			
		}
		System.out.println("input node with name " + inputName + " not found");
		return false;
	}
    
    /** Executed whenever the {@link #input} parameter changes. */
	protected void imageChanged() {
		
//		System.out.println("imageChanged");
		
		if(input != null) {
			bridge = new DatasetTensorBridge(input);
		}
		
	}
	
    /** Executed whenever the {@link #model} parameter changes. */
	protected void modelChanged() {
		
//		System.out.println("modelChanged");
		
		imageChanged();
		if(loadGraph()){
			inputNodeNameChanged();
		}
	}
	
	/** Executed whenever the {@link #inputNodeName} parameter changes. */
	protected void inputNodeNameChanged() {
		
//		System.out.println("inputNodeNameChanged");
		
		if(graph != null){
			loadModelInputShape(inputNodeName);
		}
		if(bridge.getTensorShape() != null){
			if(!bridge.isMappingInitialized()){
				bridge.setMappingDefaults();
			}
		}
	}
	
	protected void openTFMappingDialog() {
		
		imageChanged();
		
		if(bridge.getTensorShape() == null){
			modelChanged();
		}
		
		MappingDialog.create(bridge);
	}

	@Override
    public void run() {
		
//		System.out.println("run");
		
		min = input.randomAccess().get().getMinValue();
		max = input.randomAccess().get().getMaxValue();
		
		if(graph == null){
			modelChanged();
		}

		try (
			final Tensor image = arrayToTensor(datasetToArray(input));
		)
		{
			outputImage = executeInceptionGraph(graph, image);	
			uiService.show(outputImage);
		}
		
//		uiService.show(arrayToDataset(datasetToArray(input)));
		
    }
	
	private float[][][][][] datasetToArray(final Dataset d) {
				
		float[][][][][] inputarr = bridge.createFakeTFArray();
		
		double _min = normalizeInput ? min : 0;
		double _max = normalizeInput ? max : 1;

		//copy input data to array
		
		final Cursor<T> cursor = (Cursor<T>) d.localizingCursor();
		while( cursor.hasNext() )
		{
			int[] pos = {0,0,0,0,0};
			final T val = cursor.next();
			for(int i = 0; i < pos.length; i++){
				int imgIndex = bridge.getDatasetDimIndexByTFIndex(i);
				if(imgIndex >= 0){
					pos[i] = cursor.getIntPosition(imgIndex);
				}
			}
			float fval = val.getRealFloat();
//			System.out.println("pos " + pos[0] + " " + pos[1] + " " + pos[2] + " " + pos[3] + " " + pos[4]);
			inputarr[pos[0]][pos[1]][pos[2]][pos[3]][pos[4]] = (float) ((fval-_min)/(_max-_min));
			
		}
		
		return inputarr;
	}
	
	private Tensor arrayToTensor(float[][][][][] array){
		if(bridge.getTensorShape().numDimensions() == 4){
			return Tensor.create(array[0]);
		}		
		return Tensor.create(array);
	}
	
	private Dataset executeInceptionGraph(final Graph g, final Tensor image)
		{	
		
		try (
				Session s = new Session(g);
				Tensor output_t = s.runner().feed(inputNodeName, image).fetch("output").run().get(0);
		) {
			
			float[][][][][] outputarr = bridge.createFakeTFArray();
			
			if(output_t.numDimensions() == 5){
				output_t.copyTo(outputarr);
			}else{
				if(output_t.numDimensions() == 4){
					output_t.copyTo(outputarr[0]);					
				}else{
					if(output_t.numDimensions() == 3){
						// 4D -> 3D reduction
						output_t.copyTo(outputarr[0][0]);
						//ugly hack to make sure that the z axis gets deleted
						boolean shift = false;
						for(int i = bridge.numDimensions(); i > 0; i--){
							if(input.dimensionIndex(Axes.Z) == bridge.getMapping(i)){
								shift = true;
							}
							if(shift&& i > 1){
								bridge.setMapping(i, bridge.getMapping(i-1));									
							}
						}
					}
				}
			}
			
			return arrayToDataset(outputarr);
		}
		catch(IllegalArgumentException e){
			System.out.println("TFModel: Could not run model. Please ensure that you selected the correct model import file for your input data.");
			e.printStackTrace();
		}
		return null;
	}
	
	private Dataset arrayToDataset(final float[][][][][] outputarr){
		
		long[] dimensions = new long[input.numDimensions()];
		input.dimensions(dimensions);
		Dataset img_out = input.factory().create(dimensions, input.firstElement());
		
		double _min = normalizeInput ? min : 0;
		double _max = normalizeInput ? max : 1;
		
		//write ouput dataset and undo normalization
		
		final Cursor<T> cursor = (Cursor<T>) img_out.localizingCursor();
		while( cursor.hasNext() )
		{
			int[] pos = {0,0,0,0,0};
			final T val = cursor.next();
			for(int i = 0; i < pos.length; i++){
				int imgIndex = bridge.getDatasetDimIndexByTFIndex(i);
				if(imgIndex >= 0){
					pos[i] = cursor.getIntPosition(imgIndex);
				}
			}
			val.setReal(outputarr[pos[0]][pos[1]][pos[2]][pos[3]][pos[4]]*(_max-_min)+_min);
			
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
//        final File file = ij.ui().chooseFile(null, "open");
        final File file = new File("/home/random/x-stack.tif");
        
        if(file.exists()){
            // load the dataset
            final Dataset dataset = ij.scifio().datasetIO().open(file.getAbsolutePath());

            // show the image
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(CSBDeep.class, true);
        }

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
	public void cancel(String reason) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCancelReason() {
		// TODO Auto-generated method stub
		return null;
	}

}

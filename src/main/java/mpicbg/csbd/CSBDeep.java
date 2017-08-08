/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd;

import java.io.File;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imagej.tensorflow.TensorFlowService;
import net.imagej.tensorflow.Tensors;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

/**
 */
@Plugin(type = Command.class, menuPath = "Plugins>CSBDeep", headless = true)
public class CSBDeep<T extends RealType<T>> implements Command {

    @Parameter(label = "input data", type = ItemIO.INPUT)
    private Dataset currentData;
    
    @Parameter(label = "tensorflow model file")
    private File model;
    
    @Parameter(label = "useless button")
    private Boolean uselessButton = false;
    
    @Parameter
	private TensorFlowService tensorFlowService;
    
    @Parameter
	private LogService log;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;
    
    @Parameter(type = ItemIO.OUTPUT)
    private IntervalView<FloatType> outputImage;

	@Override
    public void run() {
		
		final double min = currentData.randomAccess().get().getMinValue();
		final double max = currentData.randomAccess().get().getMaxValue();
		
		try {
			final Graph graph = tensorFlowService.loadGraph(model);

			try (
				final Tensor image = loadFromImgLib(currentData, min, max);
			)
			{
				uiService.show(Views.permute(Tensors.img(image, min, max), 2, 0));				
				final long[] shape = image.shape();
				
				
//				final float[] out = new float[image.numElements()];
//				image.writeTo(FloatBuffer.wrap(out));
//				for(int i = 0; i < out.length; i++){
//					System.out.print(out[i]+ " ");
//				}
//				System.out.println("");
				
//				for(int i = 0; i < shape.length; i++){
//					System.out.println("dim" + i + ": "+ shape[i]);
//					//System.out.println("image dim " + i + ": " + image.dimension(i) + " values, min: " + image.min(i) + " max: " + image.max(i));
//				}				
				
//				Img<FloatType> _outputImage = executeInceptionGraph(graph, image, min, max);
				//input:Frame,Y,X,Col
				//step1: Frame,Y,Col,X
				//output: X,Y,Col,Frame
				Img<FloatType> result = executeInceptionGraph(graph, image, min, max);
				for(int i = 0; i < result.numDimensions(); i++){
					System.out.println("shape afterprocessing, dim " + i + ": " + result.dimension(i));			
				}
				outputImage = Views.permute(result, 3,1);
				
				uiService.show(outputImage);

			}
		}
		catch (final Exception exc) {
			log.error(exc);
		}
		
    }
	
	@SuppressWarnings({ "unchecked" })
	private static <T extends RealType<T>> Tensor loadFromImgLib(final Dataset d, final double min, final double max) {
				
		int numDims = d.numDimensions();
		int ch = (int)d.getChannels();
		
		RandomAccessibleInterval<T> image = (RandomAccessibleInterval<T>) d.getImgPlus();
		
		System.out.println("image with " + image.numDimensions() + " dimensions and " + ch + " channels");
		
		for(int i = 0; i < image.numDimensions(); i++){
			System.out.println("image dim before permuting " + i + ": " + image.dimension(i) + " values, min: " + image.min(i) + " max: " + image.max(i));
		}
		
		if(ch > 1){
			if(numDims > 3){
				//input: X,Y,Col,Frame
				//step1: Frame,Y,Col,X
				//output:Frame,Y,X,Col
				image = Views.permute(Views.permute(image, 0, 3), 2, 3);
			}else{
				image = Views.addDimension(image,0,0);
				//input: X,Y,Frame,NEWDIM
				//step1: Frame,Y,X,NEWDIM
				image = Views.permute(image, 0, 2);				
			}
		}else{
			image = Views.addDimension(image,0,0);
			if(numDims == 2){
				image = Views.addDimension(image,0,0);
				//input: X,Y,NEWDIM,NEWDIM
				//output: NEWDIM,Y,X,NEWDIM
				image = Views.permute(image, 0, 2);
				
			}else{
				//input: X,Y,Frame, NEWDIM
				//step1: NEWDIM,Y,Frame,X
				//step2: NEWDIM,X,Frame,Y
				//output: Frame,Y,X,NEWDIM
				image = Views.permute(image, 0, 2);
			}
		}
		
		for(int i = 0; i < image.numDimensions(); i++){
			System.out.println("image dim after permuting " + i + ": " + image.dimension(i) + " values, min: " + image.min(i) + " max: " + image.max(i));
		}
		
//		final ImageJ ij = new ImageJ();
//		ij.ui().show(image);
		
		assert(image.numDimensions() == 4);
		
		return Tensors.tensor2(image, min, max);
	}
	
	private Img<FloatType> executeInceptionGraph(final Graph g,
			final Tensor image, final double min, final double max)
		{
			for(long dim : image.shape()){
				System.out.println("tensor shape before processing: " + dim);			
			}
			try (
				final Session s = new Session(g);
				final Tensor result = s.runner().feed("input_1", image)//
					.fetch("output").run().get(0);
			)
			{
				return Tensors.img(result, min, max);
			}
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

}

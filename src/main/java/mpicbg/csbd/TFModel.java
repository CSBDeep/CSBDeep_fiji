package mpicbg.csbd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.imagej.Dataset;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

public class TFModel {
	volatile Graph graph;

	public TFModel(String model_name) {

		// Reading the graph
		byte[] graph_definition = null;

		try {
			graph_definition = Files.readAllBytes(Paths.get(model_name));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Could not open model import file from " + model_name + ".");
			e.printStackTrace();
		}

		graph = new Graph();

		System.out.println("Loading model from " + model_name);
		System.out.println(graph.toString());

		try {
			graph.importGraphDef(graph_definition);
		} catch (IllegalArgumentException e) {
			System.out.println("Model import file " + model_name + " found, but unable to load model.");
		}

	}
	
	public <T extends RealType<T>> Dataset apply_to_dataset(Dataset currentData) {
		
		int numDims = currentData.numDimensions();
		long[] dims = new long[numDims];
		currentData.dimensions(dims);
		
		int w = (int)currentData.getWidth();
		int h = (int)currentData.getHeight();
		int ch = (int)currentData.getChannels();
		int f = 1;
		
		if(ch > 1){
			if(numDims > 3){
				assert ch == (int)dims[2];
				f = (int)dims[3];				
			}
		}else{
			if(numDims > 2){
				f = (int)dims[2];
			}
		}
		
		assert w == (int)dims[0];
		assert h == (int)dims[1];
		
		System.out.println( "Dataset channels: " + ch );
		System.out.println( "Dataset frames: " + f );
		System.out.println( "Dataset width: " + w );
		System.out.println( "Dataset height: " + h );
		    	
        final Img<?> _image = currentData.getImgPlus();

		final Object type = Util.getTypeFromInterval( _image );
		System.out.println( "Pixel Type: " + type.getClass() );
		System.out.println( "Img Type: " + _image.getClass() );
		
		
		float[][][][] input = new float[f][h][w][ch];
		float[][][][] output = new float[f][h][w][ch];

		//copy input data to array
		
		final Cursor<T> cursor = (Cursor<T>) currentData.localizingCursor();
		float min = currentData.firstElement().getRealFloat();
		float max = currentData.firstElement().getRealFloat();
		while( cursor.hasNext() )
		{
			final T val = cursor.next();
			final int x = cursor.getIntPosition( 0 );
			final int y = cursor.getIntPosition( 1 );
			int col = 0;
			if(ch > 1){
				col = cursor.getIntPosition( 2 );
			}
			int t = 0;
			if(f > 1){
				if(ch > 1){
					t = cursor.getIntPosition( 3 );					
				}else{
					t = cursor.getIntPosition( 2 );
				}
			}
			float fval = val.getRealFloat();
			input[t][y][x][col] = fval;
			if(fval > max) max = fval;
			if(fval < min) min = fval;
		}
		
		//normalize array
		
		for (int col = 0; col < ch; col++) {
			for (int t = 0; t < f; t++) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						input[t][y][x][col] = (input[t][y][x][col]-min)/(max-min);
					}
				}
			}
		}
		
		//apply model to array
		
		Tensor input_t = Tensor.create(input);
		try (Session s = new Session(graph);
				Tensor output_t = s.runner().feed("input_1", input_t).fetch("output").run().get(0)) {
			output_t.copyTo(output);
		}
		catch(IllegalArgumentException e){
			System.out.println("TFModel: Could not run model. Please ensure that you selected the correct model import file for your input data.");
			e.printStackTrace();
		}
		
		//create output dataset
		
		long[] dimensions = new long[numDims];
		currentData.dimensions(dimensions);
		
		Dataset img_out = currentData.factory().create(dimensions, currentData.firstElement());
		
		//write ouput dataset and undo normalization
		
		final Cursor<T> cursor1 = (Cursor<T>) img_out.localizingCursor();
		while( cursor1.hasNext() )
		{
			final T val = cursor1.next();
			final int x = cursor1.getIntPosition( 0 );
			final int y = cursor1.getIntPosition( 1 );
			int col = 0;
			if(ch > 1){
				col = cursor1.getIntPosition( 2 );
			}
			int t = 0;
			if(f > 1){
				if(ch > 1){
					t = cursor1.getIntPosition( 3 );					
				}else{
					t = cursor1.getIntPosition( 2 );
				}
			}
			//set and denormalize
			val.setReal(output[t][y][x][col]*(max-min)+min);
		}
		
		return img_out;
	}

}

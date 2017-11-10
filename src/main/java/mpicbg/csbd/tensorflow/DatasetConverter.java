package mpicbg.csbd.tensorflow;

import net.imagej.tensorflow.Tensors;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.tensorflow.Tensor;

public class DatasetConverter {

	public static RandomAccessibleInterval< FloatType > tensorToDataset( Tensor tensor, int[] mapping ) {

		RandomAccessibleInterval< FloatType > outImg = Tensors.imgFloat( tensor, mapping );
		return Views.dropSingletonDimensions( outImg );
	}

	public static Tensor datasetToTensor(
			RandomAccessibleInterval< FloatType > image,
			int[] mapping ) {

		// Add dimensions until it fits the input tensor
		while ( image.numDimensions() < mapping.length ) {
			image = Views.addDimension( image, 0, 0 );
		}

		// Create the tensor
		return Tensors.tensor( image, mapping );
	}

}

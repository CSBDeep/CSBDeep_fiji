package mpicbg.csbd.tensorflow;

import net.imagej.tensorflow.Tensors;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.tensorflow.Tensor;

import mpicbg.csbd.normalize.Normalizer;

public class DefaultDatasetConverter< T extends RealType< T > > implements DatasetConverter< T > {

	@Override
	public RandomAccessibleInterval< FloatType > tensorToDataset( Tensor tensor, int[] mapping ) {

		RandomAccessibleInterval< FloatType > outImg = Tensors.imgFloat( tensor, mapping );
		return Views.dropSingletonDimensions( outImg );
	}

	@Override
	public Tensor datasetToTensor(
			RandomAccessibleInterval< T > image,
			int[] mapping,
			Normalizer normalizer ) {

		// Normalize the image
		RandomAccessibleInterval< FloatType > im = normalizer.normalizeImage( image );

		// Add dimensions until it fits the input tensor
		while ( im.numDimensions() < mapping.length ) {
			im = Views.addDimension( im, 0, 0 );
		}

		// Create the tensor
		return Tensors.tensor( im, mapping );
	}

}

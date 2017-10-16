package mpicbg.csbd.tensorflow;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.tensorflow.Tensor;

import mpicbg.csbd.normalize.Normalizer;

public interface DatasetConverter< T extends RealType< T > > {

	RandomAccessibleInterval< FloatType > tensorToDataset( Tensor tensor, int[] mapping );

	Tensor datasetToTensor(
			RandomAccessibleInterval< T > image,
			int[] mapping,
			Normalizer normalizer );

}

package mpicbg.csbd.tensorflow;

import net.imagej.Dataset;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;

import org.tensorflow.Tensor;

import mpicbg.csbd.normalize.Normalizer;

public interface DatasetConverter{

	Dataset tensorToDataset( Tensor output_t, DatasetTensorBridge bridge );

	Tensor datasetToTensor( IterableInterval<RealType<?>> image, DatasetTensorBridge bridge, Normalizer normalizer );

}

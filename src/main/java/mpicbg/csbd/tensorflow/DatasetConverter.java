package mpicbg.csbd.tensorflow;

import net.imagej.Dataset;

import org.tensorflow.Tensor;

import mpicbg.csbd.normalize.Normalizer;

public interface DatasetConverter {

	Dataset tensorToDataset( Tensor output_t, DatasetTensorBridge bridge );
	Tensor datasetToTensor( Dataset image, DatasetTensorBridge bridge, Normalizer normalizer );
	
}

package mpicbg.csbd;

import net.imagej.Dataset;

import org.tensorflow.Tensor;

public interface DatasetConverter {

	Dataset tensorToDataset( Tensor output_t, DatasetTensorBridge bridge );
	Tensor datasetToTensor( Dataset image, DatasetTensorBridge bridge, Normalizer normalizer );
	
}

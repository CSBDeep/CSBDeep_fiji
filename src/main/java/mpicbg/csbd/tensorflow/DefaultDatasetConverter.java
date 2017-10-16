package mpicbg.csbd.tensorflow;

import org.tensorflow.Tensor;

import mpicbg.csbd.normalize.Normalizer;
import net.imagej.tensorflow.Tensors;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class DefaultDatasetConverter <T extends RealType<T>> implements DatasetConverter<T> {

	@Override
	public RandomAccessibleInterval<FloatType> tensorToDataset(Tensor tensor, DatasetTensorBridge bridge) {

		// Convert back to an image // TODO if the output has less dimensions than the input we can't use the same mapping
		RandomAccessibleInterval<FloatType> outImg = Tensors.imgFloat(tensor, getMapping(bridge));
//		RandomAccessibleInterval<FloatType> outImg = Tensors.imgDirectFloat(tensor);

		// Remove dimensions in the front if the model added some TODO check if this is needed
//		while (outImg.numDimensions() > bridge.getInitialInputTensorShape().numDimensions()) {
//			outImg = Views.hyperSlice(outImg, 0, 0);
//		}

		// Create the mapping to the original dimensions
//		int[] mapping = getMapping(bridge);
//		int[] reverseMapping = new int[mapping.length];
//		for (int i = 0; i < mapping.length; i++) {
//			reverseMapping[mapping[i]] = i;
//		}
//
//		// Permute back to the original dimensions
//		outImg = permuteDimensions(outImg, reverseMapping);
		return Views.dropSingletonDimensions(outImg);
	}

	@Override
	public Tensor datasetToTensor(RandomAccessibleInterval<T> image, DatasetTensorBridge bridge,
			Normalizer normalizer) {

		// Normalize the image
		RandomAccessibleInterval<FloatType> im = normalizer.normalizeImage(image);
		
		// Add dimensions until it fits the input tensor
		while (im.numDimensions() < bridge.getInitialInputTensorShape().getDimCount()) {
			im = Views.addDimension(im, 0, 0);
		}

		// Permute the dimensions according to the mapping
		int[] mapping = getMapping(bridge);
//		im = permuteDimensions(im, mapping);

		// Create the tensor
		return Tensors.tensor(im, mapping);
	}

	private int[] getMapping(DatasetTensorBridge bridge) {
		int[] mapping = new int[bridge.getInitialInputTensorShape().getDimCount()];
		for (int i = 0; i < mapping.length; i++) {
			// TODO do we have inputs with another dimension count than 5? And does the tfIndex starts with 1 in this cases?
			// Because I once added the following line and it seems strange.
			int tfIndex = i + 5 - bridge.getInitialInputTensorShape().getDimCount();
			mapping[bridge.getMapping(tfIndex)] = i;
		}
		return mapping;
	}
}

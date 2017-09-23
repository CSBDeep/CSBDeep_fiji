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

		// Convert back to an image
		RandomAccessibleInterval<FloatType> outImg = Tensors.img(tensor);

		// Remove dimensions in the front if the model added some
		while (outImg.numDimensions() > bridge.getInitialInputTensorShape().numDimensions()) {
			outImg = Views.hyperSlice(outImg, 0, 0);
		}

		// Create the mapping to the original dimensions
		int[] mapping = getMapping(bridge);
		int[] reverseMapping = new int[mapping.length];
		for (int i = 0; i < mapping.length; i++) {
			reverseMapping[mapping[i]] = i;
		}

		// Permute back to the original dimensions
		outImg = permuteDimensions(outImg, reverseMapping);
		return Views.dropSingletonDimensions(outImg);
	}

	@Override
	public Tensor datasetToTensor(RandomAccessibleInterval<T> image, DatasetTensorBridge bridge,
			Normalizer normalizer) {

		// Normalize the image
		RandomAccessibleInterval<FloatType> im = normalizer.normalizeImage(image);
		
		// Add dimensions until it fits the input tensor
		while (im.numDimensions() < bridge.getInitialInputTensorShape().numDimensions()) {
			im = Views.addDimension(im, 0, 0);
		}

		// Permute the dimensions according to the mapping
		int[] mapping = getMapping(bridge);
		im = permuteDimensions(im, mapping);

		// Create the tensor
		return Tensors.tensor(im);
	}
	
	// TODO maybe put in bridge?
	private static <T> RandomAccessibleInterval<T> permuteDimensions(RandomAccessibleInterval<T> im, int[] mapping) {
		RandomAccessibleInterval<T> output = im;
		int[] mapped = new int[im.numDimensions()];
		for (int i = 0; i < im.numDimensions(); i++ ) {
			mapped[i] = i;
		}
		for (int i = 0; i < im.numDimensions(); i++) {
			int from = mapping[i];
			while (from != mapped[from]) {
				from = mapped[from];
			}
			int to = i;
			output = Views.permute(output, from, to);
			mapped[i] = from;
		}
		return output;
	}
	
	private int[] getMapping(DatasetTensorBridge bridge) {
		int[] mapping = new int[bridge.getInitialInputTensorShape().numDimensions()];
		for (int i = 0; i < mapping.length; i++) {
			mapping[i] = bridge.getMapping(4 - i); // TODO that seems ugly...
		}
		return mapping;
	}
	
	// -------------------------------------------------------- DEBUG OUTPUTS
	private static void printDim(String name, Tensor im) {
		System.out.print(name + ": [ ");
		for (int i = 0; i < im.shape().length; i++) {
			System.out.print(im.shape()[i] + " ");
		}
		System.out.println("]");
	}

	private static void printDim(String name, RandomAccessibleInterval<?> im) {
		System.out.print(name + ": [ ");
		for (int i = 0; i < im.numDimensions(); i++) {
			System.out.print(im.dimension(i) + " ");
		}
		System.out.println("]");
	}
}

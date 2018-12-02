
package de.csbdresden.csbdeep.io;

import java.util.ArrayList;
import java.util.List;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.DefaultTask;
import de.csbdresden.csbdeep.util.DatasetHelper;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class DefaultInputProcessor<T extends RealType<T>> extends DefaultTask
	implements InputProcessor
{

	@Override
	public List<RandomAccessibleInterval<FloatType>> run(final Dataset input, Network network) {

		final List<RandomAccessibleInterval<FloatType>> output = new ArrayList<>();

		setStarted();

		log("Dataset type: " + input.getTypeLabelLong() +
			", converting to FloatType.");
		DatasetHelper.logDim(this, "Dataset dimensions", input);

		RandomAccessibleInterval<FloatType> rai = Converters.convert(
			(RandomAccessibleInterval) input.getImgPlus(),
			new RealFloatConverter<T>(), new FloatType());

		List<Integer> droppedDims = network.dropSingletonDims();

		rai = dropSingletonDimensions(rai, droppedDims);

		// Add dimensions until it fits the input tensor
		while (rai.numDimensions() < network.getInputNode().getNodeShape().length) {
			rai = Views.addDimension(rai, 0, 0);
		}

		output.add(rai);

		setFinished();

		return output;

	}

	protected static RandomAccessibleInterval<FloatType> dropSingletonDimensions(RandomAccessibleInterval<FloatType> rai, List<Integer> droppedDims) {
		RandomAccessibleInterval<FloatType> res = rai;

		for(int d = rai.numDimensions() - 1; d >= 0; --d) {
			if (droppedDims.contains(d)) {
				res = Views.hyperSlice((RandomAccessibleInterval)res, d, rai.min(d));
			}
		}
		return res;
	}

}

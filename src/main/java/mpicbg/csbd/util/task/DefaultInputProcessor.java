
package mpicbg.csbd.util.task;

import java.util.ArrayList;
import java.util.List;

import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.util.DatasetHelper;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class DefaultInputProcessor<T extends RealType<T>> extends DefaultTask
	implements InputProcessor
{

	@Override
	public List<RandomAccessibleInterval<FloatType>> run(final Dataset input) {

		final List<RandomAccessibleInterval<FloatType>> output = new ArrayList<>();

		setStarted();

		log("Dataset type: " + input.getTypeLabelLong() +
			", converting to FloatType.");
		DatasetHelper.logDim(this, "Dataset dimensions", input);

		RandomAccessibleInterval<FloatType> rai = Converters.convert(
			(RandomAccessibleInterval) input.getImgPlus(),
			new RealFloatConverter<T>(), new FloatType());

		output.add(rai);

		setFinished();

		return output;

	}

}

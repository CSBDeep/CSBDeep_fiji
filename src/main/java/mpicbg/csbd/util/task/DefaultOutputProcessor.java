
package mpicbg.csbd.util.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class DefaultOutputProcessor<T extends RealType<T> & NativeType<T>>
	extends DefaultTask implements OutputProcessor<T>
{

	public static String[] OUTPUT_NAMES = { "result" };

	@Override
	public List<Dataset> run(final List<RandomAccessibleInterval<T>> result,
		final Dataset dataset, final AxisType[] axes,
		final DatasetService datasetService)
	{
		setStarted();

		final List<Dataset> output = new ArrayList<>();
		result.forEach(image -> output.addAll(_run(image, dataset, axes,
			datasetService)));

		setFinished();

		return output;
	}

	public List<Dataset> _run(final RandomAccessibleInterval<T> result,
		final Dataset dataset, final AxisType[] axes, DatasetService datasetService)
	{

		final List<Dataset> output = new ArrayList<>();

		if (result != null) {

			log("Displaying " + OUTPUT_NAMES[0] + " image..");
			output.add(wrapIntoDataset(OUTPUT_NAMES[0], result,
				axes, datasetService));
			return output;
		}

		setFailed();

		return output;
	}

	protected Dataset wrapIntoDataset(final String name,
		final RandomAccessibleInterval<T> img, final AxisType[] axes,
		DatasetService datasetService)
	{

		DatasetHelper.logDim(this, "img dim before wrapping into dataset", img);

		// TODO convert back to original format to be able to save and load it
		// (float 32 bit does not load in Fiji) /- note i think we do that now

		final Dataset dataset = datasetService.create(img);
		dataset.setName(name);
		for (int i = 0; i < dataset.numDimensions(); i++) {
			dataset.axis(i).setType(axes[i]);
		}
		return dataset;
	}

}

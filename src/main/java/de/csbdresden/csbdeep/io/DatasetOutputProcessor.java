
package de.csbdresden.csbdeep.io;

import java.util.ArrayList;
import java.util.List;

import de.csbdresden.csbdeep.network.model.ImageTensor;
import de.csbdresden.csbdeep.task.DefaultTask;
import de.csbdresden.csbdeep.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class DatasetOutputProcessor<T extends RealType<T> & NativeType<T>>
	extends DefaultTask implements OutputProcessor<T, Dataset>
{

	public static String[] OUTPUT_NAMES = { "result" };
	private final DatasetService datasetService;

	public DatasetOutputProcessor(DatasetService datasetService) {
		this.datasetService = datasetService;
	}

	@Override
	public Dataset run(final List<RandomAccessibleInterval<T>> result, final ImageTensor node)
	{
		setStarted();

		final List<Dataset> output = new ArrayList<>();
		result.forEach(image -> output.addAll(_run(image, node)));

		setFinished();

		assert(output.size() == 1);

		return output.get(0);
	}

	private List<Dataset> _run(RandomAccessibleInterval<T> result, final ImageTensor node)
	{

		final List<Dataset> output = new ArrayList<>();

		if (result != null) {

			node.setImageShape(result);
			List<Integer> droppedDims = node.dropSingletonDims();
			result = dropSingletonDimensions(result, droppedDims);

			if(result.numDimensions() > 1) {
				log("Displaying " + OUTPUT_NAMES[0] + " image..");
				output.add(wrapIntoDataset(OUTPUT_NAMES[0], result,
						node.getAxesArray(), datasetService));
			} else {
				log("Displaying " + OUTPUT_NAMES[0] + " table..");

			}

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


	protected RandomAccessibleInterval<T> dropSingletonDimensions(RandomAccessibleInterval<T> rai, List<Integer> droppedDims) {
		RandomAccessibleInterval<T> res = rai;

		for(int d = rai.numDimensions() - 1; d >= 0; --d) {
			if (droppedDims.contains(d)) {
				res = Views.hyperSlice((RandomAccessibleInterval)res, d, rai.min(d));
			}
		}
		return res;
	}

}

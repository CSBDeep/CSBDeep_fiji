
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
		final Dataset dataset, final Network network,
		final DatasetService datasetService)
	{
		setStarted();

		final List<Dataset> output = new ArrayList<>();
		result.forEach(image -> output.addAll(_run(image, dataset, network,
			datasetService)));

		setFinished();

		return output;
	}

	public List<Dataset> _run(final RandomAccessibleInterval<T> result,
		final Dataset dataset, final Network network, DatasetService datasetService)
	{

		final List<Dataset> output = new ArrayList<>();

		if (result != null) {

			log("Displaying " + OUTPUT_NAMES[0] + " image..");
			output.add(wrapIntoDataset(OUTPUT_NAMES[0], result,
				network, datasetService));
			return output;
		}

		setFailed();

		return output;
	}

	protected List<RandomAccessibleInterval<T>> splitByLastDim(
		final RandomAccessibleInterval<T> fittedResult)
	{
		final int lastdim = fittedResult.numDimensions() - 1;
		return splitChannels(fittedResult, lastdim);
	}

	protected List<RandomAccessibleInterval<T>> splitByLastNodeDim(
		final RandomAccessibleInterval<T> fittedResult, final Network network)
	{
		int dim;
		if (network.getOutputNode().numDimensions() < fittedResult
			.numDimensions())
		{
			dim = fittedResult.numDimensions() - 1;
		}
		else {
			dim = network.getOutputNode().getDataset().dimensionIndex(network
				.getOutputNode().getNodeAxis(network.getOutputNode()
					.getNodeShape().length - 1));
		}
		return splitChannels(fittedResult, dim);
	}

	protected List<RandomAccessibleInterval<T>> splitChannels(
		final RandomAccessibleInterval<T> img, final int channelDim)
	{

		final ArrayList<RandomAccessibleInterval<T>> res = new ArrayList<>();

		if (channelDim >= 0 && img.dimension(channelDim) > 0) {
			for (int i = 0; i < img.dimension(channelDim); i++) {
				res.add(Views.zeroMin(Views.hyperSlice(img, channelDim, i)));
			}
		}
		else {
			res.add(img);
		}

		return res;
	}

	protected Dataset wrapIntoDataset(final String name,
		final RandomAccessibleInterval<T> img, final Network network,
		DatasetService datasetService)
	{

		DatasetHelper.logDim(this, "img dim before wrapping into dataset", img);

		// TODO convert back to original format to be able to save and load it
		// (float 32 bit does not load in Fiji) /- note i think we do that now

		final Dataset dataset = datasetService.create(img);
		dataset.setName(name);
		for (int i = 0; i < dataset.numDimensions(); i++) {
			Optional<CalibratedAxis> axis = network.getInputNode().getDataset().axis(network
					.getOutputNode().getDimType(i));
			if( !axis.isPresent() ){
				dataset.setAxis(new DefaultLinearAxis(Axes.CHANNEL), i);
			}else {
				dataset.setAxis(axis.get(), i);
			}
		}
		return dataset;
	}

}

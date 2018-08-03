
package mpicbg.csbd.normalize.task;

import mpicbg.csbd.normalize.Normalizer;
import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.task.DefaultTask;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class DefaultInputNormalizer<T extends RealType<T> & NativeType<T>>
	extends DefaultTask implements InputNormalizer<T>
{

	private Normalizer normalizer = new PercentileNormalizer<>();

	@Override
	public Dataset run(Dataset input, OpService opService,
		DatasetService datasetService)
	{

		setStarted();

		log("Normalize .. ");

		final Dataset output = normalizer.normalize(input, opService,
			datasetService);

		setFinished();

		return output;

	}

	public Normalizer getNormalizer() {
		return normalizer;
	}

}

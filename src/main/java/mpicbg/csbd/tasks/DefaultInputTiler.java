package mpicbg.csbd.tasks;

import java.util.List;
import java.util.stream.Collectors;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.Tiling;
import mpicbg.csbd.util.DefaultTask;

public class DefaultInputTiler extends DefaultTask implements InputTiler {

	@Override
	public List< AdvancedTiledView< FloatType > > run(
			final List< RandomAccessibleInterval< FloatType > > input,
			final Dataset dataset,
			final Tiling prediction ) {

		setStarted();

		final List< AdvancedTiledView< FloatType > > output =
				input.stream().map( image -> prediction.preprocess( image, dataset, this ) )//
						.collect( Collectors.toList() );

		setFinished();

		return output;

	}

}

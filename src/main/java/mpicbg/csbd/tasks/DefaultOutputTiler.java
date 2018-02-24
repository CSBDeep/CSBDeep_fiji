package mpicbg.csbd.tasks;

import java.util.List;
import java.util.stream.Collectors;

import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.Tiling;
import mpicbg.csbd.util.DefaultTask;

public class DefaultOutputTiler extends DefaultTask implements OutputTiler {

	@Override
	public List< RandomAccessibleInterval< FloatType > > run(
			final List< AdvancedTiledView< FloatType > > input,
			final Tiling tiling,
			final AxisType[] axisTypes ) {

		setStarted();

		final List< RandomAccessibleInterval< FloatType > > output =
				input.stream().map( image -> tiling.postprocess( this, image, axisTypes ) ).collect(
						Collectors.toList() );

		setFinished();

		return output;
	}

}

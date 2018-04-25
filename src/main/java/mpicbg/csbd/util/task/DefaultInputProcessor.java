package mpicbg.csbd.util.task;

import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.util.DatasetHelper;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import java.util.ArrayList;
import java.util.List;

public class DefaultInputProcessor extends DefaultTask implements InputProcessor {

	@Override
	public List< RandomAccessibleInterval< FloatType > > run( final Dataset input ) {

		final List< RandomAccessibleInterval< FloatType > > output = new ArrayList<>();

		setStarted();

		RandomAccessibleInterval<FloatType> rai = (RandomAccessibleInterval<FloatType>) input.getImgPlus();

		DatasetHelper.logDim( this, "Dataset dimensions", rai );

		output.add( rai );

		setFinished();

		return output;

	}

}

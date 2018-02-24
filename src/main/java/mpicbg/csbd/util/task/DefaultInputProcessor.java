package mpicbg.csbd.util.task;

import java.util.ArrayList;
import java.util.List;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.task.DefaultTask;

public class DefaultInputProcessor extends DefaultTask implements InputProcessor {

	@Override
	public List< RandomAccessibleInterval< FloatType > > run( final Dataset input ) {

		final List< RandomAccessibleInterval< FloatType > > output = new ArrayList<>();

		setStarted();

		output.add( ( RandomAccessibleInterval< FloatType > ) input.getImgPlus() );

		setFinished();

		return output;

	}

}

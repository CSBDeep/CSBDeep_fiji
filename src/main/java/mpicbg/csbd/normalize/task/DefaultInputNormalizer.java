package mpicbg.csbd.normalize.task;

import java.util.List;
import java.util.stream.Collectors;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.normalize.PercentileNormalizer;
import mpicbg.csbd.task.DefaultTask;

public class DefaultInputNormalizer extends DefaultTask implements InputNormalizer {

	private PercentileNormalizer< FloatType > normalizer = new PercentileNormalizer<>();

	@Override
	public List< RandomAccessibleInterval< FloatType > >
			run( final List< RandomAccessibleInterval< FloatType > > input ) {

		setStarted();

		final List< RandomAccessibleInterval< FloatType > > output =
				input.stream().map( image -> normalizeInput( image ) ).collect(
						Collectors.toList() );

		setFinished();

		return output;

	}

	protected RandomAccessibleInterval< FloatType >
			normalizeInput( final RandomAccessibleInterval< FloatType > input ) {

		log( "Normalize [" + normalizer.getInputParameterInfo() + "] .. " );

		return normalizer.normalize( input );
	}

	public PercentileNormalizer<FloatType> getNormalizer() {
		return normalizer;
	}

}

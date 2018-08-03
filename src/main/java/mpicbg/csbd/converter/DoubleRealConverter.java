
package mpicbg.csbd.converter;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author Stephan Saalfeld
 * @author Stephan Preibisch
 */
public class DoubleRealConverter<R extends RealType<R>> implements
	Converter<DoubleType, R>
{

	@Override
	public void convert(final DoubleType input, final R output) {
		output.setReal(input.getRealDouble());
	}
}

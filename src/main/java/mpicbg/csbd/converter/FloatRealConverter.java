
package mpicbg.csbd.converter;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * @author Stephan Saalfeld
 * @author Stephan Preibisch
 */
public class FloatRealConverter<R extends RealType<R>> implements
	Converter<FloatType, R>
{

	@Override
	public void convert(final FloatType input, final R output) {
		output.setReal(input.getRealFloat());
	}
}


package mpicbg.csbd.converter;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

/**
 * @author Stephan Saalfeld
 * @author Stephan Preibisch
 */
public class IntRealConverter<R extends RealType<R>> implements
	Converter<IntType, R>
{

	@Override
	public void convert(final IntType input, final R output) {
		output.setReal(input.getInteger());
	}
}

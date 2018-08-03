
package mpicbg.csbd.converter;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

/**
 * @author Stephan Saalfeld
 * @author Stephan Preibisch
 */
public class RealIntConverter<R extends RealType<R>> implements
	Converter<R, IntType>
{

	@Override
	public void convert(final R input, final IntType output) {
		output.set((int) input.getRealFloat());
	}
}

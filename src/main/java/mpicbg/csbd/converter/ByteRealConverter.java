
package mpicbg.csbd.converter;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;

/**
 * @author Stephan Saalfeld
 * @author Stephan Preibisch
 */
public class ByteRealConverter<R extends RealType<R>> implements
	Converter<ByteType, R>
{

	@Override
	public void convert(final ByteType input, final R output) {
		output.setReal(input.getByte());
	}
}

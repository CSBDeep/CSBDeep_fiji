/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2018 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package mpicbg.csbd.normalize;

import net.imagej.Dataset;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

import org.scijava.ui.UIService;

public class PercentileNormalizer< T extends RealType< T > > implements Normalizer< T > {

//	@Parameter( visibility = ItemVisibility.MESSAGE )
//	protected String normtext = "Normalization";
//    @Parameter(label = "Normalize image")
	protected boolean normalizeInput = true;
//	@Parameter
	protected float percentileBottom = 0.03f;
//	@Parameter
	protected float percentileTop = 0.998f;
//	@Parameter
	protected float min = 0;
//	@Parameter
	protected float max = 1;
//	@Parameter( label = "Clamp normalization" )
	protected boolean clamp = false;

	protected float percentileBottomVal, percentileTopVal;

	protected float factor;

	@Override
	public void testNormalization( final Dataset input, final UIService uiService ) {
		if ( normalizeInput ) {
			final Dataset dcopy = ( Dataset ) input.copy();
			final Cursor< RealType< ? > > cursor = dcopy.cursor();
			//		System.out.println( "percentiles: " + percentileBottomVal + " -> " + percentileTopVal );
			factor = ( max - min ) / ( percentileTopVal - percentileBottomVal );
			if ( clamp ) {
				while ( cursor.hasNext() ) {
					final float val = cursor.next().getRealFloat();
					cursor.get().setReal(
							Math.max(
									min,
									Math.min(
											max,
											( val - percentileBottomVal ) * factor + min ) ) );
				}
			} else {
				while ( cursor.hasNext() ) {
					final float val = cursor.next().getRealFloat();
					cursor.get().setReal(
							Math.max( 0, ( val - percentileBottomVal ) * factor + min ) );
				}
			}
			dcopy.setName( "normalized_" + input.getName() );
			uiService.show( dcopy );
		}
	}

	@Override
	public void prepareNormalization( final IterableInterval< T > input ) {
		if ( normalizeInput ) {
			final float[] ps =
					percentiles( input, new float[] { percentileBottom, percentileTop } );
			percentileBottomVal = ps[ 0 ];
			percentileTopVal = ps[ 1 ];
			factor = ( max - min ) / ( percentileTopVal - percentileBottomVal );
		}
	}

	protected static < T extends RealType< T > > float[] percentiles( final IterableInterval< T > d, final float[] percentiles ) {
		final Cursor< T > cursor = d.cursor();
		int items = 1;
		int i = 0;
		for ( ; i < d.numDimensions(); i++ ) {
			items *= d.dimension( i );
		}
		final float[] values = new float[ items ];
		i = 0;
		while ( cursor.hasNext() ) {
			cursor.fwd();
			values[ i ] = cursor.get().getRealFloat();
			i++;
		}

		Util.quicksort( values );

		final float[] res = new float[ percentiles.length ];
		for ( i = 0; i < percentiles.length; i++ ) {
			res[ i ] = values[ Math.min(
					values.length - 1,
					Math.max( 0, Math.round( ( values.length - 1 ) * percentiles[ i ] ) ) ) ];
		}

		return res;
	}

	@Override
	public boolean isActive() {
		return normalizeInput;
	}

	@Override
	public float normalize( final float val ) {
		if ( clamp ) { return Math.max(
				min,
				Math.min( max, ( val - percentileBottomVal ) * factor + min ) ); }
		return Math.max( 0, ( val - percentileBottomVal ) * factor + min );
	}

	@Override
	public Img< FloatType > normalizeImage( final RandomAccessibleInterval< T > im ) {
		final ImgFactory< FloatType > factory = new ArrayImgFactory<>();
		final Img< FloatType > output = factory.create( im, new FloatType() );

		final RandomAccess< T > in = im.randomAccess();
		final Cursor< FloatType > out = output.localizingCursor();
		while ( out.hasNext() ) {
			out.fwd();
			in.setPosition( out );
			out.get().set( normalize( in.get().getRealFloat() ) );
		}

		return output;
	}

}

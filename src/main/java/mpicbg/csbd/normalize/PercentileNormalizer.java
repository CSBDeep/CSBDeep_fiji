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
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

import net.imglib2.view.Views;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.scijava.ui.UIService;

import java.util.ArrayList;
import java.util.List;

public class PercentileNormalizer< T extends RealType< T > > implements Normalizer< T > {

	protected float[] percentiles;
	protected float[] destValues;
	protected boolean clamp;

	private T min;
	private T max;
	private T percentileBottomVal, percentileTopVal;
	private T factor;

	private static < T extends RealType< T > > List<T>
			percentiles( final IterableInterval< T > d, final float[] percentiles, ImageJ ij ) {

//		Percentile percentile = new Percentile();

		final List<T> res = new ArrayList<>();
		for ( int i = 0; i < percentiles.length; i++ ) {
			T resi = d.firstElement().copy();
			ij.op().stats().percentile(resi, d, (double)percentiles[i]);
			res.add(resi);
		}

		return res;
	}

	@Override
	public T normalize( final T val ) {
		if(clamp) {
			if(val.compareTo(min) < 0) {
				return min.copy();
			}
			if(val.compareTo(max) > 0) {
				return max.copy();
			}
		}
		T res = val.copy();
		res.sub(percentileBottomVal);
		res.mul(factor);
		res.add(min);
		return res;
	}

	@Override
	public Img< FloatType > normalize( final RandomAccessibleInterval< T > im, ImageJ ij ) {

		IterableInterval<T> iterable = ( IterableInterval< T > ) im;
		Cursor<T> cursor = Views.flatIterable(im).cursor();
		cursor.fwd();
		min = cursor.get().copy();
		min.setReal(destValues[0]);
		max = min.copy();
		max.setReal(destValues[1]);

//		final List<T> ps =
//				percentiles( iterable, percentiles, ij );
//		percentileBottomVal = ps.get(0);
//		percentileTopVal = ps.get(1);
//		T resDiff = max.copy();
//		resDiff.sub(min);
//		T srcDiff = percentileTopVal.copy();
//		srcDiff.sub(percentileBottomVal);
//		factor = resDiff.copy();
//		factor.div(srcDiff);

		final Img< FloatType > output = new ArrayImgFactory<>(new FloatType()).create( im );

//		final RandomAccess< T > in = im.randomAccess();
//		final Cursor< FloatType > out = output.localizingCursor();
//		while ( out.hasNext() ) {
//			out.fwd();
//			in.setPosition( out );
//			out.get().set( normalize( in.get() ).getRealFloat() );
//		}
		return output;
	}

	public String getInputParameterInfo() {
		return percentiles[0] + " - " + percentiles[1] + " -> " + destValues[0] + " - " + destValues[1];
	}

	public void setup(final float[] percentiles, final float[] destValues, boolean clamp) {
		assert(percentiles.length == 2);
		assert(destValues.length == 2);
		this.percentiles = percentiles;
		this.destValues = destValues;
		this.clamp = clamp;
	}

}

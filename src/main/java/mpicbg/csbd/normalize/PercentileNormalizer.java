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

import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;
import java.util.List;

public class PercentileNormalizer< T extends RealType< T > & NativeType<T>> implements Normalizer< T > {

	private double[] percentiles;
	private float[] destValues;
	private List<T> resValues;
	private boolean clamp;

	private T min;
	private T max;
	private T factor;

	private static < T extends RealType< T > > List<T>
			percentiles( final IterableInterval< T > d, final float[] percentiles, ImageJ ij ) {

		final List<T> res = new ArrayList<>();
		for (float percentile : percentiles) {
			T resi = d.firstElement().copy();
			ij.op().stats().percentile(resi, d, (double) percentile);
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
		res.sub(resValues.get(0));
		res.mul(factor);
		res.add(min);
		return res;
	}

	@Override
	public Img< T > normalize( final RandomAccessibleInterval< T > src, ImageJ ij ) {

		computePercentiles(src);

		T resDiff = max.copy();
		resDiff.sub(min);
		T srcDiff = resValues.get(1).copy();
		srcDiff.sub(resValues.get(0));
		factor = resDiff.copy();
		factor.div(srcDiff);

		final Img< T > output = new ArrayImgFactory<>(((IterableInterval<T>) src).firstElement()).create( src );

		Cursor<T> srcCursor = ((IterableInterval<T>)src).cursor();
		Cursor<T> dstCursor = output.cursor();

		while(srcCursor.hasNext()) {
			srcCursor.fwd();
			dstCursor.fwd();
			dstCursor.get().set( normalize( srcCursor.get() ) );
		}
		return output;
	}

	private void computePercentiles(RandomAccessibleInterval<T> src) {
		int dimensions = 1;
		for(int i = 0; i < src.numDimensions(); i++) {
			dimensions *= src.dimension(i);
		}

		final Img< T > dst = new DiskCachedCellImgFactory<>(((IterableInterval<T>) src).firstElement())
				.create( dimensions );

		Cursor<T> srcCursor = ((IterableInterval<T>)src).cursor();
		Cursor<T> dstCursor = dst.cursor();

		while(srcCursor.hasNext()) {
			srcCursor.fwd();
			dstCursor.fwd();
			dstCursor.get().set(srcCursor.get());
		}

		min = srcCursor.get().copy();
		min.setReal(destValues[0]);
		max = min.copy();
		max.setReal(destValues[1]);

		MyPercentile<T> percentile = new MyPercentile<>();
		percentile.setData(dst);
		T p1 = percentile.evaluate(percentiles[0]);
		T p2 = percentile.evaluate(percentiles[1]);

		resValues = new ArrayList<>();
		resValues.add(p1);
		resValues.add(p2);
	}

	public String getInputParameterInfo() {
		return percentiles[0] + " - " + percentiles[1] + " -> " + destValues[0] + " - " + destValues[1];
	}

	public void setup(final double[] percentiles, final float[] destValues, boolean clamp) {
		assert(percentiles.length == 2);
		assert(destValues.length == 2);
		this.percentiles = percentiles;
		this.destValues = destValues;
		this.clamp = clamp;
	}

	public List<T> getPercentiles() {
		return resValues;
	}

}

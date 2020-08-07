/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2020 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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

package de.csbdresden.csbdeep.normalize;

import java.util.ArrayList;
import java.util.List;

import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;

public class HistogramPercentile<T extends RealType<T>> implements
	Percentile<T>
{

	T min, max;

//	public float[] computePercentiles2(RandomAccessibleInterval<T> src,
//		final float[] percentiles, OpService opService)
//	{
//
//		float[] resValues = new float[percentiles.length];
//
//		computeMinMax(opService, (IterableInterval<T>) src);
//
//		if (min.compareTo(max) == 0) {
//			Arrays.fill(resValues, min.getRealFloat());
//			return resValues;
//		}
//
//		int numBins = (int) Math.pow(2, 16);
//		List<HistogramBin> bins = createHistogram(numBins,
//			(IterableInterval<T>) src);
//		long pixelCount = ((IterableInterval<T>) src).size();
//
//		for (int i = 0; i < percentiles.length; i++) {
//			resValues[i] = getPercentile(percentiles[i], bins, pixelCount);
//		}
//
//		return resValues;
//
//	}

	public float[] computePercentiles(RandomAccessibleInterval<T> src, final float[] percentiles, OpService opService) {
		final Cursor< T > cursor = ((IterableInterval)src).cursor();
		int items = 1;
		int i = 0;
		for ( ; i < src.numDimensions(); i++ ) {
			items *= src.dimension( i );
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
					Math.max( 0, Math.round( ( values.length - 1 ) * percentiles[ i ] / 100.f ) ) ) ];
		}

//		float[] res2 = computePercentiles2(src, percentiles, opService);
//		for(int j = 0; j < res2.length; j++) {
//			System.out.println(res[j] + " " + res2[j]);
//		}

		return res;
	}

	private void computeMinMax(OpService opService, IterableInterval<T> src) {
		// System.out.println("interval size: " + src.size());
		//
		// long milStart = System.currentTimeMillis();
		// final Pair<T,T> minMax =
		// (Pair<T,T>) opService.run(GenericMinMax.class, src);
		// long timeGeneric = System.currentTimeMillis() - milStart;
		// milStart = System.currentTimeMillis();
		final Pair<T, T> minMax = opService.stats().minMax(src);
		// long timeDefault = System.currentTimeMillis() - milStart;
		//
		// System.out.println("Generic: " + timeGeneric / 1000.);
		// System.out.println("Default: " + timeDefault / 1000.);
		//
		min = minMax.getA();
		max = minMax.getB();
	}

	private List<HistogramBin> createHistogram(int numBins,
		IterableInterval<T> src)
	{
		List<HistogramBin> bins = new ArrayList<>(numBins);
		float binLength = getBinLength(numBins);

		for (int i = 0; i < numBins; i++)
			bins.add(new HistogramBin());

		Cursor<T> cursor = src.cursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			T val = cursor.get();
			int binId = Math.min(numBins - 1, getBin(val, min, binLength));
			HistogramBin bin = bins.get(binId);
			bin.count++;
			if (bin.min == null) bin.min = val.copy();
			else if (bin.min.compareTo(val) > 0) bin.min.set(val.copy());
			if (bin.max == null) bin.max = val.copy();
			else if (bin.max.compareTo(val) < 0) bin.max.set(val.copy());
		}
		bins.get(0).min = min.copy();
		bins.get(bins.size() - 1).max = max.copy();
		return bins;
	}

	private float getBinLength(int numBins) {
		return (max.getRealFloat() - min.getRealFloat()) / (float) numBins;
	}

	private int getBin(T val, T min, float binLength) {
		T res = val.copy();
		res.sub(min);
		res.mul(1. / binLength);
		return (int) res.getRealFloat();
	}

	private float getPercentile(float percentile, List<HistogramBin> bins,
		long pixelCount)
	{
		float border = pixelCount * percentile / 100.f;
		float cur = 0;
		int i;
		float percentileMax, percentileMin;
		if (percentile > 50) {
			border = pixelCount - border;
			for (i = bins.size() - 1; i >= 0 && cur < border; i--) {
				cur += bins.get(i).count;
			}
			percentileMin = (1 - cur / pixelCount) * 100;
			if (i < bins.size() - 1) {
				i++;
				percentileMax = (1 - (cur - bins.get(i).count) / pixelCount) * 100;
			}
			else {
				percentileMax = 100;
			}
		}
		else {
			for (i = 0; i < bins.size() && cur < border; i++) {
				cur += bins.get(i).count;
			}
			percentileMax = cur / pixelCount * 100;
			if (i > 0) {
				i--;
				percentileMin = (cur - bins.get(i).count) / pixelCount * 100;
			}
			else {
				percentileMin = 0;
			}
		}
		HistogramBin bin = bins.get(i);
		if (percentile < percentileMax && percentile > percentileMin) {
			return getY(percentile, percentileMin, bin.min, percentileMax, bin.max);
		}
		else {
			if (percentile < percentileMin) {
				HistogramBin prevBin = getPreviousBin(bins, i);
				if (prevBin != null) return getY(percentile, percentileMin, bin.min,
					percentileMin - 1.f / (float) pixelCount, prevBin.max);
				else return bin.min.getRealFloat();
			}
			else {
				HistogramBin nextBin = getNextBin(bins, i);
				if (nextBin != null) return getY(percentile, percentileMax, bin.max,
					percentileMax + 1.f / (float) pixelCount, nextBin.min);
				else return bin.max.getRealFloat();
			}
		}
	}

	private float getY(float x, float x1, T y1, float x2, T y2) {
		float m = (y2.getRealFloat() - y1.getRealFloat()) / (x2 - x1);
		float n = (y1.getRealFloat() * x2 - y2.getRealFloat() * x1) / (x2 - x1);
		return m*x+n;
	}

	private HistogramBin getPreviousBin(List<HistogramBin> bins, int i) {
		while (i > 0) {
			if (bins.get(--i).count > 0) return bins.get(i);
		}
		return null;
	}

	private HistogramBin getNextBin(List<HistogramBin> bins, int i) {
		while (i < bins.size() - 1) {
			if (bins.get(++i).count > 0) return bins.get(i);
		}
		return null;
	}

	private class HistogramBin {

		int count;
		T min;
		T max;
	}

}

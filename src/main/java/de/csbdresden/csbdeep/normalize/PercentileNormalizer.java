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

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class PercentileNormalizer<T extends RealType<T> & NativeType<T>>
	implements Normalizer
{

	private float[] percentiles = new float[] { 3, 99.7f };
	private float[] destValues = new float[] { 0, 1 };
	private float[] resValues;
	private boolean clip = false;

	protected float min;
	protected float max;
	protected float factor;

	public float normalize( final T val ) {
		if ( clip ) { return Math.max(
				min,
				Math.min( max, ( val.getRealFloat() - resValues[0] ) * factor + min ) ); }
		return Math.max( 0, ( val.getRealFloat() - resValues[0] ) * factor + min );
	}

	@Override
	public Dataset normalize(final Dataset im, OpService opService,
		DatasetService datasetService)
	{
		HistogramPercentile<T> percentile = new HistogramPercentile<>();
		resValues = percentile.computePercentiles((RandomAccessibleInterval<T>) im
			.getImgPlus(), percentiles, opService);
		min = destValues[0];
		max = destValues[1];
		if(resValues[1] - resValues[0] < 0.0000001) factor = 1;
		else factor = (destValues[1] - destValues[0]) / (resValues[1] - resValues[0]);

		long[] dims = new long[im.numDimensions()];
		im.dimensions(dims);
		AxisType[] axes = new AxisType[im.numDimensions()];
		for (int i = 0; i < axes.length; i++) {
			axes[i] = im.axis(i).type();
		}

		final Dataset output = datasetService.create(new FloatType(), dims,
			"normalized input", axes);

		final RandomAccess<T> in = (RandomAccess<T>) im.getImgPlus().randomAccess();
		final Cursor<FloatType> out = (Cursor<FloatType>) output.getImgPlus()
			.localizingCursor();
		while (out.hasNext()) {
			out.fwd();
			in.setPosition(out);
			out.get().set(normalize(in.get()));
		}

		return output;
	}

	@Override
	public void setup(final float[] percentiles, final float[] destValues,
		boolean clip)
	{
		assert (percentiles.length == 2);
		assert (destValues.length == 2);
		this.percentiles = percentiles;
		this.destValues = destValues;
		this.clip = clip;
	}

	public float[] getResValues() {
		return resValues;
	}

}

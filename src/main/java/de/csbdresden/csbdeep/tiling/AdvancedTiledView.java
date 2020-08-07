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

package de.csbdresden.csbdeep.tiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.csbdresden.csbdeep.imglib2.TiledView;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class AdvancedTiledView<T extends RealType<T>> extends TiledView<T> {

	private final Map<AxisType, Long> originalDims;
	private final AxisType[] originalAxes;
	private List<RandomAccessibleInterval<T>> processedTiles;
	// protected int blockMultiple;
	// protected long blockWidth;

	public AdvancedTiledView(final RandomAccessibleInterval<T> source,
		final long[] blockSize, final long[] overlap, final AxisType[] axes)
	{
		super(source, blockSize, overlap);
		processedTiles = new ArrayList<>();
		originalDims = new HashMap<>();
		this.originalAxes = axes;
	}

	public long[] getOverlapComplete() {
		long[] overlap = new long[originalAxes.length];
		for(int i = 0; i < numDimensions(); i++) {
			overlap[i] = super.getOverlap()[i];
		}
		return overlap;
	}

	public Map<AxisType, Long> getOriginalDims() {
		return originalDims;
	}

	public List<RandomAccessibleInterval<T>> getProcessedTiles() {
		return processedTiles;
	}

	public AxisType[] getOriginalAxes() {
		return originalAxes;
	}

	public void dispose() {
		// if(originalDims != null) {
		// originalDims.clear();
		// }
	}
}

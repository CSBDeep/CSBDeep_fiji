/*
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

import java.util.Iterator;

import org.scijava.plugin.Plugin;

import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

/**
 * {@link Op} to calculate the {@code stats.minMax}.
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 * @author Deborah Schmidt
 * @param <I> input type
 */
@Plugin(type = Ops.Stats.MinMax.class, label = "Statistics: MinMax",
	priority = -1)
public class GenericMinMax<I extends RealType<I>> extends
	AbstractUnaryFunctionOp<Iterable<I>, Pair<I, I>> implements Ops.Stats.MinMax
{

	/**
	 * Compute the min and max for any {@link Iterable}
	 *
	 * @param input - the input that has to just be {@link Iterable}
	 */
	public Pair<I, I> calculate(final Iterable<I> input) {

		System.out.println("Generic min max calculator");

		// create a cursor for the image (the order does not matter)
		final Iterator<I> iterator = input.iterator();

		// initialize min and max with the first image value
		I type = iterator.next();

		final I min = type.copy();
		final I max = type.copy();

		// loop over the rest of the data and determine min and max value
		while (iterator.hasNext()) {
			// we need this type more than once
			type = iterator.next();

			if (type.compareTo(min) < 0) min.set(type);

			if (type.compareTo(max) > 0) max.set(type);
		}
		return new ValuePair<>(min, max);
	}
}

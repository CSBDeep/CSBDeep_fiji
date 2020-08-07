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

package de.csbdresden.csbdeep.network.model.tensorflow;

import org.tensorflow.DataType;
import org.tensorflow.Tensor;

import de.csbdresden.csbdeep.converter.*;
import net.imagej.tensorflow.Tensors;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class DatasetTensorFlowConverter {

	public static <T extends RealType<T>, U extends RealType<U>>
		RandomAccessibleInterval<T> tensorToDataset(final Tensor<U> tensor,
			final T res, final int[] mapping, final boolean dropSingletonDims)
	{

		final RandomAccessibleInterval<T> outImg;

		if (tensor.dataType().equals(DataType.DOUBLE)) {
			if (res instanceof DoubleType) {
				outImg = Tensors.imgDouble((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert(
					(RandomAccessibleInterval<DoubleType>) Tensors.imgDouble(
						(Tensor) tensor, mapping), new DoubleRealConverter<T>(), res);
			}
		}
		else if (tensor.dataType().equals(DataType.FLOAT)) {
			if (res instanceof FloatType) {
				outImg = Tensors.imgFloat((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert(
					(RandomAccessibleInterval<FloatType>) Tensors.imgFloat(
						(Tensor) tensor, mapping), new FloatRealConverter<T>(), res);
			}
		}
		else if (tensor.dataType().equals(DataType.INT64)) {
			if (res instanceof LongType) {
				outImg = Tensors.imgLong((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert((RandomAccessibleInterval<LongType>) Tensors
					.imgLong((Tensor) tensor, mapping), new LongRealConverter<T>(), res);
			}
		}
		else if (tensor.dataType().equals(DataType.INT32)) {
			if (res instanceof IntType) {
				outImg = Tensors.imgInt((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert((RandomAccessibleInterval<IntType>) Tensors
					.imgInt((Tensor) tensor, mapping), new IntRealConverter<T>(), res);
			}
		}
		else if (tensor.dataType().equals(DataType.UINT8)) {
			if (res instanceof ByteType) {
				outImg = Tensors.imgByte((Tensor) tensor, mapping);
			}
			else {
				outImg = Converters.convert((RandomAccessibleInterval<ByteType>) Tensors
					.imgByte((Tensor) tensor, mapping), new ByteRealConverter<T>(), res);
			}
		}
		else {
			outImg = null;
		}

		return dropSingletonDims ? Views.dropSingletonDimensions(outImg) : outImg;
	}

	public static <T extends RealType<T>> Tensor datasetToTensor(
		RandomAccessibleInterval<T> image, final int[] mapping)
	{

		Tensor tensor;
		try {
			tensor = Tensors.tensor(image, mapping);
		}
		catch (IllegalArgumentException e) {
			if (image.randomAccess().get() instanceof UnsignedShortType) {
				tensor = Tensors.tensor(Converters.convert(image,
					new RealIntConverter<T>(), new IntType()), mapping);
			}
			else {
				tensor = Tensors.tensor(Converters.convert(image,
					new RealFloatConverter<T>(), new FloatType()), mapping);
			}
		}
		return tensor;
	}

}

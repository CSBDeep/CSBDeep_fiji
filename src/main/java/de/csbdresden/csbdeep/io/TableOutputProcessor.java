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
package de.csbdresden.csbdeep.io;

import java.util.List;

import org.scijava.table.DefaultGenericTable;
import org.scijava.table.GenericTable;

import de.csbdresden.csbdeep.network.model.ImageTensor;
import de.csbdresden.csbdeep.task.DefaultTask;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class TableOutputProcessor<T extends RealType<T>> extends DefaultTask implements OutputProcessor<T, GenericTable> {

		@Override
		public GenericTable run(List<RandomAccessibleInterval<T>> result, ImageTensor node) {

			RandomAccessibleInterval data = result.get(0);
			GenericTable table = new DefaultGenericTable();
			table.setColumnCount((int) Math.max(1, data.dimension(1)));
			table.setRowCount((int) Math.max(1, data.dimension(0)));
			RandomAccess<T> ra = data.randomAccess();
			for (int i = 0; i < data.dimension(0); i++) {
				for (int j = 0; j < data.dimension(1); j++) {
					ra.setPosition(i, 0);
					ra.setPosition(j, 1);
					table.set(j, i, ra.get().copy());
				}
			}

			return table;
		}
	}

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

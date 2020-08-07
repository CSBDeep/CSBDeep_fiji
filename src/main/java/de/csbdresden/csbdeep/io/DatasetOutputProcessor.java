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

import java.util.ArrayList;
import java.util.List;

import de.csbdresden.csbdeep.network.model.ImageTensor;
import de.csbdresden.csbdeep.task.DefaultTask;
import de.csbdresden.csbdeep.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class DatasetOutputProcessor<T extends RealType<T> & NativeType<T>>
	extends DefaultTask implements OutputProcessor<T, Dataset>
{

	public static String[] OUTPUT_NAMES = { "result" };
	private final DatasetService datasetService;

	public DatasetOutputProcessor(DatasetService datasetService) {
		this.datasetService = datasetService;
	}

	@Override
	public Dataset run(final List<RandomAccessibleInterval<T>> result, final ImageTensor node)
	{
		setStarted();

		final List<Dataset> output = new ArrayList<>();
		result.forEach(image -> output.addAll(_run(image, node)));

		setFinished();

		assert(output.size() == 1);

		return output.get(0);
	}

	private List<Dataset> _run(RandomAccessibleInterval<T> result, final ImageTensor node)
	{

		final List<Dataset> output = new ArrayList<>();

		if (result != null) {

			node.setImageShape(result);
			List<Integer> droppedDims = node.dropSingletonDims();
			result = dropSingletonDimensions(result, droppedDims);

			if(result.numDimensions() > 1) {
				log("Displaying " + OUTPUT_NAMES[0] + " image..");
				output.add(wrapIntoDataset(OUTPUT_NAMES[0], result,
						node.getAxesArray(), datasetService));
			} else {
				log("Displaying " + OUTPUT_NAMES[0] + " table..");

			}

			return output;
		}

		setFailed();

		return output;
	}

	protected Dataset wrapIntoDataset(final String name,
		final RandomAccessibleInterval<T> img, final AxisType[] axes,
		DatasetService datasetService)
	{

		DatasetHelper.logDim(this, "img dim before wrapping into dataset", img);

		// TODO convert back to original format to be able to save and load it
		// (float 32 bit does not load in Fiji) /- note i think we do that now

		final Dataset dataset = datasetService.create(img);
		dataset.setName(name);
		for (int i = 0; i < dataset.numDimensions(); i++) {
			dataset.axis(i).setType(axes[i]);
		}
		return dataset;
	}


	protected RandomAccessibleInterval<T> dropSingletonDimensions(RandomAccessibleInterval<T> rai, List<Integer> droppedDims) {
		RandomAccessibleInterval<T> res = rai;

		for(int d = rai.numDimensions() - 1; d >= 0; --d) {
			if (droppedDims.contains(d)) {
				res = Views.hyperSlice((RandomAccessibleInterval)res, d, rai.min(d));
			}
		}
		return res;
	}

}

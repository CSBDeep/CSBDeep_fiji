
package de.csbdresden.csbdeep.network;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.DefaultTask;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.exception.IncompatibleTypeException;

public class DefaultInputValidator extends DefaultTask implements InputValidator {

	@Override
	public void run(final Dataset input, final Network network) throws IncompatibleTypeException {

		setStarted();

		checkForTooManyDimensions(input, network);

		for (int i = 0; i < network.getInputNode().getNodeShape().length; i++) {
			AxisType axis = network.getInputNode().getNodeAxis(i);
			long size = network.getInputNode().getNodeShape()[i];
			checkIfAxesWithFixedSizeExists(input, axis, size);
			checkScalarAxes(input, axis, size);
		}

		setFinished();

	}

	private void checkForTooManyDimensions(Dataset input, Network network) {
		if(network.getInputNode().getNodeShape().length == 4) {
			if(input.dimension(Axes.TIME) > 1 && input.dimension(Axes.Z) > 1) {
				throw new IncompatibleTypeException(input, "Network is meant for 2D images and can handle one additional batch dimension (Z or TIME), " +
						"but this dataset contains data in both Z and TIME dimension.");
			}
		}
	}

	private void checkIfAxesWithFixedSizeExists(Dataset input, AxisType axis, long size) {
		if(size > 1 && !input.axis(axis).isPresent()) {
			throw new IncompatibleTypeException(input, "Input should have axis of type " + axis.getLabel() + " and size " + size);
		}
	}

	private void checkScalarAxes(Dataset input, AxisType axis, long size) {
		if(size == 1 && input.axis(axis).isPresent()  && input.dimension(axis) != size) {
			throw new IncompatibleTypeException(input, "Input axis of type " + axis.getLabel() +
					" should have size " + size + " but has size " + input.dimension(axis));
		}
	}

}

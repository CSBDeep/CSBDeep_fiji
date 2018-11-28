
package org.csbdeep.network;

import java.util.*;

import org.csbdeep.network.model.Network;
import org.csbdeep.task.DefaultTask;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imglib2.exception.IncompatibleTypeException;

public class DefaultInputValidator extends DefaultTask implements InputValidator {

	@Override
	public void run(final Dataset input, final Network network) throws IncompatibleTypeException {

		setStarted();

		for (int i = 0; i < network.getInputNode().getNodeShape().length; i++) {
			AxisType axis = network.getInputNode().getNodeAxis(i);
			long size = network.getInputNode().getNodeShape()[i];
			if(size > 1) {
				if(!input.axis(axis).isPresent()) {
					throw new IncompatibleTypeException(input, "Input should have axis of type " + axis.getLabel());
				}
				if(input.dimension(axis) != size) {
					throw new IncompatibleTypeException(input, "Input axis of type " + axis.getLabel() + " should have size " + size);
				}
			}
		}

		setFinished();

	}


}

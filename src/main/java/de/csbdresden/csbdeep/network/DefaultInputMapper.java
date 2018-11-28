
package de.csbdresden.csbdeep.network;

import java.util.*;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.DefaultTask;
import de.csbdresden.csbdeep.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

public class DefaultInputMapper extends DefaultTask implements InputMapper {

	protected AxisType[] mapping = null;

	private static Map<Character, AxisType> axesMap = Collections.unmodifiableMap(new HashMap<Character, AxisType>() {
		{
			put('X', Axes.X);
			put('Y', Axes.Y);
			put('Z', Axes.Z);
			put('T', Axes.TIME);
			put('C', Axes.CHANNEL);
		}
	});

	@Override
	public void setMapping(final AxisType[] mapping) {
		this.mapping = mapping;
	}

	@Override
	public void run(final Dataset input, final Network network) {

		setStarted();

		DatasetHelper.assignUnknownDimensions(input);

		if (network.isInitialized()) {
			network.initMapping();
		}

//		if (network.getInputNode() != null) {
//			if(mapping != null) {
//				//TODO
//				// network input and output have default dimension reduction.
//				// if the mapping is set to something different for the input, make sure to remove the same dimension slots
//				network.getInputNode().setMapping(mapping);
//			}else {
//				mapping = network.getInputNode().getMapping();
//			}
//		}

		mapping = network.getInputNode().getMapping();

		setFinished();

	}

	public static List<AxisType> parseMappingStr(String mappingStr) {
		List<AxisType> mapping = new ArrayList<>();
		for(int i = 0; i < mappingStr.length(); i++) {
			mapping.add(axesMap.get(mappingStr.charAt(i)));
		}
		return mapping;
	}

	@Override
	public AxisType[] getMapping() {
		return mapping;
	}


}


package mpicbg.csbd.network.task;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;

public class DefaultInputMapper extends DefaultTask implements InputMapper {

	protected AxisType[] mapping = null;

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

		if (mapping != null && network.getInputNode() != null) {
			network.getInputNode().setMapping(mapping);
		}

		setFinished();

	}

}

package mpicbg.csbd.tasks;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.util.DatasetHelper;
import mpicbg.csbd.util.DefaultTask;

public class DefaultInputMapper extends DefaultTask implements InputMapper {

	protected AxisType[] mapping = null;

	@Override
	public void setMapping( final AxisType[] mapping ) {
		this.mapping = mapping;
	}

	@Override
	public void run( final Dataset input, final Network network ) {

		setStarted();

		DatasetHelper.assignUnknownDimensions( input );
		if ( network.isInitialized() ) {
			network.initMapping();
		}

		if ( mapping != null ) {
			applyMapping( network );
		}

		setFinished();

	}

	private void applyMapping( final Network network ) {
		if ( network.getInputNode() != null ) {
			network.getInputNode().setMapping( mapping );
		}
	}

}

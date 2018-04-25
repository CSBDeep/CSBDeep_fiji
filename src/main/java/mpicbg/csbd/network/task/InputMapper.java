package mpicbg.csbd.network.task;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;

public interface InputMapper extends Task {

	public void setMapping( final AxisType[] mapping );

	public void run( Dataset input, Network network );

}

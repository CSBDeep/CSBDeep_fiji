package mpicbg.csbd.network.task;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;

import mpicbg.csbd.network.Network;

public interface InputMapper {

	public void setMapping( final AxisType[] mapping );

	public void run( Dataset input, Network network );

}

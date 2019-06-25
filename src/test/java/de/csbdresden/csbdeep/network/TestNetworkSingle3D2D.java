
package de.csbdresden.csbdeep.network;

import net.imglib2.type.numeric.RealType;

public class TestNetworkSingle3D2D<T extends RealType<T>> extends
		TestNetwork<T>
{

	public TestNetworkSingle3D2D()
	{
		inputShape = new long[]{-1,-1,-1,-1,1};
		outputShape = new long[]{-1,-1,-1,1};
	}

}

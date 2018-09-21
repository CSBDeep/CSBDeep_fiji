
package mpicbg.csbd.network.task;

import net.imagej.DatasetService;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.type.numeric.RealType;

public class TestNetworkSingle3D2D<T extends RealType<T>> extends
		TestNetwork<T>
{

	public TestNetworkSingle3D2D(TensorFlowService tensorFlowService,
	                             DatasetService datasetService)
	{
		super(tensorFlowService, datasetService);
		inputShape = new long[]{-1,-1,-1,-1,1};
		outputShape = new long[]{-1,-1,-1,1};
	}

}

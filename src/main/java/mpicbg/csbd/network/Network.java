
package mpicbg.csbd.network;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.Callable;

import org.scijava.Cancelable;
import org.scijava.Disposable;

import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface Network<T extends RealType<T>> extends
	Callable<List<RandomAccessibleInterval<T>>>, Disposable, Cancelable
{

	void testGPUSupport();

	boolean loadModel(String pathOrURL, String modelName)
		throws FileNotFoundException;

	void preprocess();

	boolean supportsGPU();

	RandomAccessibleInterval<T> execute(RandomAccessibleInterval<T> tile)
		throws Exception;

	Task getStatus();

	ImageTensor getInputNode();

	ImageTensor getOutputNode();

	void loadInputNode(String defaultName, Dataset dataset);

	void loadOutputNode(String defaultName);

	void initMapping();

	boolean isInitialized();

	void resetTileCount();

	void setTiledView(TiledView<T> tiledView);

	/**
	 * Set if singleton dimensions of the output image should be dropped. If the
	 * tile size in one dimension is only one this could remove an important
	 * dimension. Default value is true.
	 */
	void setDropSingletonDims(final boolean dropSingletonDims);

	void setDoDimensionReduction(boolean doDimensionReduction);

	void setDoDimensionReduction(boolean doDimensionReduction,
		AxisType axisToRemove);

	void doDimensionReduction();

	boolean libraryLoaded();
}

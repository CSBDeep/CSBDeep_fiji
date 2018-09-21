
package mpicbg.csbd.network;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.scijava.io.location.Location;

import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.task.Task;
import mpicbg.csbd.util.IOHelper;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public abstract class DefaultNetwork<T extends RealType<T>> implements
	Network<T>
{

	protected Task status;
	protected ImageTensor inputNode = null;
	protected ImageTensor outputNode = null;
	protected TiledView<T> tiledView;
	protected boolean supportsGPU = false;
	protected Integer doneTileCount;
	protected boolean dropSingletonDims = false;
	ExecutorService pool;

	public DefaultNetwork(Task associatedTask) {
		this.status = associatedTask;
	}

	@Override
	public void testGPUSupport() {}

	protected abstract boolean loadModel(Location source, String modelName);

	@Override
	public boolean loadModel(final String pathOrURL, final String modelName)
		throws FileNotFoundException
	{

		final Location source = IOHelper.loadFileOrURL(pathOrURL);
		log("loading model " + modelName + " from source " + source
			.getURI());
		return loadModel(source, modelName);

	}

	@Override
	public abstract void preprocess();

	@Override
	public List<RandomAccessibleInterval<T>> call()
		throws ExecutionException
	{

		pool = Executors.newSingleThreadExecutor();

		final boolean multithreading = false;

		final Cursor<RandomAccessibleInterval<T>> cursor = Views.iterable(tiledView)
			.cursor();

		// Loop over the tiles and execute the prediction
		final List<RandomAccessibleInterval<T>> results = new ArrayList<>();
		final List<Future<RandomAccessibleInterval<T>>> futures = new ArrayList<>();

		while (cursor.hasNext()) {
			final RandomAccessibleInterval<T> tile = cursor.next();

			final Future<RandomAccessibleInterval<T>> future = pool.submit(() -> execute(tile));

			log("Processing tile " + (doneTileCount + 1) + "..");

			futures.add(future);

			if (!multithreading) {
				try {
					final RandomAccessibleInterval<T> res = future.get();
					if (res == null) return null;
					results.add(res);
					upTileCount();
				}
				catch (final InterruptedException exc) {
					pool.shutdownNow();
					fail();
					return null;
				}
			}
		}
		if (multithreading) {
			for (final Future<RandomAccessibleInterval<T>> future : futures) {
				try {
					final RandomAccessibleInterval<T> res = future.get();
					if (res == null) return null;
					results.add(res);
					upTileCount();
				}
				catch (final InterruptedException exc) {
					pool.shutdownNow();
					fail();
					return null;
				}
			}
		}

		return results;
	}

	@Override
	public abstract RandomAccessibleInterval<T> execute(
		RandomAccessibleInterval<T> tile) throws Exception;

	@Override
	public Task getStatus() {
		return status;
	}

	@Override
	public ImageTensor getInputNode() {
		return inputNode;
	}

	@Override
	public ImageTensor getOutputNode() {
		return outputNode;
	}

	@Override
	public void loadInputNode(final Dataset dataset) {
		inputNode = new ImageTensor();
		inputNode.initialize(dataset);
	}

	@Override
	public void loadOutputNode(Dataset dataset) {
		outputNode = new ImageTensor();
		outputNode.initialize(dataset);
	}

	@Override
	public abstract void initMapping();

	@Override
	public abstract boolean isInitialized();

	@Override
	public void resetTileCount() {
		doneTileCount = 0;
		status.setCurrentStep(doneTileCount);
	}

	protected void upTileCount() {
		doneTileCount++;
		status.setCurrentStep(doneTileCount);
	}

	@Override
	public void setTiledView(final TiledView<T> tiledView) {
		this.tiledView = tiledView;
	}

	protected void log(final String text) {
		if (status != null) {
			status.log(text);
		}else {
			System.out.println("[INFO] " + text);
		}
	}

	protected void logError(final String text) {
		if (status != null) {
			status.logError(text);
		}else {
			System.out.println("[ERROR] " + text);
		}
	}

	void fail() {
		status.setFailed();
	}

	@Override
	public void cancel(String reason) {
		pool.shutdownNow();
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public String getCancelReason() {
		return null;
	}

	/**
	 * Set if singleton dimensions of the output image should be dropped. If the
	 * tile size in one dimension is only one this could remove an important
	 * dimension. Default value is true.
	 */
	@Override
	public void setDropSingletonDims(final boolean dropSingletonDims) {
		this.dropSingletonDims = dropSingletonDims;
	}

//	@Override
//	public void setDoDimensionReduction(final boolean doDimensionReduction) {
//		setDoDimensionReduction(doDimensionReduction, Axes.Z);
//	}
//
//	@Override
//	public void setDoDimensionReduction(final boolean doDimensionReduction,
//		final AxisType axisToRemove)
//	{
//		this.doDimensionReduction = doDimensionReduction;
//		this.axisToRemove = axisToRemove;
//	}

	@Override
	public void dispose() {
		if (pool != null) {
			pool.shutdown();
		}
		pool = null;
	}
}


package mpicbg.csbd.network.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.util.DatasetHelper;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class DefaultModelExecutor<T extends RealType<T>> extends DefaultTask
	implements ModelExecutor<T>
{

	private static String PROGRESS_CANCELED = "";
	private ExecutorService pool = null;
	private Network network = null;
	private boolean canceled = false;

	@Override
	public List<AdvancedTiledView<T>> run(final List<AdvancedTiledView<T>> input,
		final Network network) throws OutOfMemoryError
	{
		if(!isCanceled()) {
			setStarted();
			this.network = network;
			if (input.size() > 0) {
				DatasetHelper.logDim(this, "Network input size", input.get(0)
						.randomAccess().get());
			}

			setCurrentStep(0);
			network.resetTileCount();
			setNumSteps(getSteps(input));

			pool = Executors.newWorkStealingPool();
			final List<AdvancedTiledView<T>> output = new ArrayList<>();
			for (AdvancedTiledView<T> tile : input) {
				output.add(run(tile, network));
				if(isCanceled()) return null;
			}
			pool.shutdown();
			if(isCanceled()) return null;
			if (output.size() > 0) {
				DatasetHelper.logDim(this, "Network output size", output.get(0)
						.getProcessedTiles().get(0));
			}
			setFinished();
			return output;
		}
		return null;
	}

	private int getSteps(List<AdvancedTiledView<T>> input) {
		int numSteps = 0;
		for (AdvancedTiledView<T> tile : input) {
			int steps = 1;
			for (int i = 0; i < tile.numDimensions(); i++) {
				steps *= tile.dimension(i);
			}
			numSteps += steps;
		}
		return numSteps;
	}

	private AdvancedTiledView<T> run(final AdvancedTiledView<T> input,
		final Network network) throws OutOfMemoryError
	{

		input.getProcessedTiles().clear();

		try {
			network.setTiledView(input);
			Future<List<RandomAccessibleInterval<T>>> resultFuture = pool.submit(network);
			if(resultFuture != null) {
				List<RandomAccessibleInterval<T>> result = resultFuture.get();
				if(result != null) {
					input.getProcessedTiles().addAll(result);
				}
			}

		}
		catch(RejectedExecutionException e) {
			//canceled
			return null;
		}
		catch (final ExecutionException | IllegalStateException exc) {
			exc.printStackTrace();
			setIdle();
			throw new OutOfMemoryError();
		}
		catch (final InterruptedException exc) {
			logError(PROGRESS_CANCELED);
			setFailed();
			cancel("");
			return null;
		}

		return input;
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void cancel(final String reason) {
		canceled = true;
		if (pool != null && !pool.isShutdown()) {
			pool.shutdownNow();
		}
		if(network != null) {
			network.cancel(reason);
		}
	}

	@Override
	public String getCancelReason() {
		return null;
	}

}

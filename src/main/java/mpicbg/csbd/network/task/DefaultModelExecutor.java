package mpicbg.csbd.network.task;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.util.DatasetHelper;
import net.imglib2.type.numeric.real.FloatType;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DefaultModelExecutor extends DefaultTask implements ModelExecutor {

	protected static String PROGRESS_CANCELED = "";
	protected ExecutorService pool = null;

	@Override
	public List< AdvancedTiledView< FloatType > >
			run( final List< AdvancedTiledView< FloatType > > input, final Network network ) {
		setStarted();
		if(input.size() > 0) {
			DatasetHelper.logDim(this, "Network input size", input.get(0).randomAccess().get());
		}
		pool = Executors.newSingleThreadExecutor();
		final List< AdvancedTiledView< FloatType > > output =
				input.stream().map( tile -> run( tile, network ) ).collect( Collectors.toList() );
		pool.shutdown();
		if(output.size() > 0) {
			DatasetHelper.logDim(this, "Network output size", output.get(0).getProcessedTiles().get(0));
		}
		setFinished();
		return output;
	}

	private AdvancedTiledView< FloatType > run(
			final AdvancedTiledView< FloatType > input,
			final Network network ) throws OutOfMemoryError {

		input.getProcessedTiles().clear();

		try {
			network.setTiledView( input );
			input.getProcessedTiles().addAll( pool.submit( network ).get() );
		} catch ( final ExecutionException exc ) {
			exc.printStackTrace();
			setIdle();
			throw new OutOfMemoryError();
		} catch ( final InterruptedException exc ) {
			logError( PROGRESS_CANCELED );
			setFailed();
			cancel();
			return null;
		}

		return input;
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void cancel( final String reason ) {
		if ( pool != null && !pool.isShutdown() ) {
			pool.shutdownNow();
		}
	}

	@Override
	public String getCancelReason() {
		return null;
	}

}

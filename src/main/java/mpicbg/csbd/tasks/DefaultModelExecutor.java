package mpicbg.csbd.tasks;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.util.DefaultTask;

public class DefaultModelExecutor extends DefaultTask implements ModelExecutor {

	protected static String PROGRESS_CANCELED = "";
	protected ExecutorService pool = Executors.newSingleThreadExecutor();

	@Override
	public List< AdvancedTiledView< FloatType > >
			run( final List< AdvancedTiledView< FloatType > > input, final Network network ) {
		setStarted();
		final List< AdvancedTiledView< FloatType > > output =
				input.stream().map( tile -> run( tile, network ) ).collect( Collectors.toList() );
		setFinished();
		return output;
	}

	private AdvancedTiledView< FloatType > run(
			final AdvancedTiledView< FloatType > input,
			final Network network ) throws OutOfMemoryError {

		setStarted();

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
		setFinished();
		return input;
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void cancel( final String reason ) {
		pool.shutdownNow();
	}

	@Override
	public String getCancelReason() {
		return null;
	}

}

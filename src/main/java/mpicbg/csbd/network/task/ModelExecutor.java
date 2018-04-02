package mpicbg.csbd.network.task;

import java.util.List;

import mpicbg.csbd.task.Task;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.Cancelable;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.tiling.AdvancedTiledView;

public interface ModelExecutor extends Task, Cancelable {

	public List< AdvancedTiledView< FloatType > >
			run( List< AdvancedTiledView< FloatType > > input, Network network );

}

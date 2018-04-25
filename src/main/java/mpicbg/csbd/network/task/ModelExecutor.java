package mpicbg.csbd.network.task;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.Task;
import mpicbg.csbd.tiling.AdvancedTiledView;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Cancelable;

import java.util.List;

public interface ModelExecutor extends Task, Cancelable {

	public List< AdvancedTiledView< FloatType > >
			run( List< AdvancedTiledView< FloatType > > input, Network network );

}


package mpicbg.csbd.network.task;

import java.util.List;

import org.scijava.Cancelable;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.Task;
import mpicbg.csbd.tiling.AdvancedTiledView;
import net.imglib2.type.numeric.RealType;

public interface ModelExecutor<T extends RealType<T>> extends Task, Cancelable {

	List<AdvancedTiledView<T>> run(List<AdvancedTiledView<T>> input,
		Network network);

}

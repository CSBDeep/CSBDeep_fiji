
package org.csbdeep.network;

import java.util.List;

import org.csbdeep.network.model.Network;
import org.scijava.Cancelable;

import org.csbdeep.task.Task;
import org.csbdeep.tiling.AdvancedTiledView;
import net.imglib2.type.numeric.RealType;

public interface ModelExecutor<T extends RealType<T>> extends Task, Cancelable {

	List<AdvancedTiledView<T>> run(List<AdvancedTiledView<T>> input,
		Network network);

}

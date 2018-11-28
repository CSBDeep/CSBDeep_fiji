
package de.csbdresden.csbdeep.network;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.scijava.Cancelable;

import de.csbdresden.csbdeep.network.model.Network;
import de.csbdresden.csbdeep.task.Task;
import de.csbdresden.csbdeep.tiling.AdvancedTiledView;
import net.imglib2.type.numeric.RealType;

public interface ModelExecutor<T extends RealType<T>> extends Task, Cancelable {

	List<AdvancedTiledView<T>> run(List<AdvancedTiledView<T>> input,
		Network network) throws ExecutionException;

}

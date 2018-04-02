package mpicbg.csbd.tiling.task;

import java.util.List;

import mpicbg.csbd.task.Task;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.tiling.AdvancedTiledView;
import mpicbg.csbd.tiling.Tiling;

public interface InputTiler extends Task {

	public List< AdvancedTiledView< FloatType > > run(
			List< RandomAccessibleInterval< FloatType > > input,
			Dataset dataset,
			Tiling prediction );

}

package mpicbg.csbd.network.task;

import java.util.List;

import net.imglib2.type.numeric.real.FloatType;

import org.scijava.Cancelable;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.tiling.AdvancedTiledView;

public interface ModelExecutor extends Cancelable {

	public List< AdvancedTiledView< FloatType > >
			run( List< AdvancedTiledView< FloatType > > input, Network network );

}

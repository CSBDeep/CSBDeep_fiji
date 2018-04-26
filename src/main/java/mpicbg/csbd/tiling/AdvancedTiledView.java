package mpicbg.csbd.tiling;

import mpicbg.csbd.imglib2.TiledView;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedTiledView< T > extends TiledView< T > {

	private final Map< AxisType, Long > originalDims;
	private final AxisType[] originalAxes;
	private List< RandomAccessibleInterval< FloatType > > processedTiles;
//	protected int blockMultiple;
//	protected long blockWidth;
	protected int largestDim;

	public AdvancedTiledView(
			final RandomAccessibleInterval< T > source,
			final long[] blockSize,
			final long[] overlap,
			final AxisType[] axes) {
		super( source, blockSize, overlap );
		processedTiles = new ArrayList<>();
		originalDims = new HashMap<>();
		this.originalAxes = axes;
	}

	public Map< AxisType, Long > getOriginalDims() {
		return originalDims;
	}

	public List< RandomAccessibleInterval< FloatType > > getProcessedTiles() {
		return processedTiles;
	}

	public void setLargestDim( final int largestDimIndex ) {
		this.largestDim = largestDimIndex;
	}

	public int getLargestDim() {
		// TODO Auto-generated method stub
		return largestDim;
	}

	public AxisType[] getOriginalAxes() {
		return originalAxes;
	}

	public void dispose() {
//		if(originalDims != null) {
//			originalDims.clear();
//		}
	}
}

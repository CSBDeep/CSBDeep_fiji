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
	private final Map< AxisType, Long > originalGrid;
	private List< RandomAccessibleInterval< FloatType > > processedTiles;
//	protected int blockMultiple;
//	protected long blockWidth;
	protected int largestDim;

	public AdvancedTiledView(
			final RandomAccessibleInterval< T > source,
			final long[] blockSize,
			final long[] overlap ) {
		super( source, blockSize, overlap );
		processedTiles = new ArrayList<>();
		originalDims = new HashMap<>();
		originalGrid = new HashMap<>();
	}

	public Map< AxisType, Long > getOriginalDims() {
		return originalDims;
	}

	public Map< AxisType, Long > getOriginalGrid() {
		return originalGrid;
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

	public void dispose() {
//		if(originalDims != null) {
//			originalDims.clear();
//		}
	}
}

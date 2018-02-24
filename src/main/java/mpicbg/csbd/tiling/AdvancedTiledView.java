package mpicbg.csbd.tiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.imglib2.TiledView;

public class AdvancedTiledView< T > extends TiledView< T > {

	private final Map< AxisType, Long > originalDims;
	private List< RandomAccessibleInterval< FloatType > > processedTiles;
//	protected int tilesNum;
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
	}

	public Long getOriginalDimSize( final AxisType axis ) {
		return originalDims.get( axis );
	}

	public Map< AxisType, Long > getOriginalDims() {
		return originalDims;
	}

	public List< RandomAccessibleInterval< FloatType > > getProcessedTiles() {
		return processedTiles;
	}

	public void setProcessedTiles(
			final List< RandomAccessibleInterval< FloatType > > processedTiles ) {
		this.processedTiles = processedTiles;
	}

	public void setOriginalDimSize( final AxisType axis, final Long size ) {
		originalDims.put( axis, size );
	}

	public void setLargestDim( final int largestDimIndex ) {
		this.largestDim = largestDimIndex;
	}

	public int getLargestDim() {
		// TODO Auto-generated method stub
		return largestDim;
	}
}

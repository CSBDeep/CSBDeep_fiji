
package mpicbg.csbd.tiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.csbd.imglib2.TiledView;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class AdvancedTiledView<T extends RealType<T>> extends TiledView<T> {

	private final Map<AxisType, Long> originalDims;
	private final AxisType[] originalAxes;
	private List<RandomAccessibleInterval<T>> processedTiles;
	// protected int blockMultiple;
	// protected long blockWidth;

	public AdvancedTiledView(final RandomAccessibleInterval<T> source,
		final long[] blockSize, final long[] overlap, final AxisType[] axes)
	{
		super(source, blockSize, overlap);
		processedTiles = new ArrayList<>();
		originalDims = new HashMap<>();
		this.originalAxes = axes;
	}

	public long[] getOverlapComplete() {
		long[] overlap = new long[originalAxes.length];
		for(int i = 0; i < numDimensions(); i++) {
			overlap[i] = super.getOverlap()[i];
		}
		return overlap;
	}

	public Map<AxisType, Long> getOriginalDims() {
		return originalDims;
	}

	public List<RandomAccessibleInterval<T>> getProcessedTiles() {
		return processedTiles;
	}

	public AxisType[] getOriginalAxes() {
		return originalAxes;
	}

	public void dispose() {
		// if(originalDims != null) {
		// originalDims.clear();
		// }
	}
}

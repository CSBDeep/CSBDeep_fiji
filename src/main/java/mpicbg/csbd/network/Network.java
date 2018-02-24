package mpicbg.csbd.network;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.Callable;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

import mpicbg.csbd.imglib2.TiledView;
import mpicbg.csbd.util.Task;

public interface Network extends Callable< List< RandomAccessibleInterval< FloatType > > > {

	public void loadLibrary();

	public boolean loadModel( String pathOrURL, String modelName ) throws FileNotFoundException;

	public void preprocess();

	public RandomAccessibleInterval< FloatType >
			execute( RandomAccessibleInterval< FloatType > tile ) throws Exception;

	public Task getStatus();

	public ImageTensor getInputNode();

	public ImageTensor getOutputNode();

	public boolean isSupportingGPU();

	public void loadInputNode( String defaultName, Dataset dataset );

	public void loadOutputNode( String defaultName );

	public void initMapping();

	public boolean isInitialized();

	public void setTiledView( TiledView< FloatType > tiledView );

	public void cancel();

	/**
	 * Set if singleton dimensions of the output image should be dropped. If the
	 * tile size in one dimension is only one this could remove an important
	 * dimension. Default value is true.
	 */
	public void setDropSingletonDims( final boolean dropSingletonDims );

	public void setDoDimensionReduction( boolean doDimensionReduction );

	public void setDoDimensionReduction( boolean doDimensionReduction, AxisType axisToRemove );

}

package mpicbg.csbd.util.task;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.task.DefaultTask;
import mpicbg.csbd.util.DatasetHelper;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.List;

public class DefaultOutputProcessor extends DefaultTask implements OutputProcessor {

	public static String[] OUTPUT_NAMES = { "result" };

	@Override
	public List< Dataset > run(
			final List< RandomAccessibleInterval< FloatType > > result,
			final Dataset dataset,
			final Network network,
			final DatasetService datasetService) {
		setStarted();

		final List< Dataset > output = new ArrayList<>();
		result.forEach( image -> output.addAll( _run( image, dataset, network, datasetService ) ) );

		setFinished();

		return output;
	}

	public List< Dataset > _run(
			final RandomAccessibleInterval<FloatType> result,
			final Dataset dataset,
			final Network network, DatasetService datasetService) {

		final List< RandomAccessibleInterval< FloatType > > splittedResult =
				splitByLastNodeDim( result, network );

		final List< Dataset > output = new ArrayList<>();

		if ( result != null ) {
			for ( int i = 0; i < splittedResult.size() && i < OUTPUT_NAMES.length; i++ ) {
				log( "Displaying " + OUTPUT_NAMES[ i ] + " image.." );
				output.add(
						wrapIntoDataset(
								OUTPUT_NAMES[ i ],
								splittedResult.get( i ),
								network,
								datasetService) );
			}
			if ( !output.isEmpty() ) {
				return output;
			}
		}

		setFailed();

		return output;
	}

	protected List< RandomAccessibleInterval< FloatType > >
			splitByLastDim( final RandomAccessibleInterval< FloatType > fittedResult ) {
		final int lastdim = fittedResult.numDimensions() - 1;
		return splitChannels( fittedResult, lastdim );
	}

	protected List< RandomAccessibleInterval< FloatType > > splitByLastNodeDim(
			final RandomAccessibleInterval< FloatType > fittedResult,
			final Network network ) {
		int dim;
		if ( network.getOutputNode().numDimensions() < fittedResult.numDimensions() ) {
			dim = fittedResult.numDimensions() - 1;
		} else {
			dim = network.getOutputNode().getDatasetDimensionIndex(
					network.getOutputNode().getNodeAxis(
							network.getOutputNode().getNodeShape().length - 1 ) );
		}
		return splitChannels( fittedResult, dim );
	}

	protected static List< RandomAccessibleInterval< FloatType > > splitChannels(
			final RandomAccessibleInterval< FloatType > img,
			final int channelDim ) {

		final ArrayList< RandomAccessibleInterval< FloatType > > res = new ArrayList<>();

		if(channelDim >= 0 && img.dimension( channelDim ) > 0) {
			for ( int i = 0; i < img.dimension( channelDim ); i++ ) {
				res.add( Views.zeroMin( Views.hyperSlice( img, channelDim, i ) ) );
			}
		} else {
			res.add(img);
		}

		return res;
	}

	protected < U extends RealType< U > & NativeType< U > > Dataset wrapIntoDataset(
			final String name,
			final RandomAccessibleInterval<U> img,
			final Network network, DatasetService datasetService) {

		DatasetHelper.logDim(this, "img dim before wrapping into dataset", img);

		//TODO convert back to original format to be able to save and load it (float 32 bit does not load in Fiji)
		final Dataset dataset = datasetService.create(
				new ImgPlus<>( ImgView.wrap( img, new ArrayImgFactory<>() ) ) );
		dataset.setName( name );
		for ( int i = 0; i < dataset.numDimensions(); i++ ) {
			dataset.setAxis(
					network.getInputNode().getDataset().axis(
							network.getOutputNode().getDimType( i ) ).get(),
					i );
		}
		// NB: Doesn't work somehow
//		int compositeChannelCount = input.getCompositeChannelCount();
//		dataset.setCompositeChannelCount( compositeChannelCount );
		return dataset;
	}

}

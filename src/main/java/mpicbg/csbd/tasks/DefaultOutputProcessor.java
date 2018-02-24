package mpicbg.csbd.tasks;

import java.util.ArrayList;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.display.DatasetView;
import net.imagej.display.DefaultDatasetView;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import mpicbg.csbd.network.Network;
import mpicbg.csbd.util.DefaultTask;

public class DefaultOutputProcessor extends DefaultTask implements OutputProcessor {

	public static String[] OUTPUT_NAMES = { "result" };

	@Override
	public List< DatasetView > run(
			final List< RandomAccessibleInterval< FloatType > > result,
			final DatasetView datasetView,
			final Network network ) {
		setStarted();

		final List< DatasetView > output = new ArrayList<>();
		result.forEach( image -> output.addAll( _run( image, datasetView, network ) ) );

		setFinished();

		return output;
	}

	public List< DatasetView > _run(
			final RandomAccessibleInterval< FloatType > result,
			final DatasetView datasetView,
			final Network network ) {

		final List< RandomAccessibleInterval< FloatType > > splittedResult =
				splitByLastNodeDim( result, network );

		final List< DatasetView > output = new ArrayList<>();

		if ( result != null ) {
			for ( int i = 0; i < splittedResult.size() && i < OUTPUT_NAMES.length; i++ ) {
				log( "Displaying " + OUTPUT_NAMES[ i ] + " image.." );
				output.add(
						wrapIntoDatasetView(
								OUTPUT_NAMES[ i ],
								splittedResult.get( i ),
								datasetView,
								network ) );
			}
			if ( !output.isEmpty() ) {
				log( "All done!" );
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

		for ( int i = 0; i < img.dimension( channelDim ); i++ ) {
			res.add( Views.zeroMin( Views.hyperSlice( img, channelDim, i ) ) );
		}

		return res;
	}

	protected < U extends RealType< U > & NativeType< U > > DatasetView wrapIntoDatasetView(
			final String name,
			final RandomAccessibleInterval< U > img,
			final DatasetView datasetView,
			final Network network ) {
		final DefaultDatasetView resDatasetView = new DefaultDatasetView();
		final Dataset d = wrapIntoDataset( name, img, network );
		resDatasetView.setContext( d.getContext() );
		resDatasetView.initialize( d );
		resDatasetView.rebuild();

		for ( int i = 0; i < Math.min(
				resDatasetView.getColorTables().size(),
				datasetView.getColorTables().size() ); i++ ) {
			resDatasetView.setColorTable( datasetView.getColorTables().get( i ), i );
		}
		return resDatasetView;
	}

	protected < U extends RealType< U > & NativeType< U > > Dataset wrapIntoDataset(
			final String name,
			final RandomAccessibleInterval< U > img,
			final Network network ) {

		final long[] imgdim = new long[ img.numDimensions() ];
		img.dimensions( imgdim );

		//TODO convert back to original format to be able to save and load it (float 32 bit does not load in Fiji)
		final Dataset dataset = new ImageJ().dataset().create(
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

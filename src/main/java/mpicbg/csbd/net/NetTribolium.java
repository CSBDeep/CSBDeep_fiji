/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd.net;

import java.io.File;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;

import org.scijava.Cancelable;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import mpicbg.csbd.CSBDeepCommand;
import mpicbg.csbd.DatasetTensorBridge;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Tribolium", headless = true )
public class NetTribolium< T extends RealType< T > > extends CSBDeepCommand< T >
		implements
		Command,
		Cancelable {

	public NetTribolium() {

		modelfileUrl = "http://fly.mpi-cbg.de/~pietzsch/CSBDeep-data/net_tribolium.zip";
		modelName = "net_tribolium";
		modelfileName = "model_resunet3_2_5_32.pb";
		inputNodeName = "input";
		outputNodeName = "output";

		header = "This is the tribolium network command.";

	}

	public static void main( final String... args ) throws Exception {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch( args );

		// ask the user for a file to open
		final File file = ij.ui().chooseFile( null, "open" );

		if ( file != null && file.exists() ) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open( file.getAbsolutePath() );

			// show the image
			ij.ui().show( dataset );

			// invoke the plugin
			ij.command().run( NetTribolium.class, true );
		}

	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void cancel( final String reason ) {}

	@Override
	public String getCancelReason() {
		return null;
	}

	@Override
	public void run() {

		final int[] mapping = { DatasetTensorBridge.T,
								DatasetTensorBridge.C,
								DatasetTensorBridge.Y,
								DatasetTensorBridge.X,
								DatasetTensorBridge.Z };
		super.runWithMapping( mapping );

	}
}

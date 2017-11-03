/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package mpicbg.csbd.commands;

import java.io.File;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.RealType;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Planaria", headless = true )
public class NetPlanaria< T extends RealType< T > > extends CSBDeepCommand< T >
		implements
		Command {

	@Override
	public void initialize() {

		super.initialize();

		modelFileUrl =
				"/home/random/Development/imagej/plugins/CSBDeep-data/net_planaria_registered/resunet3_2_3_32__cond_1_2_3_sub_5_nz_16__2017-09-25_23-05-37_896633.zip";
		modelName = "net_planaria";

		header = "This is the planaria network command.";

	}

	public static void main( final String... args ) throws Exception {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch( args );

		// ask the user for a file to open
//		final File file = ij.ui().chooseFile( null, "open" );
		final File file = new File( "../CSBDeep-data/net_planaria_registered/input.tif" );

		if ( file != null && file.exists() ) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open( file.getAbsolutePath() );

			// show the image
			ij.ui().show( dataset );

			// invoke the plugin
			ij.command().run( NetPlanaria.class, true );
		}

	}

	@Override
	public void run() {

		final AxisType[] mapping = { Axes.TIME, Axes.Z, Axes.Y, Axes.X, Axes.CHANNEL };
		super.runWithMapping( mapping );

	}
}

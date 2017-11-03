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
import net.imglib2.type.numeric.RealType;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Project", headless = true )
public class NetProject< T extends RealType< T > > extends CSBDeepCommand< T > implements Command {

	@Override
	public void initialize() {

		super.initialize();

		modelFileUrl =
				"../CSBDeep-data/net_project/model_pro_avg_resnet_3_3x3_32__session_4_5_6_7_cx_0_cy_2_augment__2017-09-20_21-47-38_581863.zip";
		modelName = "net_project";

		header = "This is the projection network command.";

	}

	public static void main( final String... args ) throws Exception {

		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch( args );

		// ask the user for a file to open
//		final File file = ij.ui().chooseFile( null, "open" );
		final File file =
				new File( "../CSBDeep-data/net_project/input.tif" );

		if ( file != null && file.exists() ) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open( file.getAbsolutePath() );

			// show the image
			ij.ui().show( dataset );

			// invoke the plugin
			ij.command().run( NetProject.class, true );
		}

	}

	@Override
	public void run() {
		super.run();
	}
}

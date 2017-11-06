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
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Tribolium", headless = true )
public class NetTribolium< T extends RealType< T > > extends CSBDeepCommand< T >
		implements
		Command {

	@Override
	public void initialize() {

		super.initialize();

		modelFileUrl = "http://csbdeep.bioimagecomputing.com/model-tribolium.zip";
		modelName = "net_tribolium";

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
	public void run() {

		final AxisType[] mapping = { Axes.TIME, Axes.CHANNEL, Axes.Y, Axes.X, Axes.Z };
		super.runWithMapping( mapping );

	}
}

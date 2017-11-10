package mpicbg.csbd.commands;

import java.io.File;
import java.io.IOException;
import java.util.OptionalLong;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.RealType;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>3D Denoising - Tribolium", headless = true )
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

		try {
			validateInput(
					input,
					"3D grayscale image with dimension order X-Y-Z",
					OptionalLong.empty(),
					OptionalLong.empty(),
					OptionalLong.empty() );

			AxisType[] mapping = { Axes.TIME, Axes.Z, Axes.Y, Axes.X, Axes.CHANNEL };
			if ( input.dimension( Axes.Z ) < input.dimension( Axes.CHANNEL ) ) {
				mapping[ 1 ] = Axes.CHANNEL;
				mapping[ 4 ] = Axes.Z;
			}
			super.runWithMapping( mapping );
		} catch ( IOException e ) {
			showError( e.getMessage() );
		}
	}
}

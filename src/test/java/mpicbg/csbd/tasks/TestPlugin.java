package mpicbg.csbd.tasks;

import mpicbg.csbd.commands.NetTubulin;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;

@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>CSBDeepTest" )
public class TestPlugin implements Command {

	@Parameter( type = ItemIO.INPUT )
	private Dataset input;

	@Parameter
	private IOService io;
//
//	@Parameter( type = ItemIO.OUTPUT )
//	private List< DatasetView > output = new ArrayList<>();

	@Override
	public void run() {
		System.out.println("Test");
//		output.add(input);
		try {
			io.save(input, "/home/new.tif");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main( final String... args ) throws Exception {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch( args );

		// ask the user for a file to open
//		final File file = ij.ui().chooseFile( null, "open" );
		final File file =
				new File( "/home/random/Development/imagej/plugins/CSBDeep-data/net_tubulin/input2.tif" );

		if ( file != null && file.exists() ) {
			// load the dataset
			final Dataset dataset = (Dataset) ij.io().open( file.getAbsolutePath() );

			// show the image
			ij.ui().show( dataset );

			// invoke the plugin
			ij.command().run( TestPlugin.class, true, "input", dataset);
		}

	}

}
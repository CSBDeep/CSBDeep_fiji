package mpicbg.csbd.commands;

import net.imagej.Dataset;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>CSBDeepTest" )
public class TestPlugin implements Command {

	@Parameter( type = ItemIO.INPUT )
	private Dataset input;

//	@Parameter( type = ItemIO.OUTPUT )
//	private Dataset output;
//
	@Parameter( type = ItemIO.OUTPUT )
	private List< Dataset > output = new ArrayList<>();

	@Override
	public void run() {
		System.out.println("Test");
		System.out.println(input);
		output.add(input);
		System.out.println(output);

	}

}
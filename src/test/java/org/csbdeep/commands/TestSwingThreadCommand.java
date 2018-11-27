package org.csbdeep.commands;

import java.util.concurrent.ExecutionException;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.download.DownloadService;
import org.scijava.io.location.BytesLocation;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.plugin.Parameter;
import org.scijava.task.Task;
import org.scijava.thread.ThreadService;
import org.scijava.util.ByteArray;
import org.scijava.widget.Button;

import net.imagej.ImageJ;

public class TestSwingThreadCommand implements Command {

	@Parameter
	DownloadService downloadService;

	@Parameter
	Context context;

	@Parameter
	ThreadService threadService;

	@Parameter(label = "Download ZIP",
			callback = "downloadZipButton")
	private Button button;

	private void downloadZipButton() {
		threadService.run(() -> {
			System.out.println("Download ZIP from button");
			downloadZip();
			System.out.println("Done button action");
		});
	}

	@Override
	public void run() {
		System.out.println("Download ZIP from run method");
		downloadZip();
		System.out.println("Done run action");
	}

	private void downloadZip() {

		ByteArray byteArray = new ByteArray(1048576);
		BytesLocation bytes = new BytesLocation(byteArray);

		final Location zipLocation = new FileLocation(getClass().getResource("denoise2D/model.zip").getPath());

		Task task = this.downloadService.download(zipLocation, bytes).task();

		try {
			task.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) {
		final ImageJ ij = new ImageJ();
		ij.launch(args);
		ij.command().run(TestSwingThreadCommand.class, true);
	}

}

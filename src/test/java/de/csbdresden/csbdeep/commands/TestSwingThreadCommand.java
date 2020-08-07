/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2020 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.csbdresden.csbdeep.commands;

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

package de.csbdresden.csbdeep.ui;

import java.io.IOException;
import java.net.URL;

import de.csbdresden.csbdeep.network.model.tensorflow.LibraryVersion;
import net.imagej.ImageJService;

public interface TensorFlowInstallationService extends ImageJService {
	void loadNativeLibrary();

	void removeAllFromLib();
	void installLib(LibraryVersion version);
	void downloadLib(URL url) throws IOException;
	void checkStatus(LibraryVersion version);
	LibraryVersion getCurrentVersion();
}

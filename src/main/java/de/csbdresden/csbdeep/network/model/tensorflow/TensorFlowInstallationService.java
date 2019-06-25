package de.csbdresden.csbdeep.network.model.tensorflow;

import java.io.IOException;
import java.net.URL;

import de.csbdresden.csbdeep.network.model.tensorflow.LibraryVersion;
import net.imagej.tensorflow.TensorFlowService;

public interface TensorFlowInstallationService extends TensorFlowService {
	void loadNativeLibrary();
	void removeAllFromLib();
	void installLib(LibraryVersion version);
	void downloadLib(URL url) throws IOException;
	void checkStatus(LibraryVersion version);
	LibraryVersion getCurrentVersion();
	String getStatus();
	boolean libraryLoaded();
}

package de.csbdresden.csbdeep.network.model.tensorflow;

import java.io.IOException;
import java.net.URL;

import net.imagej.tensorflow.TensorFlowService;

public interface TensorFlowInstallationService extends TensorFlowService {
	void loadLibrary();
	void removeAllFromLib();
	boolean installLib(LibraryVersion version) throws IOException;
	void downloadLib(URL url) throws IOException;
	void checkStatus(LibraryVersion version);
	LibraryVersion getCurrentVersion();
	String getStatus();
	boolean libraryLoaded();
	LibraryVersion getDefaultVersion();
}

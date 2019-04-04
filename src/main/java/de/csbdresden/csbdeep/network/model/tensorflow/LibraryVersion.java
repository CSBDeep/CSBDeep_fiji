package de.csbdresden.csbdeep.network.model.tensorflow;

public class LibraryVersion {
	private final String url;
	public String operatingSystem;
	public String version;
	public String compatibleCUDAVersion;
	public String compatibleCuDNNVersion;

	public LibraryVersion(String url, String os, String version, String cudaVersion, String cudnnVersion) {
		this.operatingSystem = os;
		this.url = url;
		this.version = version;
		this.compatibleCUDAVersion = cudaVersion;
		this.compatibleCuDNNVersion = cudnnVersion;
	}
}

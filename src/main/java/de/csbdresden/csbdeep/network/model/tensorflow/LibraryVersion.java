package de.csbdresden.csbdeep.network.model.tensorflow;

public class LibraryVersion {
	public String url;
	public String platform;
	public String tfVersion;
	public String cuda;
	public String cudnn;
	public String gpu;

	public String localPath;

	public boolean active;
	public boolean downloaded = false;

	public LibraryVersion(String url, String gpu, String os, String tfVersion, String cudaVersion, String cudnnVersion) {
		this.platform = os;
		this.url = url;
		this.gpu = gpu;
		this.tfVersion = tfVersion;
		this.cuda = cudaVersion;
		this.cudnn = cudnnVersion;
	}

	public LibraryVersion() {}

	@Override
	public boolean equals(Object other) {
		return platform.equals(((LibraryVersion)other).platform)
			&& gpu.equals(((LibraryVersion)other).gpu)
			&& tfVersion.equals(((LibraryVersion)other).tfVersion);
	}

	@Override
	public String toString() {
		String text = "TF " + tfVersion + " " + gpu;
		if(cuda != null || cudnn != null) {
			text += " (";
			if(cuda != null) text += "CUDA " + cuda;
			if(cuda != null && cudnn != null) text += ", ";
			if(cudnn != null) text += "CuDNN " + cudnn;
			text += ")";
		}
		if(downloaded) text += " (downloaded)";
		return text;
	}

	public String getNote() {
		if(downloaded) return "Downloaded to " + localPath;
		return "Downloadable from " + url;
	}

	public String getOrderableTFVersion() {
		String orderableVersion = "";
		String[] split = tfVersion.split("\\.");
		for(String part : split) {
			orderableVersion += String.format("%03d%n", Integer.valueOf(part));
		}
		return orderableVersion;
	}
}

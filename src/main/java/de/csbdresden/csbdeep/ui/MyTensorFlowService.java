package de.csbdresden.csbdeep.ui;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.scijava.app.AppService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.tensorflow.TensorFlow;

import de.csbdresden.csbdeep.network.model.tensorflow.LibraryVersion;
import net.imagej.tensorflow.DefaultTensorFlowService;
import net.imagej.updater.util.UpdaterUtil;

@Plugin(type = TensorFlowInstallationService.class)
public class MyTensorFlowService extends DefaultTensorFlowService implements TensorFlowInstallationService {

	private static final String TFVERSIONFILE = ".tensorflowversion";
	@Parameter
	AppService appService;

	public static String DOWNLOADDIR = "downloads/";
	public static String LIBDIR = "lib/";

	String platform = UpdaterUtil.getPlatform();

	@Parameter
	LogService logService;

	@Override
	public void loadNativeLibrary() {
//		log("The current library path is: LD_LIBRARY_PATH=" + System.getenv(
//				"LD_LIBRARY_PATH"));
		try {
			System.loadLibrary("tensorflow_jni");
		}
		catch (final UnsatisfiedLinkError e) {
			if(e.getMessage().contains("no tensorflow_jni")) {
				logService.info("No native TF library found.");
			} else {
				logService.error("Could not load native TF library " + e.getMessage());
//				e.printStackTrace();
				//TODO maybe ask if the native lib should be deleted from /lib
//				removeAllFromLib();
			}
		}
	}

	@Override
	public void removeAllFromLib()  {
		File folder = new File(getLibDir() + "/" + platform);
		if(!folder.exists()) {
			return;
		}
		File[] listOfFiles = folder.listFiles();
		for (File file : listOfFiles) {
			if (file.getName().toLowerCase().contains("tensorflow")) {
				logService.info("Deleting " + file);
				file.delete();
			}
		}
	}

	@Override
	public void installLib(LibraryVersion version) {
		logService.info("Installing " + version);
		if(version.localPath.contains(".zip")) {
			unZip(version.localPath, getLibDir() + version.platform + "/");
		}
		else if (version.localPath.endsWith(".tar.gz")) {
			unGZip(version.localPath, getLibDir() + version.platform + "/");
		}
		writeNativeVersionFile(version);
	}

	private void unGZip(String tarGzFile, String outputFolder) {
		logService.info("Unpacking " + tarGzFile + " to " + outputFolder);
		try{
			File folder = new File(outputFolder);
			if(!folder.exists()){
				folder.mkdirs();
			}

			String tarFileName = tarGzFile.replace(".gz", "");
			FileInputStream instream= new FileInputStream(tarGzFile);
			GZIPInputStream ginstream =new GZIPInputStream(instream);
			FileOutputStream outstream = new FileOutputStream(tarFileName);
			byte[] buf = new byte[1024];
			int len;
			while ((len = ginstream.read(buf)) > 0)
			{
				outstream.write(buf, 0, len);
			}
			ginstream.close();
			outstream.close();
			TarArchiveInputStream myTarFile=new TarArchiveInputStream(new FileInputStream(tarFileName));
			TarArchiveEntry entry;
			while ((entry = myTarFile.getNextTarEntry()) != null) {
				File output =  new File(folder + "/" + entry.getName());
				if(! output.getParentFile().exists()){
					output.getParentFile().mkdirs();
				}
				if(output.isDirectory()) continue;
				byte[] content = new byte[(int) entry.getSize()];
				int offset=0;
				myTarFile.read(content, offset, content.length - offset);
				logService.info("Writing " + output);
				FileOutputStream outputStream = new FileOutputStream(output);
				outputStream.write(content);
				outputStream.close();
			}
			myTarFile.close();
			File tarFile =  new File(tarFileName);
			tarFile.delete();
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}

	@Override
	public void downloadLib(URL url) throws IOException {
		createDownloadDir();
		String filename = url.getFile().substring(url.getFile().lastIndexOf("/")+1);
		String localFile = getDownloadDir() + filename;
		logService.info("Downloading " + url + " to " + localFile);
		InputStream in = url.openStream();
		Files.copy(in, Paths.get(localFile), StandardCopyOption.REPLACE_EXISTING);
	}

	private void createDownloadDir() {
		File downloadDir = new File(getDownloadDir());
		if(!downloadDir.exists()) {
			downloadDir.mkdirs();
		}
	}

	/**
	 * Unzip it
	 * @param zipFile input zip file
	 * @param outputFolder zip file output folder
	 */
	public void unZip(String zipFile, String outputFolder){

		logService.info("Unpacking " + zipFile + " to " + outputFolder);

		byte[] buffer = new byte[1024];

		try{
			File folder = new File(outputFolder);
			if(!folder.exists()){
				folder.mkdirs();
			}

			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry ze = zis.getNextEntry();

			while(ze!=null){

				String fileName = ze.getName().replace("Fiji.app/", "");
				File newFile = new File(outputFolder + File.separator + fileName);

				logService.info("Writing " + newFile.getAbsoluteFile());

				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				ze = zis.getNextEntry();

			}

			zis.closeEntry();
			zis.close();

		}catch(IOException ex){
			ex.printStackTrace();
		}
	}

	@Override
	public void checkStatus(LibraryVersion version) {
		if(version.url.startsWith("file:")) {
			if(new File(version.url.replace("file:", "")).exists()) {
				version.localPath = version.url.replace("file:", "");
				version.downloaded = true;
			}
		} else {
			String path = getDownloadDir() + getNameFromURL(version.url);
			if(new File(path).exists()) {
				version.localPath = path;
				version.downloaded = true;
			}
		}
	}

	private boolean hasNativeVersion() {
		return new File(getNativeVersionFile()).exists();
	}

	private LibraryVersion getNativeVersion() {
		String versionstr = null;
		try {
			versionstr = new String(Files.readAllBytes(Paths.get(getNativeVersionFile())));
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] parts = versionstr.split(",");
		LibraryVersion version = new LibraryVersion();
		version.platform = parts[0];
		version.gpu = parts[1];
		version.tfVersion = parts[2];
		version.url = parts[3];
		logService.info("Active native TF version: " + version);
		version.active = true;
		return version;
	}

	private void writeNativeVersionFile(LibraryVersion version) {
		String str = version.platform + "," + version.gpu + "," + version.tfVersion + "," + version.localPath;
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(getNativeVersionFile()))){
			writer.write(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private LibraryVersion getJARVersion() {
		LibraryVersion version = new LibraryVersion();
		version.tfVersion = TensorFlow.version();
		version.platform = platform;
		String main = TensorFlow.class.getResource("TensorFlow.class").getPath();
		main = main.replace("!/org/tensorflow/TensorFlow.class", "");
		version.url = main;
		main = main.substring(0, main.lastIndexOf("."));
		main = main.substring(main.lastIndexOf("/")+1, main.lastIndexOf(version.tfVersion));
		if(main.contains("gpu")) {
			version.gpu = "GPU";
		} else {
			version.gpu = "CPU";
		}
		version.active = true;
		logService.info("Active JAR TF version: " + version);
		return version;
	}

	@Override
	public LibraryVersion getCurrentVersion() {
		if(hasNativeVersion())
			return getNativeVersion();
		else return getJARVersion();
	}

	private String getNameFromURL(String url) {
		String[] parts = url.split("/");
		return parts[parts.length-1];
	}

	private String getDownloadDir() {
		return appService.getApp().getBaseDirectory().getAbsolutePath() + "/" + DOWNLOADDIR;
	}

	private String getLibDir() {
		return appService.getApp().getBaseDirectory().getAbsolutePath() + "/" + LIBDIR;
	}

	private String getNativeVersionFile() {
		return appService.getApp().getBaseDirectory().getAbsolutePath() + "/" + LIBDIR + platform + "/" + TFVERSIONFILE;
	}

}

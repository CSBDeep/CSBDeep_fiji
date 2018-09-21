
package mpicbg.csbd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import mpicbg.csbd.commands.GenericNetwork;
import org.scijava.io.http.HTTPLocation;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.module.MethodCallException;

public class IOHelper {

	public static Location loadFileOrURL(final String path)
		throws FileNotFoundException
	{
		if (path == null) {
			throw new FileNotFoundException("No path specified");
		}
		final File file = new File(path);
		Location source;
		if (!file.exists()) {
			try {
				source = new HTTPLocation(path);
			}
			catch (MalformedURLException | URISyntaxException exc) {
				throw new FileNotFoundException("Could not find file or URL: " + path);
			}
		}
		else {
			source = new FileLocation(file);
		}
		return source;

	}

	public static boolean urlExists(String url) {
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection con = null;
		boolean existingUrl = false;
		try {
			con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod("HEAD");
			existingUrl = con.getResponseCode() == HttpURLConnection.HTTP_OK;
		} catch (IOException | IllegalArgumentException e) {
		} finally {
			con.disconnect();
		}
		return existingUrl;
	}

	public static String getFileCacheName(Class<? extends GenericNetwork> parentClass, File file) throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
			return parentClass.getSimpleName() + "_" + md5;
		} catch (IOException e) {
			throw e;
		} finally {
			if(fis != null) {
				fis.close();
			}
		}
	}

	public static String getUrlCacheName(Class<? extends GenericNetwork> parentClass, String modelUrl) throws IOException {
		URL url = new URL(modelUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		Long dateTime = connection.getLastModified();
		connection.disconnect();
		ZonedDateTime urlLastModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateTime), ZoneId.of("GMT"));

		return parentClass.getSimpleName()
				+ "_" + url.getPath().replace(".zip", "").replace("/", "")
				+ "_" + DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss").format(urlLastModified);
	}
}

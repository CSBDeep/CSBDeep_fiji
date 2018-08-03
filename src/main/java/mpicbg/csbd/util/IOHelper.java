
package mpicbg.csbd.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.scijava.io.http.HTTPLocation;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;

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

}

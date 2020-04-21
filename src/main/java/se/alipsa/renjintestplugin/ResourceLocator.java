package se.alipsa.renjintestplugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class ResourceLocator {

  private static Logger logger = LoggerFactory.getLogger(ResourceLocator.class);


  /**
   * Find a file using available class loaders.
   * @param name the resource name
   * @param encodingOpt optional file encoding (default is UTF-8)
   * @return the file corresponding to the resource name
   * @throws FileNotFoundException if the resource cannot be found
   */
  public static File getResourceAsFile(String name, String... encodingOpt) throws FileNotFoundException {
    String encoding = encodingOpt.length > 0 ? encodingOpt[0] : "UTF-8";
    URL url = getResourceAsUrl(name);
    File file;
    try {
      if (url == null) {
        throw new FileNotFoundException("Failed to find file " + name);
      }
      file = new File(URLDecoder.decode(url.getFile(), encoding));
    } catch (UnsupportedEncodingException e) {
      throw new FileNotFoundException("Failed to find resource " + name + ", url is " + url);
    }
    return file;
  }

  /**
   * Find a resource using available class loaders.
   * It will also load resources/files from the
   * absolute path of the file system (not only the classpath's).
   * <p>
   * For OSGI resources different bundles might have different classloaders
   * so pass the class of the bundle where the resources are to find them.</p>
   * @param resource the resource name
   * @param caller the (optional) calling class useful for e.g. OSGI scenarios
   * @return a URL to the resource or null if no resource found
   */
  public static URL getResourceAsUrl(String resource, Class... caller) {
    final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
    classLoaders.add(Thread.currentThread().getContextClassLoader());
    classLoaders.add(ResourceLocator.class.getClassLoader());
    if (caller.length > 0) {
      classLoaders.add(caller[0].getClassLoader());
    }

    for (ClassLoader classLoader : classLoaders) {
      final URL url = getResourceWith(classLoader, resource);
      if (url != null) {
        return url;
      }
    }

    final URL systemResource = ClassLoader.getSystemResource(resource);
    if (systemResource != null) {
      return systemResource;
    } else {
      try {
        File file = new File(resource);
        if (file.exists()) {
          return file.toURI().toURL();
        } else {
          logger.info("Failed to find {}", resource);
          return null;
        }
      } catch (MalformedURLException e) {
        logger.warn("Failed to find {}, {}", resource, e);
        return null;
      }
    }
  }

  private static URL getResourceWith(ClassLoader classLoader, String resource) {
    if (classLoader != null) {
      return classLoader.getResource(resource);
    }
    return null;
  }
}

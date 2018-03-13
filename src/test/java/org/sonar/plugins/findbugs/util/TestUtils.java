package org.sonar.plugins.findbugs.util;

import javax.annotation.CheckForNull;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class TestUtils {

    private TestUtils() {
        // utility class
    }

    /**
     * Search for a test resource in the classpath. For example getResource("org/sonar/MyClass/foo.txt");
     *
     * @param path the starting slash is optional
     * @return the resource. Null if resource not found
     */
    @CheckForNull
    public static File getResource(String path) {
        String resourcePath = path;
        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }
        URL url = TestUtils.class.getResource(resourcePath);
        if (url != null) {
            try {
                return new File(url.toURI());
            } catch (URISyntaxException e) {
                return null;
            }
        }
        return null;
    }
}

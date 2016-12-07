package com.dotcms.util;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple util that determine if the stream path is a file or classpath.
 * Depending on it will return the right InputStream
 *
 * If the resource name parameter starts with file:// will return a FileInputStream, otherwise will try to get from the classpath.
 */
public class InputStreamUtils {

    private final static String PREFIX_FILE = "file://"; // file://

    /**
     * If the resourceName start with {@link InputStreamUtils}.PREFIX_FILE ("file://")
     * will load the stream from the file system, otherwise will be get from the classpath.
     *
     * @param resourceName {@link String}
     * @return InputStream
     * @throws IOException
     */
    public static InputStream getInputStream (final String resourceName) throws IOException {

        InputStream inputStream = null;

        if (StringUtils.startsWith(resourceName, PREFIX_FILE)) {

            final String normalizedName = resourceName.
                    replaceFirst(PREFIX_FILE, StringUtils.EMPTY);
            inputStream = new FileInputStream(normalizedName);
        } else {

            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            inputStream = classLoader.getResourceAsStream(resourceName);
        }

        return inputStream;
    } // getInputStream.

} // E:O:F:InputStreamUtils.

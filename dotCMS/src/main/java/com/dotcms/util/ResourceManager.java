package com.dotcms.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {

    // Stores paths to files with the global jarFilePath as the key
    private static Map<String, Optional<Path>> fileCache = new ConcurrentHashMap<>();


    /**
     * Extract the specified resource from inside the jar to the local file system.
     *
     * @param jarFilePath absolute path to the resource
     * @return full file system path if file successfully extracted, else null on error
     */
    public static Optional<Path> getResource(String jarFilePath) {

        if (jarFilePath == null) {
            return null;
        }

        // Slash on start of path only used for .getClass().getResource() to indicate
        // not to use relative path.  resource using classloader is always absolute and does not
        // use initial slash

        if (jarFilePath.startsWith("/"))
            jarFilePath=jarFilePath.substring(1);

        // See if we already have the file
        if (fileCache.containsKey(jarFilePath)) {
            return fileCache.get(jarFilePath);
        }

        // Alright, we don't have the file, let's extract it

        // Read the file we're looking for
        URL resourceURL = Thread.currentThread().getContextClassLoader().getResource(jarFilePath);
        if (resourceURL == null) {
            // If resource does not exist do not keep trying
            return cacheResult(jarFilePath, null);
        }

        if (!resourceURL.getPath().contains(".jar!")) {
            // If resource is a local file then just return that file
            File localFile = new File(resourceURL.getPath());
            if (localFile.exists()) {
                return cacheResult(jarFilePath, localFile.toPath());
            }
        }

        // Extract resource from file
        try (InputStream fileStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(jarFilePath);
        ) {
            // Grab the file name
            String[] chopped = jarFilePath.split("\\/");
            String fileName = chopped[chopped.length - 1];

            // Create our temp file (first param is just random bits)
            File tempFile = File.createTempFile("resFile", fileName);

            // Set this file to be deleted on VM exit
            tempFile.deleteOnExit();

            // Create an output stream to barf to the temp file
            OutputStream out = new FileOutputStream(tempFile);

            // Write the file to the temp file
            byte[] buffer = new byte[1024];
            int len = fileStream.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = fileStream.read(buffer);
            }

            return cacheResult(jarFilePath, tempFile.toPath());

        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<Path> cacheResult(String jarFilePath, Path path) {
        Optional<Path> result = Optional.ofNullable(path);
        fileCache.put(jarFilePath, result);
        return result;
    }
}

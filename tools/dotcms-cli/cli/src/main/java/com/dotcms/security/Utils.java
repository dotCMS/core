package com.dotcms.security;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class Utils {

    private Utils() {
        //Hide public constructor
    }

    /**
     * Figure out the sha256 of the file content, assumes that the file exists and can be read
     *
     * @param path {@link Path}
     * @return String just as unix sha returns
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String Sha256toUnixHash(final Path path) {

        try {
            final HashBuilder sha256Builder = Encryptor.Hashing.sha256();
            final byte[] buffer = new byte[4096];
            int countBytes;

            try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {

                countBytes = inputStream.read(buffer);
                while (countBytes > 0) {

                    sha256Builder.append(buffer, countBytes);
                    countBytes = inputStream.read(buffer);
                }
            }

            return sha256Builder.buildUnixHash();
        } catch (NoSuchAlgorithmException | IOException e) {
            var errorMessage = String.format("Error calculating sha256 for file [%s]", path);
            throw new RuntimeException(errorMessage, e);
        }
    }
}

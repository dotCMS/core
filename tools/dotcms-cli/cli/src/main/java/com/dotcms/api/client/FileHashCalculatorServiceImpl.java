package com.dotcms.api.client;

import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.security.Encryptor;
import com.dotcms.security.HashBuilder;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * This is a service class for calculating file hashes. It is responsible for providing a method to
 * calculate the SHA-256 hash of a given file. This class is marked as
 * {@link javax.enterprise.context.ApplicationScoped}, meaning that a single instance is shared
 * across the entire application.
 */
@ApplicationScoped
public class FileHashCalculatorServiceImpl implements FileHashCalculatorService {

    @Inject
    Logger logger;

    /**
     * Calculates the SHA-256 hash of the content of the supplied file represented by the provided
     * {@link Path}. The function will throw a {@link TraversalTaskException} if the SHA-256
     * algorithm is not found or there is an error reading the file.
     *
     * @param path the path to the file whose content hash needs to be calculated
     * @return the SHA-256 hash as a UNIX-formatted string
     * @throws TraversalTaskException if there is an error calculating the hash
     */
    public String sha256toUnixHash(final Path path) {

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
            logger.error(errorMessage, e);
            throw new TraversalTaskException(errorMessage, e);
        }
    }

}

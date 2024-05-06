package com.dotcms.api.client;

import java.nio.file.Path;

/**
 * This is a service interface for calculating file hashes. It is responsible for providing a method
 * to calculate the SHA-256 hash of a given file.
 */
public interface FileHashCalculatorService {

    /**
     * Calculates the SHA-256 hash of the content of the supplied file represented by the provided
     * {@link Path}.
     *
     * @param path the path to the file whose content hash needs to be calculated
     * @return the SHA-256 hash as a UNIX-formatted string
     */
    String sha256toUnixHash(Path path);

}

package com.dotcms.storage.repository;

import java.io.File;

/**
 * Abstract a file repository, it could follow diff implementation strategies
 * @author jsanca
 */
public interface FileRepositoryManager {

    String TEMP_REPO  = "TEMP_REPO";
    String LOCAL_REPO = "LOCAL_REPO";
    /**
     * Returns true if the file exists on the repository
     * @param fileName {@link String}
     * @return boolean
     */
    boolean exists (String fileName);

    /**
     * Get (if exists) or create it (if does not exists) the file from the repo (if possible)
     * @param fileName {@link String}
     * @return File
     */
    File getOrCreateFile (String fileName);
}

package com.dotcms.storage.repository;

import java.io.File;

/**
 * Abstract a file repository, it could follow diff implementation strategies
 */
public interface FileRepositoryManager {

    boolean exists (String fileName);

    File getOrCreateFile (String fileName);
}

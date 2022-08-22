package com.dotcms.storage.repository;

import java.io.File;

public class TempFileRepositoryManager implements FileRepositoryManager {
    @Override
    public boolean exists(String fileName) {
        return false;
    }

    @Override
    public File getOrCreateFile(String fileName) {
        return null;
    }
}

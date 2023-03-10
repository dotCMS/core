package com.dotcms.storage.repository;

import com.dotmarketing.util.FileUtil;
import io.vavr.control.Try;

import java.io.File;

/**
 * Temporal file repo implementation
 * @author jsanca
 */
public class TempFileRepositoryManager implements FileRepositoryManager {

    @Override
    public boolean exists(final String fileName) {
        return false; // always false b/c on temp folder
    }

    @Override
    public File getOrCreateFile(final String fileName) {
        final File file = Try.of(()->FileUtil.createTemporaryFile(fileName, ".tmp", true)).getOrNull();
        return file;
    }
}

package com.dotcms.storage.repository;

import com.google.common.annotations.VisibleForTesting;

import java.io.File;

/**
 * This is pretty much the implementation of the LocalFileRepositoryManager, but is expecting a hash as a filename
 * so will normalize it as an (first 4 letters will be the parent sub folder) and the rest will be the file name.
 * @author jsanca
 */
public class HashedLocalFileRepositoryManager extends  LocalFileRepositoryManager {

    public HashedLocalFileRepositoryManager() {
        super();
    }

    @VisibleForTesting
    protected HashedLocalFileRepositoryManager(final String basePath) {
        super(basePath);
    }

    /**
     * If this receives f813463c714e009df1227f706e290e01, it is converted to
     * /f813/f813463c714e009df1227f706e290e01
     *
     * if the hash length is less than 4 chars will return it as it is.
     *
     * @param hash String a hash for instance f813463c714e009df1227f706e290e01
     * @return returns /f813/f813463c714e009df1227f706e290e01
     */
    protected String normalizeFileInSubDirectoryAndFile (final String hash) {

        if (null != hash && hash.length() > 4) {

            final String directoryName = hash.substring(0, 4);
            return File.separator + directoryName + File.separator + hash;
        }

        return hash;
    }

    @Override
    public boolean exists(final String hash) {
        return super.exists(this.normalizeFileInSubDirectoryAndFile(hash));
    }

    @Override
    public File getOrCreateFile(final String hash) {
        return super.getOrCreateFile(this.normalizeFileInSubDirectoryAndFile(hash));
    }
}

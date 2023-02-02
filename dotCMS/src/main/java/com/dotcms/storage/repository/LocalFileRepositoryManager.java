package com.dotcms.storage.repository;

import com.dotmarketing.util.Config;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;

import java.io.File;

/**
 * Repo implemented in a local file system
 * @author jsanca
 */
public class LocalFileRepositoryManager implements FileRepositoryManager {

    private static final Lazy<String> BASE_PATH = Lazy.of(()->Config.getStringProperty("LOCAL_FILE_REPO_BASE_PATH", FileUtil.getRealPath("/local-repo")));
    private final String basePath;

    public LocalFileRepositoryManager() {
        this(BASE_PATH.get());
    }
    @VisibleForTesting
    protected LocalFileRepositoryManager(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public boolean exists(final String fileName) {
        final File file = new File(fileName);

        if (file.isAbsolute() && file.exists()) {

            return true;
        }

        return new File(this.basePath, fileName).exists();
    }

    @Override
    public File getOrCreateFile(final String fileName) { // todo: we have to introduce an improvement to balance the amount of file in the directory,need to create some directory convention such as assets.
        final File file = new File(fileName);
        if (file.isAbsolute() && file.exists()) {

            return file;
        }

        return new File(this.basePath, fileName);
    }
}

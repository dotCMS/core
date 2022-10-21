package com.dotcms.storage.repository;

import com.dotmarketing.util.Config;
import com.liferay.util.FileUtil;

import java.io.File;

/**
 * Repo implemented in a local file system
 * @author jsanca
 */
public class LocalFileRepositoryManager implements FileRepositoryManager {

    private static final String BASE_PATH = Config.getStringProperty("LOCAL_FILE_REPO_BASE_PATH", FileUtil.getRealPath("/local-repo"));

    @Override
    public boolean exists(final String fileName) {
        final File file = new File(fileName);

        if (file.isAbsolute() && file.exists()) {

            return true;
        }

        return new File(BASE_PATH, fileName).exists();
    }

    @Override
    public File getOrCreateFile(final String fileName) { // todo: we have to introduce an improvement to balance the amount of file in the directory,need to create some directory convention such as assets.
        final File file = new File(fileName);
        if (file.isAbsolute() && file.exists()) {

            return file;
        }

        return new File(BASE_PATH, fileName);
    }
}

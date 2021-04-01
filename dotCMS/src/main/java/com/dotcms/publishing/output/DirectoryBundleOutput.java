package com.dotcms.publishing.output;

import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherConfig;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;

public class DirectoryBundleOutput extends BundleOutput {
    private String lastFilePath;
    private File lastFile;
    private File directoryRootPath;

    public DirectoryBundleOutput(final PublisherConfig publisherConfig) {
        super(publisherConfig);
        directoryRootPath = BundlerUtil.getBundleRoot( publisherConfig );
    }

    @VisibleForTesting
    public DirectoryBundleOutput(final PublisherConfig publisherConfig, final File directoryPath) {
        super(publisherConfig);
        directoryRootPath = directoryPath;
    }

    @Override
    public boolean useHardLink() {
        return true;
    }

    @Override
    public Collection<File> getFiles(final FileFilter fileFilter){
        return FileUtil.listFilesRecursively(directoryRootPath, fileFilter);
    }

    @Override
    public long lastModified(String filePath) {
        return getRealFile(filePath).lastModified();
    }

    @Override
    public void setLastModified(final String filePath, final long timeInMillis) {
        final File fileAbsolute = getRealFile(filePath);
        fileAbsolute.setLastModified(timeInMillis);
    }

    @Override
    public OutputStream addFile(final String filePath) throws IOException {
        final File fileAbsolute = getRealFile(filePath);
        fileAbsolute.getParentFile().mkdirs();

        if (!fileAbsolute.exists()) {
            fileAbsolute.createNewFile();
        }

        return Files.newOutputStream( fileAbsolute.toPath());
    }

    @NotNull
    private File getRealFile(final String path) {
        if (path.equals(lastFilePath)) {
            return this.lastFile;
        }

        final File file = new File(directoryRootPath.getAbsolutePath() + File.separator + path);

        this.lastFilePath = path;
        this.lastFile = file;

        return file;
    }

    @Override
    public File getFile() {
        return directoryRootPath;
    }

    @Override
    public File getFile(final String filePath) {
        return getRealFile(filePath);
    }

    @Override
    public void delete(final String filePath) {
        if(this.exists(filePath)) {
            final File realFile = getRealFile(filePath);
            realFile.delete();
        }
    }

    @Override
    public void close() throws IOException {

    }

    public boolean exists(final String filePath) {
        final File realFile = getRealFile(filePath);
        return realFile.exists();
    }
}

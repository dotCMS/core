package com.dotcms.publishing.output;

import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;

public class DirectoryPublisherOutput extends PublisherOutput{
    private File directoryRootPath;

    public DirectoryPublisherOutput(final PublisherConfig publisherConfig) {
        super(publisherConfig);
        directoryRootPath = BundlerUtil.getBundleRoot( publisherConfig );
    }

    @Override
    protected OutputStream innerAddFile(final File file) throws IOException {
        final File parent = file.getParentFile();

        final File dir = getRealFile(parent);
        dir.mkdirs();

        final File fileAbsolute = getRealFile(file);

        if (!fileAbsolute.exists()) {
            fileAbsolute.createNewFile();
        }

        return Files.newOutputStream( fileAbsolute.toPath());
    }

    @NotNull
    private File getRealFile(File parent) {
        return new File(directoryRootPath.getAbsolutePath() + File.separator + parent.getAbsolutePath());
    }

    @Override
    public File getFile() {
        return directoryRootPath;
    }

    @Override
    public void delete(final File file) {
        if(this.exists(file)) {
            final File realFile = getRealFile(file);
            realFile.delete();
        }
    }

    @Override
    public void close() throws IOException {

    }

    public boolean exists(final File searchedFile) {
        final File realFile = getRealFile(searchedFile);
        return realFile.exists();
    }
}

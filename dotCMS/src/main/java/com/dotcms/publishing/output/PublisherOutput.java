package com.dotcms.publishing.output;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public abstract class PublisherOutput implements Closeable {
    private List<File> files;
    protected PublisherConfig publisherConfig;

    public PublisherOutput(final PublisherConfig publisherConfig){
        this.publisherConfig = publisherConfig;
        files = new ArrayList<>();
    }

    protected abstract OutputStream innerAddFile(File file) throws IOException;

    public abstract File getFile();

    public final OutputStream addFile(final File file) throws IOException {
        DotPreconditions.checkArgument(file != null);
        Logger.info(this, String.format("Add File path %s", file != null ? file.getPath() : "file is null"));
        files.add(file);
        return innerAddFile(file);
    }


    public final Collection<File> getFiles(final FileFilter fileFilter) {
        final Stream<File> fileStream = files
                .stream()
                .filter(file -> fileFilter.accept(file));

        return ImmutableList.copyOf(fileStream.iterator());
    }

    public boolean exists(final File searchedFile) {
        return files
                .stream()
                .map(file -> {
                    Logger.info(this, String.format("File path %s", file != null ? file.getPath() : "file is null"));
                    return file.getPath();
                })
                .anyMatch(path -> {
                    Logger.info(this, String.format("searchedFile path %s", searchedFile != null ? searchedFile.getPath() : "searchedFile is null"));
                    return searchedFile.getPath().equals(path);
                });
    }

    public abstract void delete(final File f);
}

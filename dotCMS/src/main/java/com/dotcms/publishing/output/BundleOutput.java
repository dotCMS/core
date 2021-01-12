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

public abstract class BundleOutput implements Closeable {
    protected PublisherConfig publisherConfig;

    public BundleOutput(final PublisherConfig publisherConfig){
        this.publisherConfig = publisherConfig;
    }

    public abstract OutputStream addFile(String filePath) throws IOException;

    public abstract File getFile();

    public abstract File getFile(String filePath);

    public boolean exists(final String filePath) {
        return false;
    }

    public abstract void delete(final String filePath);

    public abstract  Collection<File> getFiles(final FileFilter fileFilter);

    public abstract long lastModified(String filePath);

    public abstract void setLastModified(String myFile, long timeInMillis);
}

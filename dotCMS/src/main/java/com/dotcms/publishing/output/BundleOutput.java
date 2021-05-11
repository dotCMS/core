package com.dotcms.publishing.output;

import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.util.Config;

import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

import java.io.*;
import java.util.Collection;

/**
 * Output for a bundle generate by {@link com.dotcms.publishing.PublisherAPI#publish(PublisherConfig, BundleOutput)}
 * method
 */
public abstract class BundleOutput implements Closeable {
    protected PublisherConfig publisherConfig;

    public BundleOutput(final PublisherConfig publisherConfig){
        this.publisherConfig = publisherConfig;
    }

    /**
     * Add a new file into the output
     *
     * @param filePath Path to add the file
     * @return
     * @throws IOException
     */
    public abstract OutputStream addFile(String filePath) throws IOException;

    /**
     * Add a new file into the output
     *
     * @param file
     * @return
     * @throws IOException
     */
    public OutputStream addFile(File file) throws IOException {
        return addFile(file.getPath());
    }

    /**
     * Copy a file from the File System to this BundleOuput
     *
     * @param source source file in the File System
     * @param destinationPath destination path to copy into the output
     * @throws IOException
     */
    public final void copyFile(File source, String destinationPath) throws IOException {
        final boolean userHardLink =
                Config.getBooleanProperty("CONTENT_VERSION_HARD_LINK", true)
                        && this.useHardLinkByDefault();

        if (userHardLink) {
            FileUtil.copyFile(source, getFile(destinationPath), true);
        } else {
            innerCopyFile(source, destinationPath);
        }
    }

    /**
     * Copy method to be override by sub clss if it need it
     *
     * @param source
     * @param destinationPath
     * @throws IOException
     */
    protected void innerCopyFile(final File source, final String destinationPath) throws IOException {
        FileUtil.copyFile(source, getFile(destinationPath), useHardLinkByDefault());
    }

    /**
     * return true if by deafult should use hardlink to this output
     * @return
     */
    public boolean useHardLinkByDefault() {
        return false;
    }

    /**
     * Return the root file for this output
     * @return
     */
    public abstract File getFile();

    /**
     * Return a file from this output
     *
     * @param filePath path to return
     * @return
     */
    public abstract File getFile(String filePath);

    /**
     * Return true if the file already exists
     *
     * @param filePath
     * @return
     */
    public boolean exists(final String filePath) {
        return false;
    }

    /**
     * Delete a file in the output
     * @param filePath
     */
    public abstract void delete(final String filePath);

    /**
     * Return all the file in this {@link BundleOutput} according to fileFilter
     * @param fileFilter
     * @return
     */
    public abstract  Collection<File> getFiles(final FileFilter fileFilter);

    public abstract long lastModified(String filePath);

    public abstract void setLastModified(String myFile, long timeInMillis);

    public abstract void mkdirs(String path);
}

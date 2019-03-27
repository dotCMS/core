package com.dotmarketing.util.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

/**
 * Created by Oscar Arrieta on 1/18/17.
 */
public class FileWrapper extends File {
    private final File wrappee;

    public FileWrapper(File wrappee) {
        super(wrappee.getAbsolutePath());
        this.wrappee = wrappee;
    }

    @Override
    public String getName() {
        return this.wrappee.getName();
    }

    @Override
    public String getParent() {
        return this.wrappee.getParent();
    }

    @Override
    public File getParentFile() {
        return this.wrappee.getParentFile();
    }

    @Override
    public String getPath() {
        return this.wrappee.getPath();
    }

    @Override
    public boolean isAbsolute() {
        return this.wrappee.isAbsolute();
    }

    @Override
    public String getAbsolutePath() {
        return this.wrappee.getAbsolutePath();
    }

    @Override
    public File getAbsoluteFile() {
        return this.wrappee.getAbsoluteFile();
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return this.wrappee.getCanonicalPath();
    }

    @Override
    public File getCanonicalFile() throws IOException {
        return this.wrappee.getCanonicalFile();
    }

    @Override
    public URL toURL() throws MalformedURLException {
        return this.wrappee.toURL();
    }

    @Override
    public URI toURI() {
        return this.wrappee.toURI();
    }

    @Override
    public boolean canRead() {
        return this.wrappee.canRead();
    }

    @Override
    public boolean canWrite() {
        return this.wrappee.canWrite();
    }

    @Override
    public boolean exists() {
        return this.wrappee.exists();
    }

    @Override
    public boolean isDirectory() {
        return this.wrappee.isDirectory();
    }

    @Override
    public boolean isFile() {
        return this.wrappee.isFile();
    }

    @Override
    public boolean isHidden() {
        return this.wrappee.isHidden();
    }

    @Override
    public long lastModified() {
        return this.wrappee.lastModified();
    }

    @Override
    public long length() {
        return this.wrappee.length();
    }

    @Override
    public boolean createNewFile() throws IOException {
        return this.wrappee.createNewFile();
    }

    @Override
    public boolean delete() {
        return this.wrappee.delete();
    }

    @Override
    public void deleteOnExit() {
        this.wrappee.deleteOnExit();
    }

    @Override
    public String[] list() {
        return this.wrappee.list();
    }

    @Override
    public String[] list(FilenameFilter filter) {
        return this.wrappee.list(filter);
    }

    @Override
    public File[] listFiles() {
        return this.wrappee.listFiles();
    }

    @Override
    public File[] listFiles(FilenameFilter filter) {
        return this.wrappee.listFiles(filter);
    }

    @Override
    public File[] listFiles(FileFilter filter) {
        return this.wrappee.listFiles(filter);
    }

    @Override
    public boolean mkdir() {
        return this.wrappee.mkdir();
    }

    @Override
    public boolean mkdirs() {
        return this.wrappee.mkdirs();
    }

    @Override
    public boolean renameTo(File dest) {
        return this.wrappee.renameTo(dest);
    }

    @Override
    public boolean setLastModified(long time) {
        return this.wrappee.setLastModified(time);
    }

    @Override
    public boolean setReadOnly() {
        return this.wrappee.setReadOnly();
    }

    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return this.wrappee.setWritable(writable, ownerOnly);
    }

    @Override
    public boolean setWritable(boolean writable) {
        return this.wrappee.setWritable(writable);
    }

    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return this.wrappee.setReadable(readable, ownerOnly);
    }

    @Override
    public boolean setReadable(boolean readable) {
        return this.wrappee.setReadable(readable);
    }

    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return this.wrappee.setExecutable(executable, ownerOnly);
    }

    @Override
    public boolean setExecutable(boolean executable) {
        return this.wrappee.setExecutable(executable);
    }

    @Override
    public boolean canExecute() {
        return this.wrappee.canExecute();
    }

    @Override
    public long getTotalSpace() {
        return this.wrappee.getTotalSpace();
    }

    @Override
    public long getFreeSpace() {
        return this.wrappee.getFreeSpace();
    }

    @Override
    public long getUsableSpace() {
        return this.wrappee.getUsableSpace();
    }

    @Override
    public int compareTo(File pathname) {
        return this.wrappee.compareTo(pathname);
    }

    @Override
    public boolean equals(Object obj) {
        return this.wrappee.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.wrappee.hashCode();
    }

    @Override
    public String toString() {
        return this.wrappee.toString();
    }

    @Override
    public Path toPath() {
        return this.wrappee.toPath();
    }
}

package com.dotcms.rendering.js.proxy;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyArray;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Encapsulates a {@link File} in a Js context.
 * @author jsanca
 */
public class JsFile  implements Serializable, JsProxyObject<File> {

    private final File file;

    public JsFile(final File file) {
        this.file = file;
    }

    @Override
    public File getWrappedObject() {
        return file;
    }

    @HostAccess.Export
    public String getName() {
        return this.file.getName();
    }

    @HostAccess.Export
    public String getParent() {
        return this.file.getParent();
    }

    @HostAccess.Export
    public Object getParentFile() {
        return JsProxyFactory.createProxy(this.file.getParentFile());
    }

    @HostAccess.Export
    public String getPath() {
        return file.getPath();
    }

    @HostAccess.Export
    public boolean isAbsolute() {
        return file.isAbsolute();
    }

    @HostAccess.Export
    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @HostAccess.Export
    public Object getAbsoluteFile() {
        return  JsProxyFactory.createProxy(file.getAbsoluteFile());
    }

    @HostAccess.Export
    public String getCanonicalPath() throws IOException {
        return file.getCanonicalPath();
    }

    @HostAccess.Export
    public Object getCanonicalFile() throws IOException {
        return JsProxyFactory.createProxy(file.getCanonicalFile());
    }

    @HostAccess.Export
    public Object toURI() {
        return JsProxyFactory.createProxy(file.toURI());
    }

    @HostAccess.Export
    public boolean canRead() {
        return file.canRead();
    }

    @HostAccess.Export
    public boolean canWrite() {
        return file.canWrite();
    }

    @HostAccess.Export
    public boolean exists() {
        return file.exists();
    }

    @HostAccess.Export
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @HostAccess.Export
    public boolean isFile() {
        return file.isFile();
    }

    @HostAccess.Export
    public boolean isHidden() {
        return file.isHidden();
    }

    @HostAccess.Export
    public long lastModified() {
        return file.lastModified();
    }

    @HostAccess.Export
    public long length() {
        return file.length();
    }

    @HostAccess.Export
    public ProxyArray list() {
        return ProxyArray.fromArray(file.list());
    }

    @HostAccess.Export
    public ProxyArray listFiles() {

        final File[] files = file.listFiles();
        if (null != files) {

            return ProxyArray.fromArray(Stream.of(files)
                    .map(JsProxyFactory::createProxy).toArray());
        }

        return null;
    }
}

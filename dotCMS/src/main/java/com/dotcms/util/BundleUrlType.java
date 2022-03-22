package com.dotcms.util;

import com.google.common.collect.AbstractIterator;
import org.osgi.framework.Bundle;
import org.reflections.vfs.Vfs;
import org.reflections.vfs.Vfs.Dir;
import org.reflections.vfs.Vfs.File;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * UrlType for OSGI bundle
 * This is needed to use the scan annotation on osgi.
 * such as:
 * <code>
 *         final Bundle bundle                  = bundleContext.getBundle();
 *         final BundleWiring bundleWiring      = bundle.adapt(BundleWiring.class);
 *         final ClassLoader  classLoader       = bundleWiring.getClassLoader();
 *         final String basePackage             = "com.dotcms....";
 *
 *         AnnotationUtils.addUrlType(new BundleUrlType(bundle));
 *
 *         APILocator.getWorkflowAPI().scanPackageForActionlets(basePackage, classLoader);
 * </code>
 * @author jsanca
 */
public class BundleUrlType implements Vfs.UrlType {

    public static final String BUNDLE_PROTOCOL_1 = "bundleresource";
    public static final String BUNDLE_PROTOCOL_2 = "bundle";

    private final Bundle bundle;

    public BundleUrlType(final Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public boolean matches(final URL url) {
        return BUNDLE_PROTOCOL_1.equals(url.getProtocol()) ||
                BUNDLE_PROTOCOL_2.equals(url.getProtocol()) ;
    }

    @Override
    public Dir createDir(final URL url) {
        return new BundleDir(bundle, url);
    }

    public class BundleDir implements Dir {

        private final String path;
        private final Bundle bundle;

        public BundleDir(final Bundle bundle, final URL url) {
            this(bundle, url.getPath());
        }

        public BundleDir(final Bundle bundle, final String path) {
            this.bundle = bundle;
            this.path   = this.resolvePath(path);
        }

        private String resolvePath (final String path) {

            if (path.startsWith(BUNDLE_PROTOCOL_1 + ":")) {
                return path.substring((BUNDLE_PROTOCOL_1 + ":").length());
            }

            if (path.startsWith(BUNDLE_PROTOCOL_2 + ":")) {
                return path.substring((BUNDLE_PROTOCOL_2 + ":").length());
            }

            return path;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public Iterable<File> getFiles() {
            return () -> new AbstractIterator<File>() {

                final Enumeration<URL> entries = bundle.findEntries(path, "*.class", true);

                protected File computeNext() {
                    return entries.hasMoreElements() ? new BundleFile(BundleDir.this, entries.nextElement()) : endOfData();
                }
            };
        }

        @Override
        public void close() { }
    }

    public class BundleFile implements File {

        private final BundleDir dir;
        private final String name;
        private final URL url;

        public BundleFile(final BundleDir dir, final URL url) {

            final String path = url.getFile();
            this.dir  = dir;
            this.url  = url;
            this.name = path.substring(path.lastIndexOf("/") + 1);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getRelativePath() {
            return url.getFile().substring(dir.getPath().length());
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return url.openStream();
        }
    }
}

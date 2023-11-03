package com.dotcms.rendering.js;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.UtilMethods;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import io.vavr.Tuple2;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.graalvm.polyglot.io.FileSystem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This File system implementation allows to the js context to access file stored into the
 * it supports things such as:
 * /application/js/modules/mymodules.js
 * //demo.dotcms.com/application/js/modules/mymodules.js
 * /javascript/modules/systemmodule.js
 * dotCMS file system.
 * @author jsanca
 */
public class JsFileSystem implements FileSystem {

    private static final String APPLICATION_ROOT_PATH = "/application/";
    private static final String JAVASCRIPT_ROOT_PATH = "/javascript/";

    public JsFileSystem() {
    }

    private Path checkAllowedPath(final Path path) {

        if (null == path) {
            throw new IllegalArgumentException("Path can not be null");
        }

        final String pathString = path.toString();

        if (pathString.startsWith(HostUtil.HOST_INDICATOR)) {
            checkHost(pathString);
        }

        if (pathString.contains(APILocator.getFileAssetAPI().getRelativeAssetsRootPath())) {
            return path;
        }

        if (!pathString.startsWith(JAVASCRIPT_ROOT_PATH) && !pathString.startsWith(APPLICATION_ROOT_PATH)) {
            throw new IllegalArgumentException("Path is only allowed on: (" + JAVASCRIPT_ROOT_PATH + " or " + APPLICATION_ROOT_PATH + ")");
        }

        return path;
    }

    private void checkHost(final String pathString) {

        try {
            final Tuple2<String, Host> pathHostTuple = HostUtil.splitPathHost(pathString, APILocator.systemUser(), APPLICATION_ROOT_PATH);
            if (Objects.isNull(pathHostTuple._2())) {
                throw new IllegalArgumentException("Host not found: " + pathString);
            }

            if (Objects.isNull(pathHostTuple._1())) {
                throw new IllegalArgumentException("Path is only allowed on: (" + JAVASCRIPT_ROOT_PATH + " or " + APPLICATION_ROOT_PATH + ")");
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Path is only allowed on: (" + JAVASCRIPT_ROOT_PATH + " or " + APPLICATION_ROOT_PATH + ")");
        }
    }


    @Override
    public Path parsePath(final String path) {
        return Paths.get(path);
    }

    @Override
    public Path toAbsolutePath(final Path path) {
        if (path.isAbsolute()) {
            return path;
        }
        return path.toAbsolutePath();
    }

    @Override
    public Path toRealPath(final Path path, final LinkOption... linkOptions) throws IOException {

        checkAllowedPath(path);

        if (path.toString().startsWith(APPLICATION_ROOT_PATH)) {

            return applicationFolderToRealPath(path);
        }

        Path root = Paths.get("/").toAbsolutePath().getRoot();  // todo: analyze this
        Path real = root;
        for (int i = 0; i < path.getNameCount(); i++) {
            Path name = path.getName(i);
            if (".".equals(name.toString())) {
                continue;
            } else if ("..".equals(name.toString())) {
                real = real.getParent();
                if (real == null) {
                    real = root;
                }
                continue;
            }
            real = real.resolve(name);
        }
        return real;
    }

    private Path applicationFolderToRealPath (final Path path) {

        final String pathString = path.toString();
        Host site = null;
        String modulePath = pathString;

        try {

            if (pathString.startsWith(HostUtil.HOST_INDICATOR)) {
                final Tuple2<String, Host> pathHostTuple = HostUtil.splitPathHost(pathString, APILocator.systemUser(), APPLICATION_ROOT_PATH);
                site = pathHostTuple._2();
                modulePath = pathHostTuple._1();
            } else { // it is relative try current host or default
                site = HostUtil.findCurrentHost();
                if (null == site) {
                    site = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
                }
            }

            if (!modulePath.startsWith(APPLICATION_ROOT_PATH)) {
                throw new IllegalArgumentException("Path is only allowed on: (" + JAVASCRIPT_ROOT_PATH + " or " + APPLICATION_ROOT_PATH + ")");
            }

            final Identifier identifier = APILocator.getIdentifierAPI().find(site, pathString);
            if (null != identifier && UtilMethods.isSet(identifier.getId())) {

                final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(identifier.getId());
                final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);

                // it could be relative
                return Path.of(fileAsset.getFileAsset().getPath());
            }
        } catch (DotContentletStateException | DotDataException | DotSecurityException e) {

            throw new DoesNotExistException(e.getMessage(), e);
        }

        return path;
    }

    @Override
    public SeekableByteChannel newByteChannel(final Path path,
                                              final Set<? extends OpenOption> options,
                                              final FileAttribute<?>... attrs) throws IOException {

        try {

            final File file = path.toFile();
            if (file.exists() && file.canRead()) {
                final String source = JsEngine.toString(path.toAbsolutePath().toString(), file);

                if (Objects.nonNull(source)) {

                    return new SeekableInMemoryByteChannel(source.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (DotContentletStateException e) {

            throw new DoesNotExistException(e.getMessage(), e);
        }

        throw new NoSuchFileException(path.toString());
    }

    @Override
    public void checkAccess(final Path path, final Set<? extends AccessMode> modes, final LinkOption... linkOptions) throws IOException {

        if (!onlyRead(modes)) {
            throw new IOException("Only read access is allowed");
        }
    }

    private boolean onlyRead(Set<? extends AccessMode> modes) {

        for (final AccessMode mode : modes) {
            if (AccessMode.READ != mode) {
                return false;
            }
        }
        return true;
    }


    @Override
    public Path parsePath(final URI uri) {
        return Paths.get(uri);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException("New directory stream is not supported by the JsFileSystem");
    }

    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options) throws IOException {
        throw new UnsupportedOperationException(
                "Read attributes is not supported by the JsFileSystem"
        );
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException(
                "Create is not supported by the JsFileSystem"
        );
    }

    @Override
    public void delete(final Path path) throws IOException {
        throw new UnsupportedOperationException(
                "Delete is not supported by the JsFileSystem"

        );
    }
}

package com.dotcms.rendering.js;

import com.dotmarketing.util.Config;
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
 *
 * dotCMS file system.
 * @author jsanca
 */
public class JsFileSystem implements FileSystem {

    public JsFileSystem() {
    }

    private Path checkAllowedPath(final Path path) {
        // todo: throw exception if not allowed
        return path;
    }

    @Override
    public Path parsePath(final String path) {
        return checkAllowedPath(Paths.get(path));
    }

    @Override
    public Path toAbsolutePath(final Path path) {
        if (path.isAbsolute()) {
            return path;
        }
        return checkAllowedPath(path.toAbsolutePath());
    }

    @Override
    public Path toRealPath(final Path path, final LinkOption... linkOptions) throws IOException {
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

    @Override
    public SeekableByteChannel newByteChannel(final Path path,
                                              final Set<? extends OpenOption> options,
                                              final FileAttribute<?>... attrs) throws IOException {

        checkAllowedPath(path);
        // it could be relative
        String source = JsEngine.toString(path.toAbsolutePath().toString(), path.toFile());
        if (Objects.isNull(source)) {

            // or absolute
            final String realPath = Config.CONTEXT.getRealPath(path.toAbsolutePath().toString());
            source = JsEngine.toString(realPath, new File(realPath));
        }

        if (Objects.nonNull(source)) {

            return new SeekableInMemoryByteChannel(source.getBytes(StandardCharsets.UTF_8));
        }

        throw new NoSuchFileException(path.toString());
    }

    @Override
    public void checkAccess(final Path path, final Set<? extends AccessMode> modes, final LinkOption... linkOptions) throws IOException {

        checkAllowedPath(path);
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
        return checkAllowedPath(Paths.get(uri));
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

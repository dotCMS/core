package com.dotcms.rendering.js;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.graalvm.polyglot.io.FileSystem;

import javax.annotation.Nullable;
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
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/**
 * File system the JavaScript engine uses to load modules. Scripts can load modules stored as file assets under the
 * {@code /application/} folder of a site, for instance:
 * <ul>
 *     <li>{@code /application/js/modules/mymodule.mjs}, from the current site</li>
 *     <li>{@code //demo.dotcms.com/application/js/modules/mymodule.mjs}, from the named site</li>
 * </ul>
 * Module paths stay logical: the engine sees the path the script asked for, the module source is read through a
 * {@link ModuleSource}, and relative imports resolve against the module's folder. Paths under {@code /javascript/}
 * are accepted and answer "not found". Any other path is refused.
 * @author jsanca
 */
public class JsFileSystem implements FileSystem {

    private static final String APPLICATION_ROOT_PATH = "/application/";
    private static final String JAVASCRIPT_ROOT_PATH = "/javascript/";
    public static final String DOT_SITE_TOKEN = "/dot-site-token/";
    public static final String PATH_IS_ONLY_ALLOWED_ON = "Path is only allowed on: (";
    private static final String MODULE_NOT_READABLE_MESSAGE = "The module could not be read";
    private static final String MODULE_NOT_READABLE_LOG = "A JavaScript module could not be read";
    private static final int MAX_LOGGED_CAUSES = 5;
    private static final char LINE_SEPARATOR = '\u2028';
    private static final char PARAGRAPH_SEPARATOR = '\u2029';

    /**
     * Supplies the source text of an {@code /application/} module.
     */
    @FunctionalInterface
    interface ModuleSource {

        /**
         * Reads the source text of a module.
         *
         * @param siteName   the name of the site the module belongs to, or {@code null} for the current site
         *                   (the default site when there is no current one)
         * @param modulePath the module path inside the site, starting with {@code /application/}
         * @return the module source, or empty when the site or the module does not exist or the content is not a
         *         file asset
         * @throws IOException when the module exists but cannot be read, or a lookup fails
         */
        Optional<String> read(String siteName, String modulePath) throws IOException;
    }

    /**
     * A path a script is allowed to load: the site it names (if any), the module path inside that site, and the
     * normalized logical path the engine keeps as the module's real path.
     */
    private static final class ModulePath {

        private final String siteName;
        private final String path;
        private final Path logicalPath;

        private ModulePath(final String siteName, final String path, final Path logicalPath) {
            this.siteName = siteName;
            this.path = path;
            this.logicalPath = logicalPath;
        }

        private boolean isApplicationModule() {
            return path.startsWith(APPLICATION_ROOT_PATH);
        }
    }

    private final ModuleSource moduleSource;

    /**
     * Creates a file system that reads modules from the content repository.
     */
    public JsFileSystem() {
        this(JsFileSystem::readFromContent);
    }

    /**
     * Creates a file system that reads module sources from the given {@link ModuleSource}.
     *
     * @param moduleSource where the source text of {@code /application/} modules comes from
     */
    @VisibleForTesting
    JsFileSystem(final ModuleSource moduleSource) {
        this.moduleSource = Objects.requireNonNull(moduleSource);
    }

    /**
     * Checks that a script may load the given path. The path is normalized first, then it must be a module under
     * {@code /application/} (on the current site, or on a named site through the {@link #DOT_SITE_TOKEN} prefix, whose
     * site name is not blank) or a path under {@code /javascript/}, with at least one name after the folder, no
     * backslash and no control character.
     *
     * @param path the path the engine wants to use
     * @return the site, module path and logical path of an allowed path
     * @throws IllegalArgumentException when the path is not allowed
     */
    private ModulePath checkAllowedPath(final Path path) {

        if (null == path) {
            throw new IllegalArgumentException("Path can not be null");
        }

        final Path logicalPath = path.normalize();
        final String pathString = logicalPath.toString();

        if (pathString.indexOf('\\') < 0 && !containsControlCharacter(pathString)) {

            if (pathString.startsWith(DOT_SITE_TOKEN)) {

                final String siteAndPath = pathString.substring(DOT_SITE_TOKEN.length());
                final int separator = siteAndPath.indexOf('/');
                if (separator > 0) {

                    final String siteName = siteAndPath.substring(0, separator);
                    final String sitePath = siteAndPath.substring(separator);
                    if (!siteName.isBlank() && isUnder(sitePath, APPLICATION_ROOT_PATH)) {
                        return new ModulePath(siteName, sitePath, logicalPath);
                    }
                }
            } else if (isUnder(pathString, APPLICATION_ROOT_PATH) || isUnder(pathString, JAVASCRIPT_ROOT_PATH)) {
                return new ModulePath(null, pathString, logicalPath);
            }
        }

        Logger.debug(JsFileSystem.class, "A script path was refused: only module folders can be loaded");
        throw new IllegalArgumentException(PATH_IS_ONLY_ALLOWED_ON + JAVASCRIPT_ROOT_PATH + " or " + APPLICATION_ROOT_PATH + ")");
    }

    /**
     * Tells whether a normalized path names something inside the given folder (not the folder itself).
     *
     * @param path   a normalized path
     * @param folder a folder path ending with a slash
     * @return true when the path starts with the folder and has at least one more name
     */
    private static boolean isUnder(final String path, final String folder) {
        return path.startsWith(folder) && path.length() > folder.length();
    }

    /**
     * Tells whether a path contains a control character: any ISO control character ({@code U+0000} to
     * {@code U+001F} and {@code U+007F} to {@code U+009F}, as told by {@link Character#isISOControl(char)}), the line
     * separator {@code U+2028} or the paragraph separator {@code U+2029}.
     *
     * @param path a normalized path
     * @return true when at least one character of the path is a control character
     */
    private static boolean containsControlCharacter(final String path) {

        for (int index = 0; index < path.length(); index++) {
            final char character = path.charAt(index);
            if (Character.isISOControl(character)
                    || character == LINE_SEPARATOR
                    || character == PARAGRAPH_SEPARATOR) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Path parsePath(final String path) {
        if(path.startsWith(HostUtil.HOST_INDICATOR)) {

            return prefixHostScheme(path);
        }

        return Paths.get(path);
    }

    /**
     * Turns a site-qualified path, such as {@code //site/path}, into a path the file system can parse: only the
     * leading {@link HostUtil#HOST_INDICATOR} is replaced with {@link #DOT_SITE_TOKEN}, so later checks know the path
     * names a site. A double slash further along the path is left as is, and the parsed path collapses it into a
     * single separator.
     *
     * @param path a path that starts with {@link HostUtil#HOST_INDICATOR}
     * @return the parsed path, which the path rule then checks like any other
     */
    private Path prefixHostScheme(final String path) {

        final String convertedPath = DOT_SITE_TOKEN + path.substring(HostUtil.HOST_INDICATOR.length());
        return Paths.get(convertedPath);
    }

    @Override
    public Path toAbsolutePath(final Path path) {
        if (path.isAbsolute()) {
            return path;
        }
        return path.toAbsolutePath();
    }

    /**
     * Returns the logical path of an allowed module path. The module keeps the path the script used, so relative
     * imports inside it resolve against its folder.
     *
     * @param path        the path to resolve
     * @param linkOptions ignored, there are no links among modules
     * @return the normalized path
     * @throws IllegalArgumentException when the path is not allowed
     */
    @Override
    public Path toRealPath(final Path path, final LinkOption... linkOptions) throws IOException {

        return checkAllowedPath(path).logicalPath;
    }

    /**
     * Reads the source of an {@code /application/} module through the {@link ModuleSource}.
     *
     * @param path    the module path
     * @param options ignored, modules are only read
     * @param attrs   ignored
     * @return a channel over the module source
     * @throws NoSuchFileException      when the site or the module does not exist, the content is not a file asset,
     *                                  or the path is under {@code /javascript/}
     * @throws IOException              when the module exists but cannot be read, or a lookup fails
     * @throws IllegalArgumentException when the path is not allowed
     */
    @Override
    public SeekableByteChannel newByteChannel(final Path path,
                                              final Set<? extends OpenOption> options,
                                              final FileAttribute<?>... attrs) throws IOException {

        final ModulePath modulePath = checkAllowedPath(path);
        if (modulePath.isApplicationModule()) {

            final Optional<String> source = moduleSource.read(modulePath.siteName, modulePath.path);
            if (source.isPresent()) {

                return new SeekableInMemoryByteChannel(source.get().getBytes(StandardCharsets.UTF_8));
            }
        }

        throw new NoSuchFileException(modulePath.logicalPath.toString());
    }

    /**
     * Allows read access only, and only to paths a script may load.
     *
     * @param path        the path to check
     * @param modes       the requested access modes
     * @param linkOptions ignored
     * @throws IOException              when any mode other than read is requested
     * @throws IllegalArgumentException when the path is not allowed
     */
    @Override
    public void checkAccess(final Path path, final Set<? extends AccessMode> modes, final LinkOption... linkOptions) throws IOException {

        if (!onlyRead(modes)) {
            throw new IOException("Only read access is allowed");
        }

        checkAllowedPath(path);
    }

    private boolean onlyRead(Set<? extends AccessMode> modes) {

        for (final AccessMode mode : modes) {
            if (AccessMode.READ != mode) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads an {@code /application/} module from the content repository: a named site is looked up by its
     * identifier, name or alias, without falling back to the default site, and the system host counts as no site;
     * with no name, the current site is used (the default one when there is no current site). Then the file asset at
     * the module path is read, in any language, on its working version. Lookups run as the system user. The content
     * is cached by the file's location, so a new version of the module is read again. Any failure while looking up or
     * reading the module is reported through {@link #moduleNotReadable(String, String, String, Exception)}.
     *
     * @param siteName   the identifier, name or alias of the site, or {@code null} for the current site
     * @param modulePath the module path inside the site, starting with {@code /application/}
     * @return the module source, or empty when the site or the module does not exist, the site is the system host,
     *         or the content is not a file asset
     * @throws IOException when the module exists but cannot be read, or a lookup fails
     */
    private static Optional<String> readFromContent(final String siteName, final String modulePath) throws IOException {

        try {

            final Optional<Host> site = null != siteName
                    ? APILocator.getHostAPI().findByIdOrKey(siteName, APILocator.systemUser(), false)
                    : Optional.ofNullable(currentOrDefaultSite());
            if (site.isEmpty() || site.get().isSystemHost()) {
                return Optional.empty();
            }

            final Identifier identifier = APILocator.getIdentifierAPI().find(site.get(), modulePath);
            if (null == identifier || !UtilMethods.isSet(identifier.getId())) {
                return Optional.empty();
            }

            final Contentlet contentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(identifier.getId());
            if (null == contentlet || !contentlet.isFileAsset()) {
                return Optional.empty();
            }

            final File binary = APILocator.getFileAssetAPI().fromContentlet(contentlet).getFileAsset();
            final String source = null != binary ? JsEngine.toString(binary.getAbsolutePath(), binary) : null;
            if (null == source) {
                throw moduleNotReadable(siteName, modulePath, "identifier " + identifier.getId(), null);
            }

            return Optional.of(source);
        } catch (final DotDataException | DotSecurityException | RuntimeException e) {

            throw moduleNotReadable(siteName, modulePath, causeClassNames(e), e);
        }
    }

    /**
     * Logs that a module could not be read and builds the exception reported to the engine. The warning names the
     * site and the module path, followed by the given detail. A failure the content repository reports
     * ({@link DotDataException}, {@link DotSecurityException} or {@link DotRuntimeException}) goes to the debug log
     * only, since its message can carry data-layer text; any other failure is logged with the warning, trace
     * included, so a programming error does not read as a failed lookup. The returned exception has a fixed message
     * and no cause, so no internal detail travels with it.
     *
     * @param siteName   the identifier, name or alias of the site, or {@code null} for the current site
     * @param modulePath the module path inside the site
     * @param detail     what the warning adds after the site and the module path: the class names along the
     *                   failure's cause chain, or the identifier of the module whose file could not be read
     * @param failure    the failure that stopped the lookup, or {@code null} when the module's file could not be read
     * @return the exception to throw
     */
    private static IOException moduleNotReadable(@Nullable final String siteName, final String modulePath,
            final String detail, @Nullable final Exception failure) {

        final String warning = MODULE_NOT_READABLE_LOG + " ("
                + (null != siteName ? "site " + siteName : "current site") + ", module " + modulePath + ": " + detail
                + ")";
        if (null == failure || isRepositoryFailure(failure)) {
            Logger.warn(JsFileSystem.class, warning);
            if (null != failure) {
                Logger.debug(JsFileSystem.class, MODULE_NOT_READABLE_LOG, failure);
            }
        } else {
            Logger.warn(JsFileSystem.class, warning, failure);
        }
        return new IOException(MODULE_NOT_READABLE_MESSAGE);
    }

    /**
     * Tells whether a failure is one the content repository reports while looking up or reading content, as opposed
     * to a programming error.
     *
     * @param failure the failure to classify
     * @return {@code true} for a {@link DotDataException}, {@link DotSecurityException} or {@link DotRuntimeException}
     */
    private static boolean isRepositoryFailure(final Exception failure) {

        return failure instanceof DotDataException || failure instanceof DotSecurityException
                || failure instanceof DotRuntimeException;
    }

    /**
     * Lists the simple class names along a failure's cause chain, outermost first, up to
     * {@link #MAX_LOGGED_CAUSES} names, joined with {@code " <- "}. Messages are left out.
     *
     * @param failure the outermost failure
     * @return the class names of the failure and its causes
     */
    private static String causeClassNames(final Throwable failure) {

        final StringJoiner names = new StringJoiner(" <- ");
        Throwable current = failure;
        for (int depth = 0; null != current && depth < MAX_LOGGED_CAUSES; depth++) {
            names.add(current.getClass().getSimpleName());
            current = current.getCause();
        }
        return names.toString();
    }

    /**
     * Returns the site of the current request, or the default site when there is none.
     *
     * @return the current or default {@link Host}
     * @throws DotDataException     when the default site cannot be loaded
     * @throws DotSecurityException when the system user cannot read the default site
     */
    private static Host currentOrDefaultSite() throws DotDataException, DotSecurityException {

        final Host currentSite = HostUtil.findCurrentHost();
        return null != currentSite
                ? currentSite
                : APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
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

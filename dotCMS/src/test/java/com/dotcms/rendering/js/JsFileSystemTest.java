package com.dotcms.rendering.js;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link JsFileSystem}: which paths a script may load, and that module sources are read through the
 * {@link JsFileSystem.ModuleSource} rather than from the file system. The engine-level tests build a GraalJS context
 * with this file system and a stub module source, so no dotCMS runtime is needed. The default module source is tested
 * against static mocks of the content repository APIs.
 */
public class JsFileSystemTest {

    /**
     * Name of the folder dotCMS stores uploaded files in by default.
     */
    private static final String STORAGE_FOLDER = "assets";

    /**
     * Message of the {@link IOException} thrown when the default module source cannot read a module.
     */
    private static final String MODULE_READ_FAILURE = "The module could not be read";

    /**
     * Fixed start of the warning logged when the default module source cannot read a module.
     */
    private static final String MODULE_READ_FAILURE_LOG = "A JavaScript module could not be read";

    /**
     * Logical path of {@code /application/js/a.mjs} on site-b.test, as the file system receives it.
     */
    private static final String SITE_B_MODULE = "/dot-site-token/site-b.test/application/js/a.mjs";

    @TempDir
    Path tempDir;

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<CacheLocator> cacheLocator;
    private FileAssetAPI fileAssetAPI;

    /**
     * Keeps the tests hermetic: any {@link APILocator} call a test does not stub fails, so nothing here reaches the
     * content repository, and the script cache is an empty mock.
     */
    @BeforeEach
    public void isolateFromRuntime() {
        fileAssetAPI = mock(FileAssetAPI.class);
        apiLocator = mockStatic(APILocator.class, invocation -> {
            throw new IllegalStateException("The content repository is not available in this unit test");
        });
        apiLocator.when(APILocator::getFileAssetAPI).thenReturn(fileAssetAPI);
        cacheLocator = mockStatic(CacheLocator.class);
        cacheLocator.when(CacheLocator::getJavascriptCache).thenReturn(mock(JsCache.class));
    }

    /**
     * Releases the static mocks set up by {@link #isolateFromRuntime()}, including when that setup stopped part way.
     */
    @AfterEach
    public void releaseRuntime() {
        if (null != apiLocator) {
            apiLocator.close();
        }
        if (null != cacheLocator) {
            cacheLocator.close();
        }
    }

    @Test
    public void applicationModuleIsReadThroughModuleSource() {
        // Given a module source that holds /application/js/modules/a.mjs for the current site
        final StubModuleSource source = new StubModuleSource()
                .with(null, "/application/js/modules/a.mjs", "export const v = 'A';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When toRealPath resolves /application/js/modules/a.mjs and newByteChannel reads the result
        final Path realPath = assertDoesNotThrow(
                () -> fileSystem.toRealPath(fileSystem.parsePath("/application/js/modules/a.mjs")));
        final String content = assertDoesNotThrow(() -> read(fileSystem, realPath));

        // Then toRealPath returns the same logical path and the channel yields the module source text
        assertEquals("/application/js/modules/a.mjs", realPath.toString());
        assertEquals("export const v = 'A';", content);
    }

    @Test
    public void siteQualifiedModuleIsReadThroughModuleSource() {
        // Given a module source that holds /application/js/b.mjs for site-b.test
        final StubModuleSource source = new StubModuleSource()
                .with("site-b.test", "/application/js/b.mjs", "export const v = 'B';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When //site-b.test/application/js/b.mjs is parsed, resolved and read
        final Path realPath = assertDoesNotThrow(
                () -> fileSystem.toRealPath(fileSystem.parsePath("//site-b.test/application/js/b.mjs")));
        final String content = assertDoesNotThrow(() -> read(fileSystem, realPath));

        // Then the module source is asked for site-b.test and /application/js/b.mjs, and its text is returned
        assertEquals("export const v = 'B';", content);
        assertEquals(List.of(StubModuleSource.key("site-b.test", "/application/js/b.mjs")), source.requests());
    }

    @Test
    public void siteSegmentOnlyAtTheStartIsConverted() {
        // Given a module source that holds /application/js/b.mjs for site-b.test
        final StubModuleSource source = new StubModuleSource()
                .with("site-b.test", "/application/js/b.mjs", "export const v = 'B';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When //site-b.test/application/js//b.mjs, which has a second double slash before the file name, is parsed,
        // resolved and read
        final Path realPath = assertDoesNotThrow(
                () -> fileSystem.toRealPath(fileSystem.parsePath("//site-b.test/application/js//b.mjs")));
        final String content = assertDoesNotThrow(() -> read(fileSystem, realPath));

        // Then only the leading double slash becomes the site token, the second one collapses into a single slash,
        // and the module source is asked for site-b.test and /application/js/b.mjs
        assertEquals("/dot-site-token/site-b.test/application/js/b.mjs", realPath.toString());
        assertEquals("export const v = 'B';", content);
        assertEquals(List.of(StubModuleSource.key("site-b.test", "/application/js/b.mjs")), source.requests());
    }

    @Test
    public void newByteChannelRefusesFileSystemPath() throws IOException {
        // Given an existing temporary file under the file storage folder
        final Path file = writeFile(STORAGE_FOLDER + "/a/b/x.mjs", "export const v = 'FILE';");
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When newByteChannel is called with its absolute path
        // Then an IllegalArgumentException is thrown, the file is not read and the module source is never called
        assertThrows(IllegalArgumentException.class, () -> read(fileSystem, file));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void pathNextToStorageFolderIsRefused() throws IOException {
        // Given a path to a file in a folder next to the file storage folder, whose name starts with that folder's name
        final Path file = writeFile(STORAGE_FOLDER + "_other/x.json", "{}");
        final JsFileSystem fileSystem = new JsFileSystem(new StubModuleSource());

        // When toRealPath is called with it
        // Then an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fileSystem.toRealPath(file));
    }

    @Test
    public void checkAccessAppliesPathRule() throws IOException {
        // Given a path to a file under the file storage folder
        final Path file = writeFile(STORAGE_FOLDER + "/x.mjs", "export const v = 'FILE';");
        final JsFileSystem fileSystem = new JsFileSystem(new StubModuleSource());

        // When checkAccess is called with READ
        // Then an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fileSystem.checkAccess(file, Set.of(AccessMode.READ)));
    }

    @Test
    public void parentSegmentsAreNormalizedBeforeTheCheck() {
        // Given the path /application/../other/x.mjs, not normalized by the caller
        final JsFileSystem fileSystem = new JsFileSystem(new StubModuleSource());

        // When toRealPath is called with it
        // Then an IllegalArgumentException is thrown, because the normalized path is /other/x.mjs
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/application/../other/x.mjs")));
    }

    @Test
    public void segmentsInsideModuleFolderAreNormalized() {
        // Given a module source that holds /application/b.mjs for the current site
        final StubModuleSource source = new StubModuleSource()
                .with(null, "/application/b.mjs", "export const v = 'B';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When toRealPath is called with /application/a/../b.mjs and with /application/./b.mjs, and newByteChannel
        // reads /application/a/../b.mjs
        final Path fromParentSegment = assertDoesNotThrow(
                () -> fileSystem.toRealPath(Paths.get("/application/a/../b.mjs")));
        final Path fromCurrentSegment = assertDoesNotThrow(
                () -> fileSystem.toRealPath(Paths.get("/application/./b.mjs")));
        final String content = assertDoesNotThrow(() -> read(fileSystem, Paths.get("/application/a/../b.mjs")));

        // Then both paths resolve to /application/b.mjs and the channel yields the module source text
        assertEquals("/application/b.mjs", fromParentSegment.toString());
        assertEquals("/application/b.mjs", fromCurrentSegment.toString());
        assertEquals("export const v = 'B';", content);
    }

    @Test
    public void parentSegmentCrossingSiteTokenIsRefused() {
        // Given the path /dot-site-token/site-b.test/../application/x.mjs, whose parent segment removes the site name
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When toRealPath is called with it
        // Then an IllegalArgumentException is thrown, because the normalized path is
        // /dot-site-token/application/x.mjs, whose module path does not start with /application/, and the module
        // source is never called
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/dot-site-token/site-b.test/../application/x.mjs")));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void pathWithBackslashIsRefused() {
        // Given an /application/ path with a backslash in a segment
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When toRealPath is called with it
        // Then an IllegalArgumentException is thrown and the module source is never called
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/application/js\\..\\x.mjs")));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void pathWithControlCharacterIsRefused() {
        // Given an /application/ path with a line break in a segment
        final JsFileSystem fileSystem = new JsFileSystem(new StubModuleSource());

        // When toRealPath is called with it
        // Then an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/application/js/a\nb.mjs")));
    }

    @Test
    public void pathWithUnicodeLineSeparatorIsRefused() {
        // Given /application/ paths with a line separator (U+2028), a next-line character (U+0085) and a paragraph
        // separator (U+2029) in a segment
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When toRealPath is called with each
        // Then an IllegalArgumentException is thrown and the module source is never called
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/application/js/a\u2028b.mjs")));
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/application/js/a\u0085b.mjs")));
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/application/js/a\u2029b.mjs")));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void nullPathIsRefused() {
        // Given a file system with an empty module source
        final JsFileSystem fileSystem = new JsFileSystem(new StubModuleSource());

        // When toRealPath is called without a path
        // Then an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fileSystem.toRealPath(null));
    }

    @Test
    public void refusedPathKeepsTheMessage() {
        // Given an absolute path off the module folders, a relative path and an empty path
        final JsFileSystem fileSystem = new JsFileSystem(new StubModuleSource());
        final String expectedMessage = "Path is only allowed on: (/javascript/ or /application/)";

        // When toRealPath is called with each
        final IllegalArgumentException absoluteFailure = assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/tmp/x.mjs")));
        final IllegalArgumentException relativeFailure = assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("outside/x.mjs")));
        final IllegalArgumentException emptyFailure = assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("")));

        // Then every IllegalArgumentException carries the message that names the allowed folders
        assertEquals(expectedMessage, absoluteFailure.getMessage());
        assertEquals(expectedMessage, relativeFailure.getMessage());
        assertEquals(expectedMessage, emptyFailure.getMessage());
    }

    @Test
    public void siteTokenWithoutModulePathIsRefused() {
        // Given the path /dot-site-token/site-b.test, which names a site but no module
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When toRealPath is called with it
        // Then an IllegalArgumentException is thrown and the module source is never called
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/dot-site-token/site-b.test")));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void blankSiteSegmentIsRefused() {
        // Given a site-qualified /application/ path whose site segment is a single space
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When newByteChannel is called with it
        // Then an IllegalArgumentException is thrown and the module source is never called
        assertThrows(IllegalArgumentException.class,
                () -> read(fileSystem, Paths.get("/dot-site-token/ /application/js/a.mjs")));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void siteNamedNullIsLookedUpByName() {
        // Given a module source that holds /application/js/a.mjs for a site named "null"
        final StubModuleSource source = new StubModuleSource()
                .with("null", "/application/js/a.mjs", "export const v = 'N';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When newByteChannel reads /dot-site-token/null/application/js/a.mjs
        final String content = assertDoesNotThrow(
                () -> read(fileSystem, Paths.get("/dot-site-token/null/application/js/a.mjs")));

        // Then the module source is asked for the site named "null", not for the current site
        assertEquals("export const v = 'N';", content);
        assertEquals(List.of(StubModuleSource.key("null", "/application/js/a.mjs")), source.requests());
    }

    @Test
    public void encodedSegmentIsKeptLiteral() {
        // Given a module source with no module at /application/js/a%20b.mjs
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When newByteChannel is called with that path
        // Then the module source receives the path unchanged and a NoSuchFileException is thrown
        assertThrows(NoSuchFileException.class,
                () -> read(fileSystem, Paths.get("/application/js/a%20b.mjs")));
        assertEquals(List.of(StubModuleSource.key(null, "/application/js/a%20b.mjs")), source.requests());
    }

    @Test
    public void encodedDotSegmentsAreKeptLiteral() {
        // Given a module source with no module at /application/js/%2e%2e/x.mjs
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When newByteChannel is called with that path
        // Then the module source receives the path unchanged, with %2e%2e kept as a folder name, and a
        // NoSuchFileException is thrown
        assertThrows(NoSuchFileException.class,
                () -> read(fileSystem, Paths.get("/application/js/%2e%2e/x.mjs")));
        assertEquals(List.of(StubModuleSource.key(null, "/application/js/%2e%2e/x.mjs")), source.requests());
    }

    @Test
    public void siteQualifiedPathOffApplicationIsRefused() {
        // Given the paths /dot-site-token/site-b.test/other/x.mjs and /dot-site-token/site-b.test/javascript/x.mjs,
        // the second one using the folder that is accepted only without a site
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When toRealPath is called with each
        // Then an IllegalArgumentException is thrown every time and the module source is never called
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/dot-site-token/site-b.test/other/x.mjs")));
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/dot-site-token/site-b.test/javascript/x.mjs")));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void missingSiteOrModuleIsNotFound() {
        // Given a module source that holds nothing
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When newByteChannel reads /dot-site-token/no-such-site.test/application/x.mjs and /application/missing.mjs
        // Then both throw NoSuchFileException, after asking the module source
        assertThrows(NoSuchFileException.class,
                () -> read(fileSystem, Paths.get("/dot-site-token/no-such-site.test/application/x.mjs")));
        assertThrows(NoSuchFileException.class, () -> read(fileSystem, Paths.get("/application/missing.mjs")));
        assertEquals(List.of(StubModuleSource.key("no-such-site.test", "/application/x.mjs"),
                StubModuleSource.key(null, "/application/missing.mjs")), source.requests());
    }

    @Test
    public void bareModuleFolderIsRefused() {
        // Given the module folders alone, with and without a trailing slash: /application/, /application,
        // /javascript/, /javascript and /dot-site-token/site-b.test/application/
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When newByteChannel is called with each
        // Then an IllegalArgumentException is thrown every time and the module source is never called
        assertThrows(IllegalArgumentException.class, () -> read(fileSystem, Paths.get("/application/")));
        assertThrows(IllegalArgumentException.class, () -> read(fileSystem, Paths.get("/application")));
        assertThrows(IllegalArgumentException.class, () -> read(fileSystem, Paths.get("/javascript/")));
        assertThrows(IllegalArgumentException.class, () -> read(fileSystem, Paths.get("/javascript")));
        assertThrows(IllegalArgumentException.class,
                () -> read(fileSystem, Paths.get("/dot-site-token/site-b.test/application/")));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void pathSharingApplicationPrefixIsRefused() {
        // Given the path /applicationx/a.mjs
        final JsFileSystem fileSystem = new JsFileSystem(new StubModuleSource());

        // When toRealPath is called with it
        // Then an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fileSystem.toRealPath(Paths.get("/applicationx/a.mjs")));
    }

    @Test
    public void pathWithModuleFolderInTheMiddleIsRefused() {
        // Given the paths /tmp/application/x.mjs and /x/javascript/a.mjs, which name a module folder after another
        // folder instead of at the start
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When toRealPath is called with each, and newByteChannel is called with the first
        // Then an IllegalArgumentException is thrown every time and the module source is never called
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/tmp/application/x.mjs")));
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.toRealPath(Paths.get("/x/javascript/a.mjs")));
        assertThrows(IllegalArgumentException.class,
                () -> read(fileSystem, Paths.get("/tmp/application/x.mjs")));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void javascriptPrefixIsNotReadFromFileSystem() {
        // Given the path /javascript/modules/x.mjs
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When newByteChannel is called with it
        // Then a NoSuchFileException is thrown and the module source is never called
        assertThrows(NoSuchFileException.class, () -> read(fileSystem, Paths.get("/javascript/modules/x.mjs")));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void fileUriOfFileSystemPathIsRefused() throws IOException {
        // Given an existing temporary file under the file storage folder
        final Path file = writeFile(STORAGE_FOLDER + "/a/x.mjs", "export const v = 'FILE';");
        final JsFileSystem fileSystem = new JsFileSystem(new StubModuleSource());

        // When its file: URI is given to parsePath and toRealPath is called with the parsed path
        final Path fileFromUri = fileSystem.parsePath(file.toUri());

        // Then an IllegalArgumentException is thrown
        assertThrows(IllegalArgumentException.class, () -> fileSystem.toRealPath(fileFromUri));
    }

    @Test
    public void fileUriOfApplicationModuleResolvesToLogicalPath() {
        // Given a module source holding /application/js/modules/a.mjs for the current site
        final StubModuleSource source = new StubModuleSource()
                .with(null, "/application/js/modules/a.mjs", "export const v = 'A';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When file:///application/js/modules/a.mjs is given to parsePath, and the parsed path is resolved and read
        final Path moduleFromUri = fileSystem.parsePath(URI.create("file:///application/js/modules/a.mjs"));
        final Path realPath = assertDoesNotThrow(() -> fileSystem.toRealPath(moduleFromUri));
        final String content = assertDoesNotThrow(() -> read(fileSystem, realPath));

        // Then the module resolves to its logical path and is read through the module source
        assertEquals("/application/js/modules/a.mjs", realPath.toString());
        assertEquals("export const v = 'A';", content);
        assertEquals(List.of(StubModuleSource.key(null, "/application/js/modules/a.mjs")), source.requests());
    }

    @Test
    public void checkAccessOfNonReadModeIsRefused() {
        // Given the allowed path /application/js/modules/a.mjs, which a module source holds for the current site
        final StubModuleSource source = new StubModuleSource()
                .with(null, "/application/js/modules/a.mjs", "export const v = 'A';");
        final JsFileSystem fileSystem = new JsFileSystem(source);
        final Path modulePath = Paths.get("/application/js/modules/a.mjs");

        // When checkAccess is called on it with WRITE, with READ and WRITE together, with EXECUTE, with READ alone and
        // with no mode, and with no mode on /tmp/x.mjs
        // Then every request with a mode other than READ throws an IOException; READ alone and no mode are accepted
        // on the module path, no mode on /tmp/x.mjs is refused by the path rule, and the module source is never
        // called
        assertThrows(IOException.class, () -> fileSystem.checkAccess(modulePath, Set.of(AccessMode.WRITE)));
        assertThrows(IOException.class,
                () -> fileSystem.checkAccess(modulePath, Set.of(AccessMode.READ, AccessMode.WRITE)));
        assertThrows(IOException.class, () -> fileSystem.checkAccess(modulePath, Set.of(AccessMode.EXECUTE)));
        assertDoesNotThrow(() -> fileSystem.checkAccess(modulePath, Set.of(AccessMode.READ)));
        assertDoesNotThrow(() -> fileSystem.checkAccess(modulePath, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> fileSystem.checkAccess(Paths.get("/tmp/x.mjs"), Set.of()));
        assertTrue(source.requests().isEmpty());
    }

    @Test
    public void engineImportOfFileSystemPathIsRejected() throws IOException {
        // Given a GraalJS context with this file system and an existing temporary file under the file storage folder
        final Path file = writeFile(STORAGE_FOLDER + "/a/b/x.mjs", "export const v = 'FILE-CONTENT';");
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script imports the file by its absolute path
        final String result = runAsync(fileSystem, "(await import('" + file + "')).v");

        // Then the import is rejected, the file content is not in the result and the module source is never called
        assertTrue(result.startsWith("REJECTED"), result);
        assertFalse(result.contains("FILE-CONTENT"), result);
        assertTrue(source.requests().isEmpty(), source.requests().toString());
    }

    @Test
    public void engineImportOfFileUriIsRejected() throws IOException {
        // Given a GraalJS context with this file system and an existing temporary file under the file storage folder
        final Path file = writeFile(STORAGE_FOLDER + "/a/x.mjs", "export const v = 'FILE-CONTENT';");
        final StubModuleSource source = new StubModuleSource();
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script imports the file by its file: URI
        final String result = runAsync(fileSystem, "(await import('" + file.toUri() + "')).v");

        // Then the import is rejected, the file content is not in the result and the module source is never called
        assertTrue(result.startsWith("REJECTED"), result);
        assertFalse(result.contains("FILE-CONTENT"), result);
        assertTrue(source.requests().isEmpty(), source.requests().toString());
    }

    @Test
    public void engineImportOfApplicationModuleWorks() {
        // Given a GraalJS context with this file system and a module source holding /application/js/modules/a.mjs
        final StubModuleSource source = new StubModuleSource()
                .with(null, "/application/js/modules/a.mjs", "export const v = 'A';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script imports /application/js/modules/a.mjs
        final String result = runAsync(fileSystem, "(await import('/application/js/modules/a.mjs')).v");

        // Then the result carries the module's export
        assertEquals("RESOLVED A", result);
    }

    @Test
    public void engineResolvesRelativeImportBetweenApplicationModules() {
        // Given a module source where /application/js/modules/b.mjs imports ./a.mjs
        final StubModuleSource source = new StubModuleSource()
                .with(null, "/application/js/modules/a.mjs", "export const v = 'A';")
                .with(null, "/application/js/modules/b.mjs", "import { v } from './a.mjs'; export const w = 'B' + v;");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script imports /application/js/modules/b.mjs
        final String result = runAsync(fileSystem, "(await import('/application/js/modules/b.mjs')).w");

        // Then ./a.mjs resolves to /application/js/modules/a.mjs through the module source
        assertEquals("RESOLVED BA", result);
        assertTrue(source.requests().contains(StubModuleSource.key(null, "/application/js/modules/a.mjs")),
                source.requests().toString());
    }

    @Test
    public void engineResolvesParentImportInsideApplicationFolder() {
        // Given a module source where /application/js/modules/b.mjs imports ../lib/x.mjs
        final StubModuleSource source = new StubModuleSource()
                .with(null, "/application/js/modules/b.mjs",
                        "import { v } from '../lib/x.mjs'; export const w = 'B' + v;")
                .with(null, "/application/js/lib/x.mjs", "export const v = 'X';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script imports /application/js/modules/b.mjs
        final String result = runAsync(fileSystem, "(await import('/application/js/modules/b.mjs')).w");

        // Then ../lib/x.mjs resolves to /application/js/lib/x.mjs and the result carries both exports
        assertEquals("RESOLVED BX", result);
    }

    @Test
    public void engineRefusesParentImportAboveApplicationFolder() {
        // Given a module source where /application/b.mjs imports ../x.mjs, which resolves to /x.mjs
        final StubModuleSource source = new StubModuleSource()
                .with(null, "/application/b.mjs", "import { v } from '../x.mjs'; export const w = v;");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script imports /application/b.mjs
        final String result = runAsync(fileSystem, "(await import('/application/b.mjs')).w");

        // Then the import is rejected and the module source is never asked for /x.mjs
        assertTrue(result.startsWith("REJECTED"), result);
        assertFalse(source.requests().contains(StubModuleSource.key(null, "/x.mjs")), source.requests().toString());
    }

    @Test
    public void engineResolvesParentImportInsideApplicationFolderOfNamedSite() {
        // Given a module source where /application/js/modules/b.mjs of site-b.test imports ../lib/x.mjs
        final StubModuleSource source = new StubModuleSource()
                .with("site-b.test", "/application/js/modules/b.mjs",
                        "import { v } from '../lib/x.mjs'; export const w = 'B' + v;")
                .with("site-b.test", "/application/js/lib/x.mjs", "export const v = 'X';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script imports //site-b.test/application/js/modules/b.mjs
        final String result = runAsync(fileSystem,
                "(await import('//site-b.test/application/js/modules/b.mjs')).w");

        // Then ../lib/x.mjs resolves to /application/js/lib/x.mjs of site-b.test
        assertEquals("RESOLVED BX", result);
        assertTrue(source.requests().contains(StubModuleSource.key("site-b.test", "/application/js/lib/x.mjs")),
                source.requests().toString());
    }

    @Test
    public void engineRefusesParentImportAboveApplicationFolderOfNamedSite() {
        // Given a module source where /application/js/modules/b.mjs of site-b.test imports ../../../x.mjs, which
        // climbs above /application/ of that site
        final StubModuleSource source = new StubModuleSource()
                .with("site-b.test", "/application/js/modules/b.mjs",
                        "import { v } from '../../../x.mjs'; export const w = v;");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script imports //site-b.test/application/js/modules/b.mjs
        final String result = runAsync(fileSystem,
                "(await import('//site-b.test/application/js/modules/b.mjs')).w");

        // Then the import is rejected and the module source is asked only for the importing module
        assertTrue(result.startsWith("REJECTED"), result);
        assertEquals(List.of(StubModuleSource.key("site-b.test", "/application/js/modules/b.mjs")),
                source.requests());
    }

    @Test
    public void engineImportsSiteQualifiedModule() {
        // Given a module source where /application/js/b.mjs of site-b.test imports ./c.mjs
        final StubModuleSource source = new StubModuleSource()
                .with("site-b.test", "/application/js/b.mjs", "import { v } from './c.mjs'; export const w = 'B' + v;")
                .with("site-b.test", "/application/js/c.mjs", "export const v = 'C';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script imports //site-b.test/application/js/b.mjs
        final String result = runAsync(fileSystem, "(await import('//site-b.test/application/js/b.mjs')).w");

        // Then ./c.mjs resolves to /application/js/c.mjs of site-b.test through the module source
        assertEquals("RESOLVED BC", result);
        assertTrue(source.requests().contains(StubModuleSource.key("site-b.test", "/application/js/c.mjs")),
                source.requests().toString());
    }

    @Test
    public void engineLoadIsNotSupported() throws IOException {
        // Given a GraalJS context with this file system
        final Path file = writeFile(STORAGE_FOLDER + "/x.js", "var loaded = 'FILE-CONTENT';");
        final StubModuleSource source = new StubModuleSource()
                .with(null, "/application/js/a.js", "var loaded = 'MODULE-CONTENT';");
        final JsFileSystem fileSystem = new JsFileSystem(source);

        // When a script calls load() on an /application/ path and on an absolute temporary file path
        final String fromApplication = runSync(fileSystem, "load('/application/js/a.js'); loaded");
        final String fromFile = runSync(fileSystem, "load('" + file + "'); loaded");

        // Then both calls fail because load() is not supported, and no content is returned
        assertTrue(fromApplication.startsWith("FAILED"), fromApplication);
        assertTrue(fromApplication.contains("not supported"), fromApplication);
        assertTrue(fromFile.startsWith("FAILED"), fromFile);
        assertTrue(fromFile.contains("not supported"), fromFile);
        assertFalse(fromFile.contains("FILE-CONTENT"), fromFile);
    }

    @Test
    public void defaultSourceReportsLookupFailureWithFixedMessage() throws Exception {
        // Given the default module source, with a content repository that fails every lookup
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads a module of the current site and a module of site-b.test
        final IOException currentSiteFailure = assertThrows(IOException.class,
                () -> read(fileSystem, Paths.get("/application/js/a.mjs")));
        final IOException namedSiteFailure = assertThrows(IOException.class,
                () -> read(fileSystem, Paths.get(SITE_B_MODULE)));

        // Then both throw an IOException with the fixed message and no cause
        assertEquals(MODULE_READ_FAILURE, currentSiteFailure.getMessage());
        assertEquals(MODULE_READ_FAILURE, namedSiteFailure.getMessage());
        assertNull(currentSiteFailure.getCause());
        assertNull(namedSiteFailure.getCause());

        // Given a site lookup that fails with an error whose message names the requested site
        final User systemUser = mock(User.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.findByIdOrKey("site-b.test", systemUser, false))
                .thenThrow(new DotRuntimeException("The site site-b.test could not be loaded"));
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);

        // When newByteChannel reads the module of site-b.test
        final IOException siteLookupFailure = assertThrows(IOException.class,
                () -> read(fileSystem, Paths.get(SITE_B_MODULE)));

        // Then the IOException has the fixed message, does not name the site and has no cause
        assertEquals(MODULE_READ_FAILURE, siteLookupFailure.getMessage());
        assertFalse(siteLookupFailure.getMessage().contains("site-b.test"));
        assertNull(siteLookupFailure.getCause());
    }

    @Test
    public void defaultSourceWarnsWithSiteAndModuleButNotTheFailureMessage() throws Exception {
        // Given the default module source, with a site lookup that fails with an error whose message is detailed
        final DotRuntimeException lookupFailure = new DotRuntimeException("The site site-b.test could not be loaded");
        final User systemUser = mock(User.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.findByIdOrKey("site-b.test", systemUser, false)).thenThrow(lookupFailure);
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        final JsFileSystem fileSystem = new JsFileSystem();

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {

            // When newByteChannel reads the module of site-b.test
            assertThrows(IOException.class, () -> read(fileSystem, Paths.get(SITE_B_MODULE)));

            // Then the warning names the site, the module path and the failure's class, but not its message
            final ArgumentCaptor<String> warning = ArgumentCaptor.forClass(String.class);
            logger.verify(() -> Logger.warn(eq(JsFileSystem.class), warning.capture()));
            assertTrue(warning.getValue().startsWith(MODULE_READ_FAILURE_LOG), warning.getValue());
            assertTrue(warning.getValue().contains("site-b.test"), warning.getValue());
            assertTrue(warning.getValue().contains("/application/js/a.mjs"), warning.getValue());
            assertTrue(warning.getValue().contains("DotRuntimeException"), warning.getValue());
            assertFalse(warning.getValue().contains("could not be loaded"), warning.getValue());

            // And the failure itself goes to the debug log only
            logger.verify(() -> Logger.debug(eq(JsFileSystem.class), eq(MODULE_READ_FAILURE_LOG), same(lookupFailure)));
            logger.verify(() -> Logger.warn(eq(JsFileSystem.class), anyString(), any(Throwable.class)), never());
        }
    }

    @Test
    public void defaultSourceWarnsWithTheTraceOfAnUnexpectedFailure() throws Exception {
        // Given the default module source, with a site lookup that fails with an unexpected error
        final NullPointerException unexpected = new NullPointerException("unexpected");
        final User systemUser = mock(User.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.findByIdOrKey("site-b.test", systemUser, false)).thenThrow(unexpected);
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        final JsFileSystem fileSystem = new JsFileSystem();

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {

            // When newByteChannel reads the module of site-b.test
            final IOException failure = assertThrows(IOException.class,
                    () -> read(fileSystem, Paths.get(SITE_B_MODULE)));

            // Then the warning carries the unexpected error with its trace, and the script still gets the fixed
            // message with no cause
            final ArgumentCaptor<String> warning = ArgumentCaptor.forClass(String.class);
            logger.verify(() -> Logger.warn(eq(JsFileSystem.class), warning.capture(), same(unexpected)));
            assertTrue(warning.getValue().contains("/application/js/a.mjs"), warning.getValue());
            assertEquals(MODULE_READ_FAILURE, failure.getMessage());
            assertNull(failure.getCause());
        }
    }

    @Test
    public void defaultSourceWarningNamesTheCurrentSite() throws Exception {
        // Given the default module source, a current site, and a file asset at /application/js/a.mjs on it with no
        // binary
        final Contentlet contentlet = fileAssetWithBinary(null);
        final Host currentSite = mock(Host.class);
        final Identifier identifier = mock(Identifier.class);
        when(identifier.getId()).thenReturn("current-module-id");
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        when(identifierAPI.find(currentSite, "/application/js/a.mjs")).thenReturn(identifier);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI.findContentletByIdentifierAnyLanguage("current-module-id")).thenReturn(contentlet);
        apiLocator.when(APILocator::getIdentifierAPI).thenReturn(identifierAPI);
        apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
        final JsFileSystem fileSystem = new JsFileSystem();

        try (MockedStatic<HostUtil> hostUtil = mockStatic(HostUtil.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            hostUtil.when(HostUtil::findCurrentHost).thenReturn(currentSite);

            // When newByteChannel reads /application/js/a.mjs, with no site in the path
            assertThrows(IOException.class, () -> read(fileSystem, Paths.get("/application/js/a.mjs")));

            // Then the warning names the current site, the module path and the module's identifier
            final ArgumentCaptor<String> warning = ArgumentCaptor.forClass(String.class);
            logger.verify(() -> Logger.warn(eq(JsFileSystem.class), warning.capture()));
            assertTrue(warning.getValue().contains("current site"), warning.getValue());
            assertTrue(warning.getValue().contains("/application/js/a.mjs"), warning.getValue());
            assertTrue(warning.getValue().contains("current-module-id"), warning.getValue());
        }
    }

    @Test
    public void defaultSourceTreatsSystemHostAsNotFound() throws Exception {
        // Given the default module source, where the key SYSTEM_HOST resolves to the system host
        final User systemUser = mock(User.class);
        final Host systemHost = mock(Host.class);
        when(systemHost.isSystemHost()).thenReturn(true);
        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.findByIdOrKey("SYSTEM_HOST", systemUser, false)).thenReturn(Optional.of(systemHost));
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        apiLocator.when(APILocator::getIdentifierAPI).thenReturn(identifierAPI);
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads /application/js/a.mjs of SYSTEM_HOST
        // Then a NoSuchFileException is thrown, as for an unknown site, and no module is looked up on it
        assertThrows(NoSuchFileException.class,
                () -> read(fileSystem, Paths.get("/dot-site-token/SYSTEM_HOST/application/js/a.mjs")));
        verify(identifierAPI, never()).find(any(Host.class), any());
    }

    @Test
    public void defaultSourceDoesNotUseAnotherSiteWhenNameIsUnknown() throws Exception {
        // Given the default module source, and a content repository with no site named site-b.test
        final User systemUser = mock(User.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.findByIdOrKey("site-b.test", systemUser, false)).thenReturn(Optional.empty());
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads /application/js/a.mjs of site-b.test
        // Then a NoSuchFileException is thrown, and neither a lookup by name nor the default site is used
        assertThrows(NoSuchFileException.class, () -> read(fileSystem, Paths.get(SITE_B_MODULE)));
        verify(hostAPI, never()).findByName(any(), any(), anyBoolean());
        verify(hostAPI, never()).findDefaultHost(any(), anyBoolean());
    }

    @Test
    public void defaultSourceLooksUpSiteNamedNullByName() throws Exception {
        // Given the default module source, and a content repository with no site named null
        final User systemUser = mock(User.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.findByIdOrKey("null", systemUser, false)).thenReturn(Optional.empty());
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads /application/js/a.mjs of the site named null
        // Then the site is looked up by that name, a NoSuchFileException is thrown, and the default site is not used
        assertThrows(NoSuchFileException.class,
                () -> read(fileSystem, Paths.get("/dot-site-token/null/application/js/a.mjs")));
        verify(hostAPI).findByIdOrKey("null", systemUser, false);
        verify(hostAPI, never()).findDefaultHost(any(), anyBoolean());
    }

    @Test
    public void defaultSourceTreatsNonFileAssetAsNotFound() throws Exception {
        // Given the default module source, where /application/js/a.mjs of site-b.test is content that is not a file
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.isFileAsset()).thenReturn(false);
        stubSiteBModule(contentlet);
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads it
        // Then a NoSuchFileException is thrown and no file is looked up for that content
        assertThrows(NoSuchFileException.class, () -> read(fileSystem, Paths.get(SITE_B_MODULE)));
        verify(fileAssetAPI, never()).fromContentlet(any());
    }

    @Test
    public void defaultSourceReadsModuleOfCurrentSite() throws Exception {
        // Given the default module source, a current site, and a file asset at /application/js/a.mjs on it
        final Path binary = writeFile("current/a.mjs", "export const v = 'CURRENT';");
        final Contentlet contentlet = fileAssetWithBinary(binary.toFile());
        final Host currentSite = mock(Host.class);
        final Identifier identifier = mock(Identifier.class);
        when(identifier.getId()).thenReturn("current-module-id");
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        when(identifierAPI.find(currentSite, "/application/js/a.mjs")).thenReturn(identifier);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI.findContentletByIdentifierAnyLanguage("current-module-id")).thenReturn(contentlet);
        apiLocator.when(APILocator::getIdentifierAPI).thenReturn(identifierAPI);
        apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
        final JsFileSystem fileSystem = new JsFileSystem();

        try (MockedStatic<HostUtil> hostUtil = mockStatic(HostUtil.class)) {
            hostUtil.when(HostUtil::findCurrentHost).thenReturn(currentSite);

            // When newByteChannel reads /application/js/a.mjs, with no site in the path
            final String content = read(fileSystem, Paths.get("/application/js/a.mjs"));

            // Then the module of the current site is returned, without looking up any site by name
            assertEquals("export const v = 'CURRENT';", content);
        }
    }

    @Test
    public void defaultSourceTreatsMissingIdentifierAsNotFound() throws Exception {
        // Given the default module source, and site-b.test with nothing at /application/js/a.mjs
        final User systemUser = mock(User.class);
        final Host site = mock(Host.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.findByIdOrKey("site-b.test", systemUser, false)).thenReturn(Optional.of(site));
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        when(identifierAPI.find(site, "/application/js/a.mjs")).thenReturn(mock(Identifier.class));
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        apiLocator.when(APILocator::getIdentifierAPI).thenReturn(identifierAPI);
        apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads it
        // Then a NoSuchFileException is thrown and no content is looked up
        assertThrows(NoSuchFileException.class, () -> read(fileSystem, Paths.get(SITE_B_MODULE)));
        verify(contentletAPI, never()).findContentletByIdentifierAnyLanguage(any());
    }

    @Test
    public void defaultSourceTreatsMissingContentAsNotFound() throws Exception {
        // Given the default module source, where the identifier of /application/js/a.mjs on site-b.test has no content
        stubSiteBModule(null);
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads it
        // Then a NoSuchFileException is thrown and no file is looked up
        assertThrows(NoSuchFileException.class, () -> read(fileSystem, Paths.get(SITE_B_MODULE)));
        verify(fileAssetAPI, never()).fromContentlet(any());
    }

    @Test
    public void defaultSourceReportsUnreadableBinaryWithFixedMessage() throws Exception {
        // Given the default module source, where /application/js/a.mjs of site-b.test is a file whose binary is missing
        stubSiteBModule(fileAssetWithBinary(tempDir.resolve("missing.mjs").toFile()));
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads it
        final IOException failure = assertThrows(IOException.class, () -> read(fileSystem, Paths.get(SITE_B_MODULE)));

        // Then an IOException with the fixed message is thrown
        assertEquals(MODULE_READ_FAILURE, failure.getMessage());
    }

    @Test
    public void defaultSourceReportsMissingBinaryWithFixedMessage() throws Exception {
        // Given the default module source, where /application/js/a.mjs of site-b.test is a file asset with no binary
        stubSiteBModule(fileAssetWithBinary(null));
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads it
        final IOException failure = assertThrows(IOException.class, () -> read(fileSystem, Paths.get(SITE_B_MODULE)));

        // Then an IOException with the fixed message and no cause is thrown
        assertEquals(MODULE_READ_FAILURE, failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test
    public void defaultSourceReadsFileAssetBinary() throws Exception {
        // Given the default module source, where /application/js/a.mjs of site-b.test is a file with a readable binary
        final Path binary = writeFile("module/d.mjs", "export const v = 'D';");
        stubSiteBModule(fileAssetWithBinary(binary.toFile()));
        final JsFileSystem fileSystem = new JsFileSystem();

        // When newByteChannel reads it
        final String content = assertDoesNotThrow(() -> read(fileSystem, Paths.get(SITE_B_MODULE)));

        // Then the channel yields the text of the binary
        assertEquals("export const v = 'D';", content);
    }

    /**
     * Stubs the content repository so that, looked up as the system user, site-b.test exists and its
     * {@code /application/js/a.mjs} identifier resolves to the given content.
     */
    private void stubSiteBModule(final Contentlet contentlet) throws Exception {
        final User systemUser = mock(User.class);
        final Host site = mock(Host.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        when(hostAPI.findByIdOrKey("site-b.test", systemUser, false)).thenReturn(Optional.of(site));

        final Identifier identifier = mock(Identifier.class);
        when(identifier.getId()).thenReturn("module-id");
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        when(identifierAPI.find(site, "/application/js/a.mjs")).thenReturn(identifier);

        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI.findContentletByIdentifierAnyLanguage("module-id")).thenReturn(contentlet);

        apiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        apiLocator.when(APILocator::getIdentifierAPI).thenReturn(identifierAPI);
        apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
    }

    /**
     * Returns file content whose file asset, as given by the file asset API, has the given binary.
     */
    private Contentlet fileAssetWithBinary(final File binary) {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.isFileAsset()).thenReturn(true);
        final FileAsset fileAsset = mock(FileAsset.class);
        when(fileAsset.getFileAsset()).thenReturn(binary);
        when(fileAssetAPI.fromContentlet(contentlet)).thenReturn(fileAsset);
        return contentlet;
    }

    /**
     * Writes a file under the test's temporary folder, creating its parent folders.
     */
    private Path writeFile(final String relativePath, final String content) throws IOException {
        final Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    /**
     * Opens the path through the file system under test and returns its whole content as text.
     */
    private static String read(final JsFileSystem fileSystem, final Path path) throws IOException {
        try (SeekableByteChannel channel = fileSystem.newByteChannel(path, Collections.emptySet())) {
            final ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
            while (buffer.hasRemaining() && channel.read(buffer) > 0) {
                // keep reading until the buffer is full
            }
            return new String(buffer.array(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Runs an asynchronous expression in a GraalJS context built the way the JavaScript engine builds it, and returns
     * {@code RESOLVED <value>} or {@code REJECTED <error>}.
     */
    private static String runAsync(final JsFileSystem fileSystem, final String expression) {
        try (Context context = newContext(fileSystem)) {
            context.eval(Source.create("js", "(async () => { try { globalThis.result = 'RESOLVED ' + ("
                    + expression + "); } catch (e) { globalThis.result = 'REJECTED ' + e; } })();"));
            return context.eval("js", "String(globalThis.result)").asString();
        }
    }

    /**
     * Runs a script in a GraalJS context built the way the JavaScript engine builds it, and returns
     * {@code DONE <value>} or {@code FAILED <error>}.
     */
    private static String runSync(final JsFileSystem fileSystem, final String script) {
        try (Context context = newContext(fileSystem)) {
            return "DONE " + context.eval("js", script).toString();
        } catch (final RuntimeException e) {
            return "FAILED " + e.getMessage();
        }
    }

    /**
     * Builds a context through {@link JsEngine#buildContext(JsFileSystem, boolean)} with the given file system and
     * the default host access.
     */
    private static Context newContext(final JsFileSystem fileSystem) {
        return JsEngine.buildContext(fileSystem, false);
    }

    /**
     * Module source backed by a map, which records every request it receives.
     */
    private static final class StubModuleSource implements JsFileSystem.ModuleSource {

        private final Map<String, String> modules = new HashMap<>();
        private final List<String> requests = new ArrayList<>();

        /**
         * Adds a module to this source.
         */
        StubModuleSource with(final String siteName, final String modulePath, final String content) {
            modules.put(key(siteName, modulePath), content);
            return this;
        }

        /**
         * Returns every request received so far, in order, as {@link #key(String, String)} values.
         */
        List<String> requests() {
            return requests;
        }

        /**
         * Builds the lookup key of a module: the site name (empty for the current site) and the module path.
         */
        static String key(final String siteName, final String modulePath) {
            return (null == siteName ? "" : siteName) + "|" + modulePath;
        }

        @Override
        public Optional<String> read(final String siteName, final String modulePath) {
            final String key = key(siteName, modulePath);
            requests.add(key);
            return Optional.ofNullable(modules.get(key));
        }
    }
}

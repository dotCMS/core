package com.dotcms.csspreproc;

import com.dotcms.csspreproc.dartsass.DartSassCompiler;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.ema.proxy.MockHttpCaptureResponse;
import com.dotcms.ema.proxy.MockPrintWriter;
import com.dotcms.mock.request.DotCMSMockRequestWithSession;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.WebKeys;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CSSAssetStorageTest {
    @BeforeAll
    static void initialize() throws Exception {
        IntegrationTestInitService.getInstance().init();
        assertEquals(Boolean.getBoolean("s3.cms.enabled"), AssetStorageFeature.isEnabled());
    }

    @Test
    void compiledImportsSurviveEvictionAndTrackWorkingAndLiveChanges(@TempDir Path temporary) throws Exception {
        final Host site = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final var folder = new FolderDataGen().site(site).nextPersisted();
        final var binaries = APILocator.getBinaryAssetStorageAPI();
        try {
            final Path colorsUpload = Files.writeString(temporary.resolve("_Colors.scss"), "$tone: red;\n");
            final var colors = new FileAssetDataGen(folder, colorsUpload.toFile()).nextPersisted();
            final var source = new FileAssetDataGen(folder, Files.writeString(temporary.resolve("Theme.scss"),
                    "@import \"Colors\";\nbody { color: $tone; background: url(\"Photo.SVG\"); }\n").toFile()).nextPersisted();
            final var photo = new FileAssetDataGen(folder, Files.writeString(temporary.resolve("Photo.SVG"),
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" height=\"1\"/>").toFile()).nextPersisted();
            for (var content : List.of(colors, source, photo)) {
                APILocator.getContentletAPI().publish(content, APILocator.systemUser(), false);
            }
            final var asset = APILocator.getFileAssetAPI().fromContentlet(source);
            final var request = request(site, asset.getURI());
            final Path generated = Path.of(ConfigUtils.getDotGeneratedPath(), source.getInode().substring(0, 1),
                    source.getInode().substring(1, 2), source.getInode());

            if (AssetStorageFeature.isEnabled()) {
                final var failing = mock(BinaryAssetStorageAPI.class, org.mockito.AdditionalAnswers.delegatesTo(binaries));
                doThrow(new DotDataException("Injected CSS publication outage")).when(failing).storeGeneratedFile(any());
                try (var locator = mockStatic(APILocator.class, CALLS_REAL_METHODS)) {
                    locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(failing);
                    assertThrows(RuntimeException.class, () -> new DotLibSassCompiler(site, asset.getURI(), false, request).compile());
                    verify(failing).storeGeneratedFile(any());
                }
                assertTrue(cssFiles(generated).isEmpty(), "Failed upload must not leave a reusable local-only CSS result");
            }

            final var first = new DotLibSassCompiler(site, asset.getURI(), false, request);
            first.compile();
            final byte[] red = first.getOutput();
            assertCSS(red, "red");
            assertTrue(new String(red, StandardCharsets.UTF_8).contains("Photo.SVG"));

            if (AssetStorageFeature.isEnabled()) {
                assertEquals(1, cssFiles(generated).size());
                final File css = cssFiles(generated).get(0).toFile();
                assertTrue(binaries.evictLocalFile(css));
                for (var content : List.of(colors, source, photo)) {
                    assertTrue(binaries.evictLocalFile(content.getBinary(FileAssetAPI.BINARY_FIELD)));
                }
                CacheLocator.getCSSCache().clearCache();
                // Native compilation is forbidden here: the real storage chain must restore the completed CSS.
                try (var nativeCompiler = mockConstruction(DartSassCompiler.class)) {
                    final var cold = new DotLibSassCompiler(site, asset.getURI(), false, request);
                    cold.compile();
                    assertArrayEquals(red, cold.getOutput());
                    final var response = new MockHttpCaptureResponse(new MockHttpResponse()) {
                        @Override public PrintWriter getWriter() { return new MockPrintWriter(getOutputStream()); }
                    };
                    new CSSPreProcessServlet().doGet(request, response);
                    assertCSS(response.getBytes(), "red");
                    assertTrue(nativeCompiler.constructed().isEmpty(), "Cold CSS should be served without native recompilation");
                }
                assertArrayEquals(red, Files.readAllBytes(css.toPath()));
            } else {
                assertTrue(cssFiles(generated).isEmpty(), "Disabled mode must not create S3 CSS artifacts");
            }

            final var edit = APILocator.getContentletAPI().checkout(colors.getInode(), APILocator.systemUser(), false);
            final Path replacement = Files.createDirectories(temporary.resolve("edit")).resolve("_Colors.scss");
            edit.setBinary(FileAssetAPI.BINARY_FIELD, Files.writeString(replacement, "$tone: blue;\n").toFile());
            APILocator.getContentletAPI().checkin(edit, APILocator.systemUser(), false);
            assertEquals("$tone: red;\n", Files.readString(colors.getBinary(FileAssetAPI.BINARY_FIELD).toPath()));
            final var working = new DotLibSassCompiler(site, asset.getURI(), false, request);
            working.compile();
            assertCSS(working.getOutput(), "blue");
            final var live = new DotLibSassCompiler(site, asset.getURI(), true, request);
            live.compile();
            assertCSS(live.getOutput(), "red");
            if (AssetStorageFeature.isEnabled()) {
                final var outputs = cssFiles(generated);
                assertEquals(3, outputs.size());
                for (Path output : outputs) {
                    assertTrue(binaries.evictLocalFile(output.toFile()));
                }
                binaries.deleteAllBinaries(source.getInode());
                for (Path output : outputs) {
                    assertNull(binaries.getGeneratedFile(output.toFile()), "Inode deletion must remove cold CSS variants");
                }
            }
        } finally {
            CacheLocator.getCSSCache().clearCache();
            FolderDataGen.remove(folder);
        }
    }

    private static void assertCSS(byte[] data, String color) {
        final String actual = new String(data, StandardCharsets.UTF_8);
        assertTrue(CssComparator.areSemanticallyEqual(actual,
                "body { color: " + color + "; background: url(\"Photo.SVG\"); }"), actual);
    }

    private static List<Path> cssFiles(Path folder) throws Exception {
        if (!Files.isDirectory(folder)) return List.of();
        try (var files = Files.list(folder)) {
            return files.filter(path -> path.getFileName().toString().startsWith("dotGenerated_css_")).toList();
        }
    }

    private static DotCMSMockRequestWithSession request(Host site, String uri) throws Exception {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(site.getIdentifier());
        final var request = new DotCMSMockRequestWithSession(session, false);
        request.setRemoteHost(site.getIdentifier());
        request.setAttribute(com.liferay.portal.util.WebKeys.USER, APILocator.getUserAPI().loadUserById("dotcms.org.1"));
        request.setRequestURI(uri);
        return request;
    }
}

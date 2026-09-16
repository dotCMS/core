package com.dotcms.rendering.velocity.services;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

public class AssetTemplateStorageTest {
    @BeforeAll
    static void initialize() throws Exception {
        IntegrationTestInitService.getInstance().init();
        assertEquals(Boolean.getBoolean("s3.cms.enabled"), AssetStorageFeature.isEnabled());
    }

    @Test
    void markdownReadsColdFileThroughTheStreamAPI(@TempDir Path temporary) throws Throwable {
        final var site = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final var folder = new FolderDataGen().site(site).nextPersisted();
        try {
            final var content = new FileAssetDataGen(folder,
                    Files.writeString(temporary.resolve("MixedCase.md"), "# Cold Markdown\n\nStored in S3.\n").toFile()).nextPersisted();
            APILocator.getContentletAPI().publish(content, APILocator.systemUser(), false);
            final var file = content.getBinary(FileAssetAPI.BINARY_FIELD);
            if (AssetStorageFeature.isEnabled()) {
                assertTrue(APILocator.getBinaryAssetStorageAPI().evictLocalFile(file));
            }
            final var session = org.mockito.Mockito.mock(javax.servlet.http.HttpSession.class);
            final var request = new com.dotcms.mock.request.DotCMSMockRequestWithSession(session, false);
            request.setAttribute(com.liferay.portal.util.WebKeys.USER, APILocator.getUserAPI().loadUserById("dotcms.org.1"));
            final var context = org.mockito.Mockito.mock(org.apache.velocity.tools.view.context.ViewContext.class);
            org.mockito.Mockito.when(context.getRequest()).thenReturn(request);
            final var markdown = new com.dotcms.rendering.velocity.viewtools.MarkdownTool();
            markdown.init(context);
            final String rendered = markdown.parseFile("//" + site.getHostname()
                    + APILocator.getFileAssetAPI().fromContentlet(content).getURI());
            assertTrue(rendered.contains("<h1>Cold Markdown</h1>"));
            assertTrue(rendered.contains("Stored in S3."));
        } finally {
            FolderDataGen.remove(folder);
        }
    }

    @Test
    void physicalTemplatePathsRemainReadableAfterCacheEviction(@TempDir Path temporary) throws Exception {
        final var folder = new FolderDataGen().nextPersisted();
        try {
            final var upload = Files.writeString(temporary.resolve("Theme-MixedCase.CSS"), "body { color: purple; }");
            final var content = new FileAssetDataGen(folder, upload.toFile()).nextPersisted();
            final var file = content.getBinary(FileAssetAPI.BINARY_FIELD);
            final var binaries = APILocator.getBinaryAssetStorageAPI();
            if (AssetStorageFeature.isEnabled()) {
                assertTrue(binaries.evictLocalFile(file));
                IncludeLoader.instance();
                final var unavailable = org.mockito.Mockito.mock(com.dotcms.storage.binary.BinaryAssetStorageAPI.class);
                final var failure = new com.dotmarketing.exception.DotDataException("S3 unavailable");
                org.mockito.Mockito.when(unavailable.openLocalFile(org.mockito.ArgumentMatchers.any())).thenThrow(failure);
                try (var locator = org.mockito.Mockito.mockStatic(APILocator.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
                    locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(unavailable);
                    final var thrown = assertThrows(org.apache.velocity.exception.VelocityException.class,
                            () -> new DotResourceLoader().getResourceStream(file.getAbsolutePath()));
                    assertFalse(thrown instanceof ResourceNotFoundException, "An outage is not a missing template");
                    assertSame(failure, thrown.getCause());
                }
            }
            try (var input = new DotResourceLoader().getResourceStream(file.getAbsolutePath())) {
                assertEquals(Files.readString(upload), new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
            if (AssetStorageFeature.isEnabled()) {
                assertTrue(binaries.evictLocalFile(file));
            }
            try (var input = VTLLoader.instance().streamFile(file.getAbsolutePath())) {
                assertEquals(Files.readString(upload), new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
            if (AssetStorageFeature.isEnabled()) {
                binaries.deleteAllBinaries(content.getInode());
                assertThrows(java.io.IOException.class, () -> IncludeLoader.instance().streamFile(file.getAbsolutePath()));
                assertThrows(java.io.IOException.class, () -> VTLLoader.instance().streamFile(file.getAbsolutePath()));
            }
        } finally {
            FolderDataGen.remove(folder);
        }
    }

    @Test
    void enabledLoadersRejectSiblingDirectoriesBeforeStorageAccess() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(AssetStorageFeature.isEnabled());
        // An allowed directory's name as a substring must not make another directory readable.
        final Path assets = Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath()).toAbsolutePath();
        final Path sibling = assets.resolveSibling(assets.getFileName() + "-outside-" + java.util.UUID.randomUUID());
        final Path source = sibling.resolve("Forbidden.CSS");
        Files.createDirectories(sibling);
        Files.writeString(source, "must not be read");
        final var include = IncludeLoader.instance();
        final var vtl = VTLLoader.instance();
        final var storage = org.mockito.Mockito.mock(com.dotcms.storage.binary.BinaryAssetStorageAPI.class);
        try (var locator = org.mockito.Mockito.mockStatic(APILocator.class)) {
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            assertThrows(ResourceNotFoundException.class, () -> include.streamFile(source.toString()));
            assertThrows(ResourceNotFoundException.class, () -> vtl.streamFile(source.toString()));
            org.mockito.Mockito.verifyNoInteractions(storage);
        } finally {
            Files.delete(source);
            Files.delete(sibling);
        }
    }
}

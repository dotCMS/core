package com.dotcms.rendering.velocity.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks how Velocity file loading behaves with S3 asset storage on: which failures are storage
 * outages, and that a missing file outside the asset root is an ordinary miss.
 */
class StoredVelocityFileLoaderTest {

    @TempDir Path root;
    private String previousFlag;

    @BeforeEach
    void enableFlag() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
    }

    @AfterEach
    void restoreFlag() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    @Test
    void databaseLoaderFailureRemainsACachedMiss() throws Exception {
        final ContainerAPI containers = mock(ContainerAPI.class);
        when(containers.findContainer(anyString(), any(), anyBoolean(), anyBoolean()))
                .thenThrow(new DotDataException("database unavailable"));
        final DotResourceCache cache = mock(DotResourceCache.class);
        try (var locator = mockStatic(APILocator.class); var caches = mockStatic(CacheLocator.class)) {
            locator.when(APILocator::getContainerAPI).thenReturn(containers);
            caches.when(CacheLocator::getVeloctyResourceCache).thenReturn(cache);
            final String path = "/LIVE/abc123_1/uuid.container";

            assertThrows(ResourceNotFoundException.class, () -> new DotResourceLoader().getResourceStream(path));
            verify(cache).addMiss(path);
        }
    }

    @Test
    void storageFailureOfAStoredTemplateIsNotCachedAsMissing() throws Exception {
        final Path dynamic = Files.createDirectories(root.resolve("dynamic"));
        final File template = Files.writeString(dynamic.resolve("Page.vtl"), "#set($x = 1)").toFile();
        final BinaryAssetStorageAPI binaries = mock(BinaryAssetStorageAPI.class);
        when(binaries.openLocalFile(any())).thenThrow(new DotDataException("S3 unavailable"));
        final DotResourceCache cache = mock(DotResourceCache.class);
        // Built before the static stubbing below: Mockito cannot stub a mock inside another stubbing.
        final LanguageAPI languages = languages();
        try (var paths = mockStatic(ConfigUtils.class); var locator = mockStatic(APILocator.class);
             var caches = mockStatic(CacheLocator.class)) {
            paths.when(ConfigUtils::getDynamicContentPath).thenReturn(dynamic.toString());
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
            locator.when(APILocator::getLanguageAPI).thenReturn(languages);
            caches.when(CacheLocator::getVeloctyResourceCache).thenReturn(cache);

            final var thrown = assertThrows(VelocityException.class,
                    () -> new DotResourceLoader().getResourceStream(template.getAbsolutePath()));
            assertFalse(thrown instanceof ResourceNotFoundException, "An outage is not a missing template");
            verify(cache, never()).addMiss(any());
        }
    }

    @Test
    void missingTemplateOutsideTheAssetRootIsAPlainMiss(@TempDir Path elsewhere) throws Exception {
        // The asset root sits under the dynamic content path so the loader's allowed-path check
        // accepts it without the servlet configuration a unit test does not have.
        final Path assets = Files.createDirectories(root.resolve("assets"));
        final BinaryAssetStorageAPI binaries = mock(BinaryAssetStorageAPI.class);
        when(binaries.openLocalFile(any())).thenAnswer(
                call -> new ByteArrayInputStream("restored".getBytes(StandardCharsets.UTF_8)));
        try (var paths = mockStatic(ConfigUtils.class); var locator = mockStatic(APILocator.class);
             var logger = mockStatic(Logger.class)) {
            paths.when(ConfigUtils::getDynamicContentPath).thenReturn(root.toString());
            paths.when(ConfigUtils::getAssetPath).thenReturn(assets.toString());
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
            final VTLLoader loader = VTLLoader.instance();

            final String missing = elsewhere.resolve("Missing.vtl").toString();
            assertThrows(ResourceNotFoundException.class, () -> loader.streamFile(missing));
            logger.verify(() -> Logger.warn(any(Object.class), startsWith("POSSIBLE HACK ATTACK")), never());

            // A missing file under the asset root still reaches the restore path.
            final String evicted = assets.resolve("a/b/abc123/fileAsset/Evicted.vtl").toString();
            try (var input = loader.streamFile(evicted)) {
                assertEquals("restored", new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
            verify(binaries).openLocalFile(new File(evicted));
        }
    }

    private static LanguageAPI languages() {
        final LanguageAPI languages = mock(LanguageAPI.class);
        when(languages.getDefaultLanguage()).thenReturn(new Language(1));
        return languages;
    }
}

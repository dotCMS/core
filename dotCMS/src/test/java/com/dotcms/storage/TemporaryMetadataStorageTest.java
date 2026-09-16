package com.dotcms.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.MetadataCache;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TemporaryMetadataStorageTest {
    @Test
    void portableMetadataPreservesLegacyValuesAndClearedFocalPointsAcrossNodes() throws Exception {
        final String oldFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        final String id = "temp_Mixed-Case";
        final Map<String, Serializable> legacy = Map.of("dot:credit", "Author", "dot:focalPoint", "0.25,0.5");
        final var shared = new AtomicReference<Map<String, Serializable>>();
        final var files = mock(FileStorageAPI.class);
        try (var locator = mockStatic(APILocator.class); var caches = mockStatic(CacheLocator.class);
             var paths = mockStatic(ConfigUtils.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            locator.when(APILocator::getFileStorageAPI).thenReturn(files);
            caches.when(CacheLocator::getMetadataCache).thenReturn(mock(MetadataCache.class));
            paths.when(ConfigUtils::getAssetTempPath).thenReturn("/node-a/assets/tmp_upload");
            when(files.retrieveRawMetaData(any())).thenAnswer(call -> {
                StorageKey key = call.getArgument(0);
                if (key.getStorage() == StorageType.S3) {
                    assertEquals("/tmp_upload/" + id + "/" + id + FileMetadataAPI.META_TMP, key.getPath());
                    return shared.get();
                }
                assertEquals(StorageType.FILE_SYSTEM, key.getStorage());
                assertTrue(key.getPath().startsWith("/node-a/"));
                return legacy;
            });
            when(files.setMetadata(any(), any())).thenAnswer(call -> {
                FetchMetadataParams request = call.getArgument(0);
                assertEquals(StorageType.S3, request.getStorageKey().getStorage());
                assertFalse(request.isCache());
                shared.set(new HashMap<>(call.<Map<String, Serializable>>getArgument(1)));
                return true;
            });
            final var nodeA = new FileMetadataAPIImpl();
            nodeA.putCustomMetadataAttributes(id, Map.of("HeroImage", Map.of("focalPoint", "0.75,0.5")));
            assertEquals("0.25,0.5", legacy.get("dot:focalPoint"), "Legacy metadata must remain unchanged");
            paths.when(ConfigUtils::getAssetTempPath).thenReturn("/node-b/different/tmp_upload");
            final var nodeB = new FileMetadataAPIImpl();
            assertEquals(Map.of("credit", "Author", "focalPoint", "0.75,0.5"),
                    nodeB.getMetadata(id).orElseThrow().getCustomMeta());
            nodeB.putCustomMetadataAttributes(id, Map.of("HeroImage", Map.of()));
            assertTrue(nodeA.getMetadata(id).orElseThrow().getCustomMeta().isEmpty(),
                    "An explicit clear must not fall back to the earlier local focal point");
            verify(files, never()).retrieveMetaData(any());

            doThrow(new DotDataException("S3 unavailable")).when(files)
                    .retrieveRawMetaData(argThat(key -> key.getStorage() == StorageType.S3));
            assertThrows(DotDataException.class, () -> nodeA.getMetadata(id));
            assertThrows(DotDataException.class,
                    () -> nodeA.putCustomMetadataAttributes(id, Map.of("HeroImage", Map.of("credit", "Lost"))));
            assertThrows(IllegalArgumentException.class, () -> nodeA.getMetadata("temp_../other"));
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, oldFlag);
        }
    }

    @Test
    void disabledWritesRetainFilesystemPathAndProvider() throws Exception {
        final String oldFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        final var files = mock(FileStorageAPI.class);
        try (var locator = mockStatic(APILocator.class); var caches = mockStatic(CacheLocator.class);
             var paths = mockStatic(ConfigUtils.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, false);
            locator.when(APILocator::getFileStorageAPI).thenReturn(files);
            caches.when(CacheLocator::getMetadataCache).thenReturn(mock(MetadataCache.class));
            paths.when(ConfigUtils::getAssetTempPath).thenReturn("/shared/nfs/tmp_upload");
            new FileMetadataAPIImpl().putCustomMetadataAttributes("temp_abc", Map.of("field", Map.of("credit", "Author")));
            verify(files).putCustomMetadataAttributes(argThat(request ->
                    request.getStorageKey().getStorage() == StorageType.FILE_SYSTEM
                    && request.getStorageKey().getPath().equals("/shared/nfs/tmp_upload/temp_abc/temp_abc.meta.tmp")), any());
            verify(files, never()).retrieveRawMetaData(any());
            verify(files, never()).setMetadata(any(), any());
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, oldFlag);
        }
    }
}

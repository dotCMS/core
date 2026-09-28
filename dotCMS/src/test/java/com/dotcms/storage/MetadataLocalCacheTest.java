package com.dotcms.storage;

import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.MetadataCache;
import com.dotmarketing.util.Config;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetadataLocalCacheTest {
    @TempDir Path root;

    @Test void generationHoldsLeaseAndPropagatesRestoreFailureWhileDisabledModeStaysLocal() throws Exception {
        final String oldFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        final var file = root.resolve("Mixed.Txt").toFile();
        final var assets = mock(BinaryAssetStorageAPI.class);
        final var generator = mock(MetadataGenerator.class);
        final var metadata = mock(FileMetadataAPI.class);
        final var provider = mock(StoragePersistenceProvider.class);
        final var storage = mock(StoragePersistenceAPI.class);
        final var leased = new AtomicBoolean();
        final var streamOpen = new AtomicBoolean();
        final var api = new FileStorageAPIImpl(mock(ObjectReaderDelegate.class), mock(ObjectWriterDelegate.class),
                generator, provider, mock(MetadataCache.class));
        final var config = new GenerateMetadataConfig.Builder()
                .storageKey(new StorageKey.Builder().group("dotmetadata").path("/test").storage(StorageType.FILE_SYSTEM).build())
                .override(true).store(false).cache(false).full(false).build();
        when(provider.getStorage(any())).thenReturn(storage);
        when(assets.acquireCacheLease()).thenAnswer(call -> {
            assertFalse(leased.getAndSet(true));
            return (BinaryAssetStorageAPI.CacheLease) () -> {
                assertFalse(streamOpen.get());
                leased.set(false);
            };
        });
        when(assets.openLocalFile(file)).thenAnswer(call -> {
            assertTrue(leased.get());
            Files.writeString(file.toPath(), "restored bytes");
            streamOpen.set(true);
            return new java.io.ByteArrayInputStream(new byte[0]) {
                @Override public void close() { streamOpen.set(false); }
            };
        });
        when(generator.standAloneMetadata(file)).thenAnswer(call -> {
            assertTrue(file.isFile());
            if (AssetStorageFeature.isEnabled()) {
                assertTrue(leased.get());
                assertTrue(streamOpen.get());
            }
            return new TreeMap<>(Map.of("name", file.getName()));
        });
        when(generator.tikaBasedMetadata(eq(file), anyLong())).thenAnswer(call -> {
            if (AssetStorageFeature.isEnabled()) {
                assertTrue(leased.get());
                assertTrue(streamOpen.get());
            }
            return Map.of("content", "restored bytes");
        });
        try (var locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(assets);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);
            Config.setProperty(AssetStorageFeature.FLAG, true);
            assertEquals("Mixed.Txt", api.generateRawBasicMetaData(file).get("name"));
            assertFalse(leased.get());
            Files.delete(file.toPath());
            assertEquals("restored bytes", api.generateRawFullMetaData(file, 100).get("content"));
            assertFalse(leased.get());
            Files.delete(file.toPath());
            assertEquals("Mixed.Txt", api.generateMetaData(file, config).get("name"));
            assertFalse(leased.get());
            final var failure = new com.dotmarketing.exception.DotRuntimeException("Shared metadata unavailable",
                    new DotDataException("S3 unavailable"));
            when(generator.tikaBasedMetadata(eq(file), anyLong())).thenThrow(failure);
            assertSame(failure, assertThrows(com.dotmarketing.exception.DotRuntimeException.class,
                    () -> api.generateRawFullMetaData(file, 100)));
            assertFalse(leased.get());
            final var fullConfig = new GenerateMetadataConfig.Builder().storageKey(config.getStorageKey())
                    .override(true).store(false).cache(false).full(true).build();
            assertSame(failure, assertThrows(com.dotmarketing.exception.DotRuntimeException.class,
                    () -> api.generateMetaData(file, fullConfig)));
            assertFalse(leased.get());
            doThrow(new DotDataException("S3 unavailable")).when(assets).openLocalFile(file);
            assertThrows(com.dotmarketing.exception.DotRuntimeException.class, () -> api.generateRawBasicMetaData(file));
            assertThrows(com.dotmarketing.exception.DotRuntimeException.class, () -> api.generateRawFullMetaData(file, 100));
            assertThrows(com.dotmarketing.exception.DotRuntimeException.class, () -> api.generateMetaData(file, config));
            assertFalse(leased.get());
            clearInvocations(assets);
            Config.setProperty(AssetStorageFeature.FLAG, false);
            assertTrue(api.generateRawFullMetaData(file, 100).isEmpty(), "Disabled mode retains the existing parser-error fallback");
            assertEquals("Mixed.Txt", api.generateRawBasicMetaData(file).get("name"));
            assertEquals("Mixed.Txt", api.generateMetaData(file, config).get("name"));
            verifyNoInteractions(assets);
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, oldFlag);
        }
    }

}

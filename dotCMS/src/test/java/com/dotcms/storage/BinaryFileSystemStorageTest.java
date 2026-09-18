package com.dotcms.storage;

import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotcms.storage.binary.BinaryAssetStorageAPIImpl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BinaryFileSystemStorageTest {
    @TempDir Path root;

    @Test
    void slowNetworkReadDoesNotBlockOtherBinaryOperations() throws Exception {
        final FileSystemStoragePersistenceAPIImpl storage = mock(FileSystemStoragePersistenceAPIImpl.class);
        final String group = BinaryAssetStorageAPI.BINARY_ASSETS_GROUP;
        when(storage.existsGroup(group)).thenReturn(true);
        final File file = Files.writeString(root.resolve("Report.PDF"), "payload").toFile();
        final CountDownLatch reading = new CountDownLatch(1);
        final CountDownLatch releaseRead = new CountDownLatch(1);
        when(storage.pullFile(group, "a/b/abc123/HeroImage/Slow.PDF")).thenAnswer(call -> {
            reading.countDown();
            assertTrue(releaseRead.await(10, TimeUnit.SECONDS));
            return file;
        });
        when(storage.pullFile(group, "d/e/def456/HeroImage/Fast.PDF")).thenReturn(file);
        final BinaryAssetStorageAPI api = new BinaryAssetStorageAPIImpl(storage);
        api.storeGeneratedFile(file);
        assertSame(file, api.getGeneratedFile(file));
        api.deleteGeneratedFiles("abc123");
        verify(storage, never()).pushFile(eq(BinaryAssetStorageAPI.GENERATED_ASSETS_GROUP), anyString(), any(), any());
        verify(storage, never()).createGroup(eq(BinaryAssetStorageAPI.GENERATED_ASSETS_GROUP), any());
        final var executor = Executors.newFixedThreadPool(2);
        try {
            final var slow = executor.submit(() -> api.getBinaryFile("abc123", "HeroImage", "Slow.PDF"));
            assertTrue(reading.await(5, TimeUnit.SECONDS));
            final var fast = executor.submit(() -> {
                api.storeBinary("def456", "HeroImage", "Fast.PDF", file);
                return api.getBinaryFile("def456", "HeroImage", "Fast.PDF");
            });
            assertSame(file, fast.get(5, TimeUnit.SECONDS), "Unrelated I/O must complete while NFS is slow");
            releaseRead.countDown();
            assertSame(file, slow.get(5, TimeUnit.SECONDS));
        } finally {
            releaseRead.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void mixedCaseNamesSurviveBothCopyModesAndFilesystemFallbackCannotEvict() throws Exception {
        final String previousRoot = Config.getStringProperty("ROOT_GROUP_FOLDER_PATH", null);
        final String previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        Config.setProperty("ROOT_GROUP_FOLDER_PATH", root.toString());
        try (MockedStatic<ConfigUtils> configUtils = mockStatic(ConfigUtils.class)) {
            configUtils.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            final FileSystemStoragePersistenceAPIImpl storage = new FileSystemStoragePersistenceAPIImpl();
            final BinaryAssetStorageAPI api = new BinaryAssetStorageAPIImpl(storage);
            final File source = Files.writeString(root.resolve("Upload.tmp"), "payload").toFile();
            for (final boolean hardLink : List.of(false, true)) {
                api.storeBinary("abc123", "HeroImage", "MyReport.PDF", source, hardLink);
                final Path path = root.resolve("a/b/abc123/HeroImage/MyReport.PDF");
                assertTrue(Files.isSameFile(path, api.getBinaryFile("abc123", "HeroImage", "MyReport.PDF").toPath()));
                assertEquals(List.of("MyReport.PDF"), List.of(path.getParent().toFile().list()));
                assertEquals("payload", Files.readString(path));
                assertFalse(api.evictLocalFile(path.toFile()));
                api.deleteBinary("abc123", "HeroImage");
                assertFalse(Files.exists(path));
                assertFalse(api.existsBinary("abc123", "HeroImage"));
            }
        } finally {
            Config.setProperty("ROOT_GROUP_FOLDER_PATH", previousRoot);
            Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
        }
    }
}

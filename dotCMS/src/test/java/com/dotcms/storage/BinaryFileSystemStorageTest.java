package com.dotcms.storage;

import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotcms.storage.binary.BinaryAssetStorageAPIImpl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
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
            final var fast = executor.submit(() -> api.getBinaryFile("def456", "HeroImage", "Fast.PDF"));
            assertSame(file, fast.get(5, TimeUnit.SECONDS), "Unrelated I/O must complete while NFS is slow");
            releaseRead.countDown();
            assertSame(file, slow.get(5, TimeUnit.SECONDS));
        } finally {
            releaseRead.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void filesystemFallbackCannotEvictMixedCaseBinary() throws Exception {
        final String previousRoot = Config.getStringProperty("ROOT_GROUP_FOLDER_PATH", null);
        final String previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        Config.setProperty("ROOT_GROUP_FOLDER_PATH", root.toString());
        try (MockedStatic<ConfigUtils> configUtils = mockStatic(ConfigUtils.class)) {
            configUtils.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            final FileSystemStoragePersistenceAPIImpl storage = new FileSystemStoragePersistenceAPIImpl();
            final BinaryAssetStorageAPI api = new BinaryAssetStorageAPIImpl(storage);
            final Path path = root.resolve("a/b/abc123/HeroImage/MyReport.PDF");
            Files.createDirectories(path.getParent());
            Files.writeString(path, "payload");
            assertTrue(Files.isSameFile(path, api.getBinaryFile("abc123", "HeroImage", "MyReport.PDF").toPath()));
            assertFalse(api.evictLocalFile(path.toFile()));
            assertEquals("payload", Files.readString(path));
        } finally {
            Config.setProperty("ROOT_GROUP_FOLDER_PATH", previousRoot);
            Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
        }
    }

    /**
     * Only the exact binary group names keep mixed-case keys, matching how the S3 layer decides
     * which groups share binary bytes. A differently cased group name falls back to lowercased keys.
     */
    @Test
    void differentlyCasedBinaryGroupNameIsLowercased() throws Exception {
        final String previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        try {
            final Path groupDir = Files.createDirectories(root.resolve("group"));
            final FileSystemStoragePersistenceAPIImpl storage = new FileSystemStoragePersistenceAPIImpl();
            storage.createGroup(BinaryAssetStorageAPI.BINARY_ASSETS_GROUP,
                    Map.of(FileSystemStoragePersistenceAPIImpl.GROUP_DIRECTORY, groupDir.toFile()));
            final File source = Files.writeString(root.resolve("Upload.tmp"), "payload").toFile();

            storage.pushFile("Binary-Assets", "Mixed/Name.PDF", source, Map.of());

            assertEquals(List.of("mixed"), List.of(groupDir.toFile().list()));
            assertEquals(List.of("name.pdf"), List.of(groupDir.resolve("mixed").toFile().list()));
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
        }
    }

    /**
     * With the flag on, staged writes must get the same permissions as an ordinary file write, so
     * the process umask applies as it does on main. A temp-file staging path would leave them
     * owner-only and lock out group readers such as a sidecar container.
     */
    @Test
    void flagOnWritesHonourUmaskLikeOrdinaryFiles() throws Exception {
        assumeTrue(root.getFileSystem().supportedFileAttributeViews().contains("posix"));
        final String previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        try {
            final Path plain = root.resolve("plain.txt");
            try (OutputStream out = Files.newOutputStream(plain)) {
                out.write(1);
            }
            final Set<PosixFilePermission> expected = Files.getPosixFilePermissions(plain);
            final Path groupDir = Files.createDirectories(root.resolve("group"));
            final FileSystemStoragePersistenceAPIImpl storage = new FileSystemStoragePersistenceAPIImpl();
            storage.createGroup(BinaryAssetStorageAPI.BINARY_ASSETS_GROUP,
                    Map.of(FileSystemStoragePersistenceAPIImpl.GROUP_DIRECTORY, groupDir.toFile()));
            final File source = Files.writeString(root.resolve("Upload.tmp"), "payload").toFile();

            storage.pushFile(BinaryAssetStorageAPI.BINARY_ASSETS_GROUP, "a/Asset.PDF", source, Map.of());
            storage.pushObject(BinaryAssetStorageAPI.BINARY_ASSETS_GROUP, "a/Asset.PDF-metadata.json",
                    (stream, object) -> stream.write(object.toString().getBytes()), "{}", Map.of());

            assertEquals(expected, Files.getPosixFilePermissions(groupDir.resolve("a/Asset.PDF")));
            assertEquals(expected, Files.getPosixFilePermissions(groupDir.resolve("a/Asset.PDF-metadata.json")));
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
        }
    }
}

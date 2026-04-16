package com.dotcms.storage.binary;

import com.dotmarketing.util.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BinaryCacheEvictionJob}.
 * Uses real temp directories with inode-structured files and mocked Config/ConfigUtils.
 */
class BinaryCacheEvictionJobTest {

    @TempDir
    Path assetRoot;

    private BinaryCacheEvictionJob job;

    @BeforeEach
    void setUp() {
        job = new BinaryCacheEvictionJob();
    }

    /**
     * Creates a file in the inode directory structure: {assetRoot}/{c1}/{c2}/{inode}/{field}/{fileName}
     * and sets its lastModified to the given timestamp.
     */
    private Path createInodeFile(String inode, String field, String fileName,
                                  byte[] content, long lastModified) throws Exception {

        final Path fieldDir = assetRoot.resolve(
                inode.charAt(0) + File.separator
                + inode.charAt(1) + File.separator
                + inode + File.separator
                + field);
        Files.createDirectories(fieldDir);
        final Path file = fieldDir.resolve(fileName);
        Files.write(file, content);
        file.toFile().setLastModified(lastModified);
        return file;
    }

    @Test
    void test_eviction_deletes_oldest_files_when_over_threshold() throws Exception {
        final long now = System.currentTimeMillis();
        final long oneHourAgo = now - (2 * 60 * 60 * 1000L); // 2 hours ago (past min age)

        // Create 3 files: oldest (1KB), middle (1KB), newest (1KB) = 3KB total
        final byte[] oneKB = new byte[1024];
        final Path oldest = createInodeFile("abc123", "fileAsset", "old.pdf",
                oneKB, oneHourAgo - 2000);
        final Path middle = createInodeFile("def456", "fileAsset", "mid.pdf",
                oneKB, oneHourAgo - 1000);
        final Path newest = createInodeFile("ghi789", "fileAsset", "new.pdf",
                oneKB, oneHourAgo);

        try (MockedStatic<Config> config = mockStatic(Config.class);
             MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {

            config.when(() -> Config.getStringProperty(
                    BinaryAssetStorageAPIImpl.BINARY_ASSET_STORAGE_TYPE_PROP, "FILE_SYSTEM"))
                    .thenReturn("BINARY_CHAIN");
            // Threshold: 1KB (in MB = tiny, forces eviction of 2 files)
            config.when(() -> Config.getLongProperty(
                    BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_PROP,
                    BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_DEFAULT))
                    .thenReturn(0L); // 0 MB threshold → evict everything eligible
            config.when(() -> Config.getIntProperty(
                    BinaryCacheEvictionJob.BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_PROP,
                    BinaryCacheEvictionJob.BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_DEFAULT))
                    .thenReturn(60); // 1 hour min age
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                    .thenReturn(assetRoot.toString());

            job.execute(null);

            // Oldest and middle should be evicted (both > 1hr old)
            assertFalse(oldest.toFile().exists(), "Oldest file should be evicted");
            assertFalse(middle.toFile().exists(), "Middle file should be evicted");
            assertFalse(newest.toFile().exists(), "Newest file should be evicted (threshold is 0)");
        }
    }

    @Test
    void test_eviction_skips_files_within_min_age() throws Exception {
        final long now = System.currentTimeMillis();
        final long twoHoursAgo = now - (2 * 60 * 60 * 1000L);

        // Create 2 files: old (1KB, 2hrs ago) and recent (1KB, just now)
        final byte[] oneKB = new byte[1024];
        final Path oldFile = createInodeFile("abc123", "fileAsset", "old.pdf",
                oneKB, twoHoursAgo);
        final Path recentFile = createInodeFile("def456", "fileAsset", "recent.pdf",
                oneKB, now);

        try (MockedStatic<Config> config = mockStatic(Config.class);
             MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {

            config.when(() -> Config.getStringProperty(
                    BinaryAssetStorageAPIImpl.BINARY_ASSET_STORAGE_TYPE_PROP, "FILE_SYSTEM"))
                    .thenReturn("BINARY_CHAIN");
            config.when(() -> Config.getLongProperty(
                    BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_PROP,
                    BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_DEFAULT))
                    .thenReturn(0L); // Force eviction
            config.when(() -> Config.getIntProperty(
                    BinaryCacheEvictionJob.BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_PROP,
                    BinaryCacheEvictionJob.BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_DEFAULT))
                    .thenReturn(60); // 1 hour min age
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                    .thenReturn(assetRoot.toString());

            job.execute(null);

            // Old file evicted, recent file protected by min age
            assertFalse(oldFile.toFile().exists(), "Old file should be evicted");
            assertTrue(recentFile.toFile().exists(), "Recent file should survive (within min age)");
        }
    }

    @Test
    void test_no_op_in_filesystem_mode() throws Exception {
        // Create a file that would be evicted if the job ran
        final byte[] oneKB = new byte[1024];
        final Path file = createInodeFile("abc123", "fileAsset", "test.pdf",
                oneKB, System.currentTimeMillis() - (2 * 60 * 60 * 1000L));

        try (MockedStatic<Config> config = mockStatic(Config.class)) {

            config.when(() -> Config.getStringProperty(
                    BinaryAssetStorageAPIImpl.BINARY_ASSET_STORAGE_TYPE_PROP, "FILE_SYSTEM"))
                    .thenReturn("FILE_SYSTEM");

            job.execute(null);

            // File should still exist — job is no-op in FILE_SYSTEM mode
            assertTrue(file.toFile().exists(), "File should not be evicted in FILE_SYSTEM mode");
        }
    }

    @Test
    void test_no_eviction_when_under_threshold() throws Exception {
        final long twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000L);

        // Create 1 file: 1KB
        final byte[] oneKB = new byte[1024];
        final Path file = createInodeFile("abc123", "fileAsset", "test.pdf",
                oneKB, twoHoursAgo);

        try (MockedStatic<Config> config = mockStatic(Config.class);
             MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {

            config.when(() -> Config.getStringProperty(
                    BinaryAssetStorageAPIImpl.BINARY_ASSET_STORAGE_TYPE_PROP, "FILE_SYSTEM"))
                    .thenReturn("BINARY_CHAIN");
            config.when(() -> Config.getLongProperty(
                    BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_PROP,
                    BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_DEFAULT))
                    .thenReturn(5000L); // 5GB threshold — 1KB file is way under
            config.when(() -> Config.getIntProperty(
                    BinaryCacheEvictionJob.BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_PROP,
                    BinaryCacheEvictionJob.BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_DEFAULT))
                    .thenReturn(60);
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                    .thenReturn(assetRoot.toString());

            job.execute(null);

            // File should still exist — under threshold
            assertTrue(file.toFile().exists(), "File should not be evicted when under threshold");
        }
    }

}

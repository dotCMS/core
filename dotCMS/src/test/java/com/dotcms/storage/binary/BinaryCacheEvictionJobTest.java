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
    private String previousFeatureFlag;

    @org.junit.jupiter.api.BeforeEach
    void enableS3FeatureForTest() {
        previousFeatureFlag = Config.getStringProperty(com.dotcms.storage.AssetStorageFeature.FLAG, null);
        Config.setProperty(com.dotcms.storage.AssetStorageFeature.FLAG, true);
    }

    @org.junit.jupiter.api.AfterEach
    void restoreS3FeatureAfterTest() {
        Config.setProperty(com.dotcms.storage.AssetStorageFeature.FLAG, previousFeatureFlag);
    }


    @TempDir
    Path assetRoot;

    private BinaryCacheEvictionJob job;

    @Test
    void scannerIncludesImmutableRevisionsOnlyWhenFeatureIsEnabled() throws Exception {
        Path revision = assetRoot.resolve("a/b/abc123/HeroImage/.revisions/" + java.util.UUID.randomUUID() + "/Friday.PNG");
        Files.createDirectories(revision.getParent());
        Files.writeString(revision, "revision pixels");
        job = new BinaryCacheEvictionJob();
        assertTrue(job.collectBinaryAssetFiles(assetRoot.toFile()).stream().anyMatch(file -> file.path.equals(revision)));
        Config.setProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false);
        assertTrue(job.collectBinaryAssetFiles(assetRoot.toFile()).isEmpty());
    }

    @Test
    void generatedCacheSharesBudgetButTransientFilesAreNeverEvicted() throws Exception {
        final Path generatedRoot = assetRoot.resolve("dotGenerated");
        final Path directory = Files.createDirectories(generatedRoot.resolve("a/b/abc123"));
        final Path complete = Files.writeString(directory.resolve("dotGenerated_resize_abcdef.png"), "complete");
        final Path unbacked = Files.writeString(directory.resolve("dotGenerated_resize_123456.png"), "unbacked");
        final Path transientFile = Files.writeString(directory.resolve("dotGenerated_resize_abcdef.png_123.png"), "partial");
        final BinaryAssetStorageAPI storage = mock(BinaryAssetStorageAPI.class);
        when(storage.evictLocalFile(complete.toFile())).thenAnswer(call -> Files.deleteIfExists(complete));
        job = new BinaryCacheEvictionJob(() -> storage);
        try (MockedStatic<Config> config = mockStatic(Config.class);
             MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
            config.when(() -> Config.getBooleanProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false))
                    .thenReturn(true);
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath).thenReturn(assetRoot.toString());
            configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath).thenReturn(generatedRoot.toString());
            job.execute(null);
            assertFalse(Files.exists(complete));
            assertTrue(Files.exists(unbacked));
            assertTrue(Files.exists(transientFile));
            verify(storage, never()).evictLocalFile(transientFile.toFile());
        }
    }

    @Test
    void filesystemFallbackAndRemoteErrorsKeepLocalFiles() throws Exception {
        final Path file = createInodeFile("abc123", "HeroImage", "Report.PDF",
                new byte[1024], System.currentTimeMillis() - 7_200_000);
        final com.dotcms.storage.StoragePersistenceAPI storage =
                mock(com.dotcms.storage.StoragePersistenceAPI.class);
        final BinaryAssetStorageAPIImpl api = new BinaryAssetStorageAPIImpl(storage);
        job = new BinaryCacheEvictionJob(() -> api);
        try (MockedStatic<Config> config = mockStatic(Config.class);
             MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
            config.when(() -> Config.getBooleanProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false))
                    .thenReturn(true);
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath).thenReturn(assetRoot.toString());
            job.execute(null);
            assertTrue(Files.exists(file), "Configured chain with no durable provider must retain files");
            when(storage.hasDurableCopy(anyString(), anyString(), any(File.class)))
                    .thenThrow(new com.dotmarketing.exception.DotDataException("S3 unavailable"));
            job.execute(null);
            assertTrue(Files.exists(file), "An S3 error must never authorize eviction");
        }
    }

    @Test
    void fileChangedDuringVerificationIsRetained() throws Exception {
        final Path file = createInodeFile("abc123", "HeroImage", "Report.PDF",
                new byte[1024], System.currentTimeMillis() - 7_200_000);
        final com.dotcms.storage.StoragePersistenceAPI storage =
                mock(com.dotcms.storage.StoragePersistenceAPI.class);
        when(storage.hasDurableCopy(anyString(), anyString(), any(File.class))).thenAnswer(call -> {
            Files.writeString(file, "new contents");
            return true;
        });
        try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath).thenReturn(assetRoot.toString());
            assertFalse(new BinaryAssetStorageAPIImpl(storage).evictLocalFile(file.toFile()));
            assertEquals("new contents", Files.readString(file));
        }
    }

    @Test
    void protectedFilesNeverEnterTheBudgetOrReachTheProvider() throws Exception {
        final String revision = java.util.UUID.randomUUID().toString();
        final java.util.List<Path> protectedFiles = new java.util.ArrayList<>();
        for (String relative : java.util.List.of(
                "server/node/heartbeat.dat", "server/node/signing.key",
                "server/publishing-filters/filter.yml", "certs/search.pem", "messages/en.properties",
                "integrity/report.csv", "server/apps/secrets.json", "osgi/config/settings.cfg",
                "a/b/abc123/metaData/content.meta", "a/b/abc123/.hidden/private.bin",
                "a/b/abc123/HeroImage/.restore-123.tmp", "a/b/abc123/HeroImage/.metadata-123.tmp",
                "a/b/abc123/HeroImage/dotGenerated_resize_abcdef.png",
                "a/b/abc123/HeroImage/dotGenerated_resize_abcdef.png_123.png",
                "a/b/abc123/HeroImage/.revisions/invalid/Friday.PNG",
                "a/b/abc123/HeroImage/.revisions/------------------------------------/Friday.PNG",
                "a/b/abc123/HeroImage/not-revisions/" + revision + "/Friday.PNG",
                "a/b/xyz123/HeroImage/Friday.PNG",
                "dotGenerated/a/b/abc123/dotGenerated_resize_abcdef.png_123.png",
                "dotGenerated/a/b/abc123/.restore-123.tmp",
                "dotGenerated/a/b/xyz123/dotGenerated_resize_abcdef.png")) {
            Path file = assetRoot.resolve(relative);
            Files.createDirectories(file.getParent());
            protectedFiles.add(Files.write(file, new byte[128 * 1024]));
        }
        final Path original = createInodeFile("CdE456", "HeroImage", "dotGenerated_UserUpload.PNG", new byte[128], 1);
        final Path version = assetRoot.resolve("C/d/CdE456/HeroImage/.revisions/" + revision + "/dotGenerated_UserUpload.PNG");
        Files.createDirectories(version.getParent());
        Files.writeString(version, "revision");
        final Path rendition = assetRoot.resolve("dotGenerated/C/d/CdE456/dotGenerated_resize_abcdef.PNG");
        Files.createDirectories(rendition.getParent());
        Files.writeString(rendition, "rendition");
        final com.dotcms.storage.StoragePersistenceAPI storage = mock(com.dotcms.storage.StoragePersistenceAPI.class);
        when(storage.hasDurableCopy(anyString(), anyString(), any(File.class))).thenReturn(true);
        final BinaryAssetStorageAPIImpl api = new BinaryAssetStorageAPIImpl(storage);
        job = new BinaryCacheEvictionJob(() -> api);
        try (MockedStatic<Config> config = mockStatic(Config.class);
             MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
            config.when(() -> Config.getBooleanProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false))
                    .thenReturn(true);
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath).thenReturn(assetRoot.toString());
            configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                    .thenReturn(assetRoot.resolve("dotGenerated").toString());
            assertEquals(java.util.Set.of(original, version), job.collectBinaryAssetFiles(assetRoot.toFile())
                    .stream().map(info -> info.path).collect(java.util.stream.Collectors.toSet()));
            for (Path file : protectedFiles) {
                assertFalse(api.evictLocalFile(file.toFile()), file.toString());
            }
            config.when(() -> Config.getLongProperty(BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_PROP,
                    BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_DEFAULT)).thenReturn(1L);
            job.execute(null);
            verifyNoInteractions(storage);
            assertTrue(Files.exists(original), "Protected bytes must not cause budget pressure");
            config.when(() -> Config.getLongProperty(BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_PROP,
                    BinaryCacheEvictionJob.BINARY_CACHE_MAX_SIZE_MB_DEFAULT)).thenReturn(0L);
            job.execute(null);
            for (Path file : java.util.List.of(original, version, rendition)) {
                assertFalse(Files.exists(file), file.toString());
            }
            for (Path file : protectedFiles) assertTrue(Files.exists(file), file.toString());
            verify(storage, times(3)).hasDurableCopy(anyString(), anyString(), any(File.class));
            clearInvocations(storage);
            config.when(() -> Config.getBooleanProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false))
                    .thenReturn(false);
            Files.writeString(original, "disabled");
            assertFalse(api.evictLocalFile(original.toFile()));
            job.execute(null);
            verifyNoInteractions(storage);
            assertTrue(Files.exists(original));
        }
    }

    @Test
    void symlinkedShardsAreRetainedByScannerAndDirectEviction() throws Exception {
        final Path actualRoot = Files.createDirectories(assetRoot.resolve("actual"));
        final Path file = actualRoot.resolve("a/b/abc123/HeroImage/Friday.PNG");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "original");
        final Path firstRoot = Files.createDirectories(assetRoot.resolve("first"));
        Files.createSymbolicLink(firstRoot.resolve("a"), actualRoot.resolve("a"));
        final Path secondRoot = Files.createDirectories(assetRoot.resolve("second"));
        Files.createDirectories(secondRoot.resolve("a"));
        Files.createSymbolicLink(secondRoot.resolve("a/b"), actualRoot.resolve("a/b"));
        final com.dotcms.storage.StoragePersistenceAPI storage = mock(com.dotcms.storage.StoragePersistenceAPI.class);
        when(storage.hasDurableCopy(anyString(), anyString(), any(File.class))).thenReturn(true);
        final BinaryAssetStorageAPIImpl api = new BinaryAssetStorageAPIImpl(storage);
        try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
            for (Path root : java.util.List.of(firstRoot, secondRoot)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath).thenReturn(root.toString());
                assertTrue(job.collectBinaryAssetFiles(root.toFile()).isEmpty());
                assertFalse(api.evictLocalFile(root.resolve("a/b/abc123/HeroImage/Friday.PNG").toFile()));
            }
            // An alias into the same root must not delete the canonical owned file either.
            Files.createSymbolicLink(actualRoot.resolve("x"), actualRoot.resolve("a"));
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath).thenReturn(actualRoot.toString());
            assertFalse(api.evictLocalFile(actualRoot.resolve("x/b/abc123/HeroImage/Friday.PNG").toFile()));
            assertTrue(Files.exists(file));
            verifyNoInteractions(storage);
        }
    }

    @BeforeEach
    void setUp() {
        // Exercise the real eviction guard; only the durable checksum check is stubbed.
        final com.dotcms.storage.StoragePersistenceAPI storage = mock(com.dotcms.storage.StoragePersistenceAPI.class);
        try {
            when(storage.hasDurableCopy(anyString(), anyString(), any(File.class))).thenReturn(true);
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
        job = new BinaryCacheEvictionJob(() -> new BinaryAssetStorageAPIImpl(storage));
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

            config.when(() -> Config.getBooleanProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false))
                    .thenReturn(true);
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

            config.when(() -> Config.getBooleanProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false))
                    .thenReturn(true);
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

            config.when(() -> Config.getBooleanProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false))
                    .thenReturn(false);

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

            config.when(() -> Config.getBooleanProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false))
                    .thenReturn(true);
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

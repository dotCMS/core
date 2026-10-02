package com.dotcms.storage;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssetStorageFeatureTest {
    @TempDir Path root;
    private String previousFlag;
    private String previousRoot;
    private static final String GROUP = "binary-assets";
    private static final String KEY = "a/b/abc123/HeroImage/MyFile.PNG";

    @BeforeEach
    void configure() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        previousRoot = Config.getStringProperty("ROOT_GROUP_FOLDER_PATH", null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        Config.setProperty("ROOT_GROUP_FOLDER_PATH", root.toString());
    }

    @AfterEach
    void restore() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
        Config.setProperty("ROOT_GROUP_FOLDER_PATH", previousRoot);
    }

    private FileSystemStoragePersistenceAPIImpl filesystem() {
        var fs = new FileSystemStoragePersistenceAPIImpl();
        fs.addGroupMapping(GROUP, root.toFile());
        return fs;
    }

    private ChainableStoragePersistenceAPI chain(StoragePersistenceAPI... providers) {
        return new ChainableStoragePersistenceAPI(new JsonWriterDelegate(), List.of(providers),
                mock(Chainable404StorageCache.class));
    }

    @Test
    void databaseQueryFailuresAreNotReportedAsMissingObjectsWhenEnabled() throws Exception {
        final var connection = mock(java.sql.Connection.class);
        final var queryFailure = new DotDataException("database query failed", new java.sql.SQLException("offline"));
        final var database = new DataBaseStoragePersistenceAPIImpl() {
            @Override
            protected java.sql.Connection getConnection() { return connection; }
        };
        try (var queries = mockConstruction(com.dotmarketing.common.db.DotConnect.class, (query, context) -> {
            when(query.setSQL(anyString())).thenReturn(query);
            when(query.addParam(anyString())).thenReturn(query);
            when(query.loadObjectResults(connection)).thenThrow(queryFailure);
        })) {
            final DotDataException failure = assertThrows(DotDataException.class,
                    () -> database.pullFile("metadata", "/file.json"));
            assertInstanceOf(java.sql.SQLException.class,
                    org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(failure));
            Config.setProperty(AssetStorageFeature.FLAG, false);
            final DotDataException legacy = assertThrows(DotDataException.class,
                    () -> database.pullFile("metadata", "/file.json"));
            assertTrue(legacy.getCause() instanceof com.dotmarketing.exception.DoesNotExistException);
        }
    }

    @Test
    void filesystemMetadataReplacementKeepsPriorValueDuringAndAfterFailedSerialization() throws Exception {
        final var fs = filesystem();
        final var reader = new JsonReaderDelegate<>(String.class);
        fs.pushObject(GROUP, KEY, new JsonWriterDelegate(), "Original", Map.of());
        final var writing = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var executor = Executors.newSingleThreadExecutor();
        try {
            final var failed = executor.submit(() -> fs.pushObject(GROUP, KEY, (out, value) -> {
                out.write(123);
                writing.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new java.io.IOException("writer was not released");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException(e);
                }
                throw new java.io.IOException("injected serialization failure");
            }, "Partial", Map.of()));
            assertTrue(writing.await(5, TimeUnit.SECONDS));
            assertEquals("Original", fs.pullObject(GROUP, KEY, reader));
            release.countDown();
            assertThrows(ExecutionException.class, () -> failed.get(5, TimeUnit.SECONDS));
            assertEquals("Original", fs.pullObject(GROUP, KEY, reader));
            fs.pushObject(GROUP, KEY, new JsonWriterDelegate(), "Replacement", Map.of());
            assertEquals("Replacement", fs.pullObject(GROUP, KEY, reader));
            try (var files = Files.walk(root)) {
                assertFalse(files.anyMatch(file -> file.getFileName().toString().startsWith(".metadata-")));
            }
            Config.setProperty(AssetStorageFeature.FLAG, false);
            final String legacyKey = "legacy-metadata.json";
            fs.pushObject(GROUP, legacyKey, new JsonWriterDelegate(), "Legacy", Map.of());
            fs.pushObject(GROUP, legacyKey, new JsonWriterDelegate(), "Ignored", Map.of());
            assertEquals("Legacy", fs.pullObject(GROUP, legacyKey, reader), "Disabled object writes retain legacy behavior");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void disabledFilesystemProviderKeepsLegacyPathAndMissingFileBehavior() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, false);
        var storage = filesystem();
        File source = Files.writeString(root.resolve("upload.tmp"), "legacy contents").toFile();
        storage.pushFile(GROUP, KEY, source, Map.of());
        Path legacyPath = root.resolve(KEY.toLowerCase());
        assertEquals("legacy contents", Files.readString(legacyPath));
        assertEquals(legacyPath.toFile().getCanonicalFile(), storage.pullFile(GROUP, KEY));
        assertTrue(storage.existsObject(GROUP, KEY));
        assertTrue(storage.deleteObjectAndReferences(GROUP, KEY));
        assertFalse(Files.exists(legacyPath));
        assertThrows(IllegalArgumentException.class, () -> storage.pullFile(GROUP, KEY));
    }

    @Test
    void storageOutagePropagatesAndRecoveryBypassesNegativeCache() throws Exception {
        var provider = mock(StoragePersistenceAPI.class);
        File file = Files.writeString(root.resolve("remote"), "restored").toFile();
        when(provider.pullFile(GROUP, KEY)).thenThrow(new DotDataException("S3 outage")).thenReturn(file);
        var cache = mock(Chainable404StorageCache.class);
        when(cache.is404(anyString(), anyString())).thenReturn(true);
        var chain = new ChainableStoragePersistenceAPI(new JsonWriterDelegate(), List.of(provider), cache);
        assertThrows(DotDataException.class, () -> chain.pullFile(GROUP, KEY));
        assertSame(file, chain.pullFile(GROUP, KEY));
        verifyNoInteractions(cache);
    }

    @Test
    void failedRemoteWriteKeepsPreviousLocalContents() throws Exception {
        var fs = filesystem();
        Path destination = root.resolve(KEY);
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, "previous version");
        File upload = Files.writeString(root.resolve("upload"), "replacement").toFile();
        var remote = mock(StoragePersistenceAPI.class);
        when(remote.pushFile(eq(GROUP), eq(KEY), any(), any())).thenThrow(new DotDataException("upload failed"));
        assertThrows(DotDataException.class, () -> chain(fs, remote).pushFile(GROUP, KEY, upload, Map.of()));
        assertEquals("previous version", Files.readString(destination));
    }

    @Test
    void asyncWriteReportsRemoteFailureAndDoesNotPublishLocalReplacement() throws Exception {
        var fs = filesystem();
        Path destination = root.resolve(KEY);
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, "previous version");
        File upload = Files.writeString(root.resolve("upload"), "replacement").toFile();
        var remote = mock(StoragePersistenceAPI.class);
        when(remote.pushFile(eq(GROUP), eq(KEY), any(), any())).thenThrow(new DotDataException("upload failed"));
        var result = chain(fs, remote).pushFileAsync(GROUP, KEY, upload, Map.of());
        assertInstanceOf(DotDataException.class,
                assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS)).getCause());
        assertEquals("previous version", Files.readString(destination));
    }

    @Test
    void failedRestoreStillReleasesDownload() throws Exception {
        var fs = mock(StoragePersistenceAPI.class);
        var remote = mock(StoragePersistenceAPI.class);
        File download = Files.writeString(root.resolve("download.tmp"), "remote").toFile();
        when(remote.pullFile(GROUP, KEY)).thenReturn(download);
        when(fs.pushFile(eq(GROUP), eq(KEY), any(), any())).thenThrow(new DotDataException("disk full"));
        assertThrows(DotDataException.class, () -> chain(fs, remote).pullFile(GROUP, KEY));
        verify(remote).releaseRetrievedFile(download);
    }

    @Test
    void deleteCannotBeUndoneBySameChainInflightRestore() throws Exception {
        var fs = filesystem();
        var remote = mock(StoragePersistenceAPI.class);
        File snapshot = Files.writeString(root.resolve("download.tmp"), "remote").toFile();
        var fetching = new CountDownLatch(1);
        var finishFetch = new CountDownLatch(1);
        when(remote.pullFile(GROUP, KEY)).thenAnswer(call -> {
            fetching.countDown();
            assertTrue(finishFetch.await(10, TimeUnit.SECONDS));
            return snapshot;
        });
        var chain = chain(fs, remote);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var read = executor.submit(() -> chain.pullFile(GROUP, KEY));
            assertTrue(fetching.await(5, TimeUnit.SECONDS));
            var deleting = new CountDownLatch(1);
            var delete = executor.submit(() -> {
                deleting.countDown();
                return chain.deleteObjectAndReferences(GROUP, KEY);
            });
            assertTrue(deleting.await(5, TimeUnit.SECONDS));
            try {
                assertThrows(TimeoutException.class, () -> delete.get(100, TimeUnit.MILLISECONDS));
            } finally {
                finishFetch.countDown();
            }
            read.get(5, TimeUnit.SECONDS);
            delete.get(5, TimeUnit.SECONDS);
            assertFalse(Files.exists(root.resolve(KEY)));
        }
    }

    @Test
    void deleteCannotBeUndoneBySameChainInflightObjectRestore() throws Exception {
        var fs = filesystem();
        var remote = mock(StoragePersistenceAPI.class);
        var reader = new JsonReaderDelegate<>(String.class);
        var fetching = new CountDownLatch(1);
        var finishFetch = new CountDownLatch(1);
        when(remote.pullObject(GROUP, KEY, reader)).thenAnswer(call -> {
            fetching.countDown();
            assertTrue(finishFetch.await(10, TimeUnit.SECONDS));
            return "remote";
        });
        var chain = chain(fs, remote);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var read = executor.submit(() -> chain.pullObject(GROUP, KEY, reader));
            assertTrue(fetching.await(5, TimeUnit.SECONDS));
            var deleting = new CountDownLatch(1);
            var delete = executor.submit(() -> {
                deleting.countDown();
                return chain.deleteObjectAndReferences(GROUP, KEY);
            });
            assertTrue(deleting.await(5, TimeUnit.SECONDS));
            try {
                assertThrows(TimeoutException.class, () -> delete.get(100, TimeUnit.MILLISECONDS),
                        "A delete must wait for the restore of the same key");
            } finally {
                finishFetch.countDown();
            }
            assertEquals("remote", read.get(5, TimeUnit.SECONDS));
            delete.get(5, TimeUnit.SECONDS);
            assertFalse(Files.exists(root.resolve(KEY.toLowerCase())), "The deleted object must not come back locally");
        }
    }

    @Test
    void unreadableLocalMetadataIsReplacedFromADurableCopyAndOtherwiseFails() throws Exception {
        // Metadata keeps lowercased keys in every slice; the binary groups later become case-preserving.
        final String group = "dotmetadata";
        var fs = new FileSystemStoragePersistenceAPIImpl();
        fs.addGroupMapping(group, root.toFile());
        var reader = new JsonReaderDelegate<>(String.class);
        Path local = root.resolve(KEY.toLowerCase());
        Files.createDirectories(local.getParent());
        Files.write(local, new byte[0]);
        var remote = mock(StoragePersistenceAPI.class);
        when(remote.pullObject(group, KEY, reader)).thenReturn("durable");

        assertEquals("durable", chain(fs, remote).pullObject(group, KEY, reader));
        assertEquals("durable", fs.pullObject(group, KEY, reader), "The local copy is replaced from S3");

        Files.writeString(local, "\"dura");
        assertThrows(UnreadableStoredObjectException.class, () -> fs.pullObject(group, KEY, reader),
                "A truncated local copy is a read failure, not absence");
        var empty = mock(StoragePersistenceAPI.class);
        assertThrows(DotRuntimeException.class, () -> chain(fs, empty).pullObject(group, KEY, reader),
                "Without a readable durable copy the read fails instead of reporting the object as absent");
        assertEquals("\"dura", Files.readString(local), "An unreadable copy without a replacement is kept");
    }

    @Test
    void filesystemListingFindsKeysWhateverTheCaseOfThePrefix() throws Exception {
        // Metadata keeps lowercased keys in every slice; the binary groups later become case-preserving.
        final String group = "dotmetadata";
        var fs = new FileSystemStoragePersistenceAPIImpl();
        fs.addGroupMapping(group, root.toFile());
        File source = Files.writeString(root.resolve("upload.tmp"), "contents").toFile();
        fs.pushFile(group, KEY, source, Map.of());
        assertEquals(List.of(KEY.toLowerCase()), fs.listObjectPaths(group, "a/b/ABC123"));
    }

    @Test
    void s3ExistenceChecksListOneKeyInsteadOfTheWholePrefix() throws Exception {
        var storage = mock(com.dotcms.enterprise.publishing.storage.Storage.class);
        var adapter = new AmazonS3StoragePersistenceAPIImpl(storage, "bucket",
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        when(storage.existsBucket("bucket")).thenReturn(true);
        when(storage.listFirstObject("bucket", GROUP + "/")).thenReturn(summary(GROUP + "/x"));
        assertTrue(adapter.existsGroup(GROUP));

        String key = GROUP + "/a/b/file";
        when(storage.listFirstObject("bucket", key)).thenReturn(summary(key + ".bak"));
        assertFalse(adapter.existsObject(GROUP, "/a/b/file"), "A sibling key that shares the prefix does not count");
        when(storage.listFirstObject("bucket", key + "/")).thenReturn(summary(key + "/child"));
        assertTrue(adapter.existsObject(GROUP, "/a/b/file"), "An object beneath the path counts");
        when(storage.listFirstObject("bucket", key)).thenReturn(summary(key));
        assertTrue(adapter.existsObject(GROUP, "/a/b/file"));

        verify(storage, never()).listObjects(anyString(), anyString());
    }

    private static com.amazonaws.services.s3.model.S3ObjectSummary summary(final String key) {
        var summary = new com.amazonaws.services.s3.model.S3ObjectSummary();
        summary.setKey(key);
        return summary;
    }

    @Test
    void metadataKeysAreStableAcrossConcurrentCalls() throws Exception {
        var adapter = new AmazonS3StoragePersistenceAPIImpl(
                mock(com.dotcms.enterprise.publishing.storage.AWSS3Storage.class), "test",
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.SHA256);
        var transform = AmazonS3StoragePersistenceAPIImpl.class.getDeclaredMethod("transformReadPath", String.class, String.class);
        transform.setAccessible(true);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 8; i++) {
                String directory = "a/b/inode" + i + "/HeroImage/";
                String expected = "dotmetadata/" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(directory) + "/asset-metadata.json";
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int j = 0; j < 1000; j++) {
                        assertEquals(expected, transform.invoke(adapter, "dotmetadata", "/" + directory + "asset-metadata.json"));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (var future : futures) future.get(20, TimeUnit.SECONDS);
        }
    }
}

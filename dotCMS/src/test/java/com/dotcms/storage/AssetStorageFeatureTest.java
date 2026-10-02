package com.dotcms.storage;

import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotcms.storage.binary.BinaryAssetStorageAPIImpl;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
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
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssetStorageFeatureTest {
    @Test
    void disabledPublishingArchivesDoNotInitializeStorage(@org.junit.jupiter.api.io.TempDir Path root) throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, false);
        final var provider = mock(StoragePersistenceAPI.class);
        final var archives = new com.dotcms.publishing.output.BundleArchiveStorage(provider);
        try (var paths = mockStatic(ConfigUtils.class)) {
            paths.when(ConfigUtils::getBundlePath).thenReturn(root.toString());
            final File file = Files.writeString(root.resolve("Mixed-Bundle.tar.gz"), "filesystem bundle").toFile();
            assertEquals(file, archives.get("Mixed-Bundle"));
            archives.store("Mixed-Bundle", file);
            archives.delete("Mixed-Bundle");
            assertEquals("filesystem bundle", Files.readString(file.toPath()));
            verifyNoInteractions(provider);
        }
    }
    @TempDir Path root;
    private String previousFlag;
    private String previousRoot;
    private static final String GROUP = BinaryAssetStorageAPI.BINARY_ASSETS_GROUP;
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
    void disabledFlagPreventsEvictionAndAllRenditionProviderCalls() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, null);
        assertFalse(AssetStorageFeature.isEnabled(), "Absent flag defaults to disabled");
        var provider = mock(StoragePersistenceAPI.class);
        var api = new BinaryAssetStorageAPIImpl(provider);
        File file = Files.writeString(root.resolve("File.PNG"), "cached pixels").toFile();
        api.storeGeneratedFile(file);
        api.deleteGeneratedFiles("abc123");
        assertSame(file, api.getGeneratedFile(file));
        assertFalse(api.evictLocalFile(file));
        try (var lease = api.acquireCacheLease()) {
            assertTrue(file.exists());
        }
        assertTrue(file.exists());
        verifyNoInteractions(provider);
    }

    @Test
    void disabledBackfillDoesNotCallAnyProvider() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, false);
        final var provider = mock(StoragePersistenceAPI.class);
        assertFalse(chain(provider).backfillFile(GROUP, "a/b/abc123/fileAsset/File.Txt",
                root.resolve("missing-file").toFile()));
        assertFalse(chain(provider).backfillObject(GROUP, "unused", new JsonWriterDelegate(),
                new JsonReaderDelegate<>(Map.class), new java.util.HashMap<>()));
        assertFalse(new BinaryAssetStorageAPIImpl(provider).backfillBinary("abc123", "field", null));
        assertThrows(IllegalStateException.class, () -> com.dotcms.storage.binary.BinaryAssetBackfill.runBatch("", 1, () -> { }));
        verifyNoInteractions(provider);
        assertFalse(filesystem().backfillFile(GROUP, "unused", root.resolve("missing-file").toFile()));
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
    void metadataWriteFailurePropagatesOnlyWhenEnabled() throws Exception {
        var storage = mock(FileStorageAPI.class);
        doThrow(new DotDataException("S3 metadata upload failed"))
                .when(storage).putCustomMetadataAttributes(any(), any());
        when(storage.setMetadata(any(), any())).thenThrow(new DotDataException("S3 metadata upload failed"));
        var content = new com.dotmarketing.portlets.contentlet.model.Contentlet();
        content.setInode("abc123");
        content.getMap().put("HeroImage", root.resolve("File.PNG").toFile());
        try (var locator = mockStatic(com.dotmarketing.business.APILocator.class);
             var caches = mockStatic(com.dotmarketing.business.CacheLocator.class)) {
            locator.when(com.dotmarketing.business.APILocator::getFileStorageAPI).thenReturn(storage);
            caches.when(com.dotmarketing.business.CacheLocator::getMetadataCache)
                    .thenReturn(mock(com.dotmarketing.portlets.contentlet.business.MetadataCache.class));
            var api = new FileMetadataAPIImpl();
            Map<String, Map<String, java.io.Serializable>> attributes = Map.of("HeroImage", Map.of("credit", "Author"));
            assertThrows(DotDataException.class, () -> api.putCustomMetadataAttributesForCheckin(content, attributes));
            assertThrows(DotDataException.class, () -> api.putCustomMetadataAttributes("temp_upload", attributes));
            Config.setProperty(AssetStorageFeature.FLAG, false);
            assertDoesNotThrow(() -> api.putCustomMetadataAttributesForCheckin(content, attributes));
            assertDoesNotThrow(() -> api.putCustomMetadataAttributes("temp_upload", attributes));
            verify(storage, times(3)).putCustomMetadataAttributes(any(), any());
            verify(storage).setMetadata(any(), any());
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
    void receivedMetadataOnlyUsesLegacyRecursiveCleanupWhenFeatureIsDisabled() throws Exception {
        final var storage = mock(FileStorageAPI.class);
        final var content = mock(com.dotmarketing.portlets.contentlet.model.Contentlet.class);
        final var type = mock(com.dotcms.contenttype.model.type.ContentType.class);
        when(content.getContentType()).thenReturn(type);
        when(type.fields(com.dotcms.contenttype.model.field.BinaryField.class)).thenReturn(List.of());
        try (var locator = mockStatic(com.dotmarketing.business.APILocator.class);
             var caches = mockStatic(com.dotmarketing.business.CacheLocator.class)) {
            locator.when(com.dotmarketing.business.APILocator::getFileStorageAPI).thenReturn(storage);
            caches.when(com.dotmarketing.business.CacheLocator::getMetadataCache)
                    .thenReturn(mock(com.dotmarketing.portlets.contentlet.business.MetadataCache.class));
            final var api = spy(new FileMetadataAPIImpl());
            doReturn(Map.of()).when(api).removeMetadata(content);
            api.setMetadata(content, Map.of());
            verify(api, never()).removeMetadata(content);
            verifyNoInteractions(storage);
            Config.setProperty(AssetStorageFeature.FLAG, false);
            api.setMetadata(content, Map.of());
            verify(api).removeMetadata(content);
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
    void disabledContentletReadUsesLegacyFilesystemAndPreservesStringTypeGuard() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, false);
        Path path = root.resolve(KEY);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "legacy binary");
        var fileAPI = mock(FileAssetAPI.class);
        when(fileAPI.getRealAssetsRootPath()).thenReturn(root.toString());
        try (var locator = mockStatic(com.dotmarketing.business.APILocator.class)) {
            locator.when(com.dotmarketing.business.APILocator::getFileAssetAPI).thenReturn(fileAPI);
            var content = new com.dotmarketing.portlets.contentlet.model.Contentlet();
            content.setInode("abc123");
            content.getMap().put("HeroImage", "MyFile.PNG");
            assertEquals(path.toFile(), content.getBinary("HeroImage"));
            locator.verify(com.dotmarketing.business.APILocator::getBinaryAssetStorageAPI, never());
        }
    }

    @Test
    void enabledFileWrapperResolvesAgainAfterEviction() throws Exception {
        Path path = root.resolve("Theme.CSS");
        AtomicInteger calls = new AtomicInteger();
        FileAsset wrapper = mock(FileAsset.class, CALLS_REAL_METHODS);
        doAnswer(invocation -> {
            calls.incrementAndGet();
            if (!Files.exists(path)) Files.writeString(path, "body {}");
            return path.toFile();
        }).when(wrapper).getBinary(FileAssetAPI.BINARY_FIELD);
        assertTrue(wrapper.getFileAsset().exists());
        Files.delete(path);
        final var api = mock(BinaryAssetStorageAPI.class);
        final var lease = mock(BinaryAssetStorageAPI.CacheLease.class);
        when(api.acquireCacheLease()).thenReturn(lease);
        try (var locator = mockStatic(com.dotmarketing.business.APILocator.class)) {
            locator.when(com.dotmarketing.business.APILocator::getBinaryAssetStorageAPI).thenReturn(api);
            try (var input = wrapper.getInputStream()) {
                assertEquals("body {}", new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        verify(lease).close();
        assertEquals(2, calls.get());
    }

    @Test
    void enabledThumbnailCleanupDoesNotFallThroughToLegacyShardDeletion() throws Exception {
        final Path generated = root.resolve("generated");
        final Path legacy = generated.resolve("a/b/dotGenerated_resize_1234.png");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "legacy neighboring rendition");
        final var asset = mock(FileAsset.class);
        when(asset.getInode()).thenReturn("abc123");
        final var api = mock(com.dotmarketing.portlets.fileassets.business.FileAssetAPIImpl.class, CALLS_REAL_METHODS);
        doReturn(root.toString()).when(api).getRealAssetsRootPath();
        final var storage = mock(BinaryAssetStorageAPI.class);
        try (var paths = mockStatic(ConfigUtils.class);
             var locator = mockStatic(com.dotmarketing.business.APILocator.class)) {
            paths.when(ConfigUtils::getDotGeneratedPath).thenReturn(generated.toString());
            locator.when(com.dotmarketing.business.APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            api.cleanThumbnailsFromFileAsset(asset);
            assertTrue(Files.exists(legacy), "S3 invalidation must not clear the legacy shared shard");
            Config.setProperty(AssetStorageFeature.FLAG, false);
            api.cleanThumbnailsFromFileAsset(asset);
            assertFalse(Files.exists(legacy), "Disabled behavior must retain main's original cleanup");
            verify(storage, times(1)).deleteGeneratedFiles("abc123");
        }
    }

    @Test
    void stalledRenditionInvalidationDoesNotBlockAnotherAssetRead() throws Exception {
        final var storage = mock(StoragePersistenceAPI.class);
        when(storage.existsGroup(anyString())).thenReturn(true);
        final File other = Files.writeString(root.resolve("Other.PNG"), "other asset").toFile();
        when(storage.pullFile(GROUP, "c/d/cd123/HeroImage/Other.PNG")).thenReturn(other);
        final var api = new BinaryAssetStorageAPIImpl(storage);
        try (var paths = mockStatic(ConfigUtils.class)) {
            paths.when(ConfigUtils::getDotGeneratedPath).thenReturn(root.resolve("generated").toString());
            api.getGeneratedFile(root.resolve("generated/a/b/abc123/dotGenerated_resize_1234.png").toFile());
        }
        final var listing = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        when(storage.listObjectPaths(BinaryAssetStorageAPI.GENERATED_ASSETS_GROUP, "a/b/abc123/"))
                .thenAnswer(call -> {
                    listing.countDown();
                    assertTrue(release.await(10, TimeUnit.SECONDS));
                    return List.of();
                });
        try (var executor = Executors.newFixedThreadPool(2)) {
            var deleting = executor.submit(() -> { api.deleteGeneratedFiles("abc123"); return null; });
            try {
                assertTrue(listing.await(5, TimeUnit.SECONDS));
                var reading = executor.submit(() -> api.getBinaryFile("cd123", "HeroImage", "Other.PNG"));
                assertSame(other, reading.get(5, TimeUnit.SECONDS));
            } finally {
                release.countDown();
            }
            deleting.get(5, TimeUnit.SECONDS);
        }
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
    void coldRestoreDisposesProviderStagingAndKeepsMixedCaseDestination() throws Exception {
        var fs = filesystem();
        File download = Files.writeString(root.resolve("download.tmp"), "remote contents").toFile();
        var remote = mock(StoragePersistenceAPI.class);
        when(remote.pullFile(GROUP, KEY)).thenReturn(download);
        doAnswer(call -> Files.deleteIfExists(download.toPath())).when(remote).releaseRetrievedFile(download);
        File restored = chain(fs, remote).pullFile(GROUP, KEY);
        assertTrue(Files.isSameFile(root.resolve(KEY), restored.toPath()));
        assertEquals("remote contents", Files.readString(restored.toPath()));
        assertFalse(download.exists());
        try (var entries = Files.list(restored.toPath().getParent())) {
            assertEquals(List.of("MyFile.PNG"), entries.map(p -> p.getFileName().toString()).toList());
        }
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

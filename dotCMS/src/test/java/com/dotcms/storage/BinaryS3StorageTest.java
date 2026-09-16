package com.dotcms.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.dotcms.enterprise.publishing.storage.AWSS3Storage;
import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotcms.storage.binary.BinaryAssetStorageAPIImpl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real filesystem + AWS adapter + S3 server. See docs/testing/BINARY_S3_STORAGE.md. */
@EnabledIfSystemProperty(named = "s3.test.endpoint", matches = ".+")
class BinaryS3StorageTest {
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

    private static final String GROUP = BinaryAssetStorageAPI.BINARY_ASSETS_GROUP;
    private static final String CREDENTIAL = "binary-storage-test";
    @TempDir Path root;
    private AmazonS3 client;
    private AWSS3Storage storage;
    private AmazonS3StoragePersistenceAPIImpl s3;
    private FileSystemStoragePersistenceAPIImpl fs;
    private BinaryAssetStorageAPIImpl api;
    private MockedStatic<ConfigUtils> configUtils;
    private String bucket;
    private boolean versioned;
    private String previousRoot;

    @BeforeEach
    void setUp() {
        final String endpoint = System.getProperty("s3.test.endpoint");
        client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(CREDENTIAL, CREDENTIAL)))
                .build();
        bucket = "binary-test-" + UUID.randomUUID();
        client.createBucket(bucket);
        storage = new AWSS3Storage(new AWSS3Configuration.Builder()
                .accessKey(CREDENTIAL).secretKey(CREDENTIAL).endPoint(endpoint).region("us-east-1").build());
        s3 = new AmazonS3StoragePersistenceAPIImpl(storage, bucket,
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        previousRoot = Config.getStringProperty("ROOT_GROUP_FOLDER_PATH", null);
        Config.setProperty("ROOT_GROUP_FOLDER_PATH", root.toString());
        fs = new FileSystemStoragePersistenceAPIImpl();
        configUtils = mockStatic(ConfigUtils.class);
        configUtils.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
        configUtils.when(ConfigUtils::getDotGeneratedPath).thenReturn(root.resolve("dotGenerated").toString());
        configUtils.when(ConfigUtils::getBundlePath).thenReturn(root.resolve("bundles").toString());
        api = new BinaryAssetStorageAPIImpl(new ChainableStoragePersistenceAPI(
                new JsonWriterDelegate(), List.of(fs, s3), mock(Chainable404StorageCache.class)));
    }

    @AfterEach
    void tearDown() {
        if (configUtils != null) {
            configUtils.close();
        }
        Config.setProperty("ROOT_GROUP_FOLDER_PATH", previousRoot);
        if (storage != null) {
            storage.shutdownTransferManager();
        }
        if (client != null) {
            if (bucket != null) {
                if (versioned) {
                    var versions = client.listVersions(bucket, "");
                    while (true) {
                        versions.getVersionSummaries().forEach(version ->
                                client.deleteVersion(bucket, version.getKey(), version.getVersionId()));
                        if (!versions.isTruncated()) {
                            break;
                        }
                        versions = client.listNextBatchOfVersions(versions);
                    }
                }
                client.listObjectsV2(bucket).getObjectSummaries()
                        .forEach(object -> client.deleteObject(bucket, object.getKey()));
                client.deleteBucket(bucket);
            }
            client.shutdown();
        }
    }

    @Test
    void installationsKeepIndependentOwnersWhileSharingBinaryBytesAndExtraction() throws Exception {
        final var secondClient = new AWSS3Storage(new AWSS3Configuration.Builder()
                .accessKey(CREDENTIAL).secretKey(CREDENTIAL).endPoint(System.getProperty("s3.test.endpoint"))
                .region("us-east-1").build());
        try {
            final var first = namespaced("Client-Prod", AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE, storage);
            final var second = namespaced("Client-Prod2", AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE, secondClient);
            final String path = "a/b/abc123/HeroImage/MixedCase.TXT";
            final File source = Files.writeString(root.resolve("shared-input"), "same original bytes").toFile();
            final File replacement = Files.writeString(root.resolve("replacement"), "other installation bytes").toFile();
            s3.pushFile(GROUP, path, replacement, Map.of());
            assertFalse(first.existsGroup(GROUP), "Root-layout owners do not belong to a new namespace");
            assertNull(first.pullFile(GROUP, path), "A missing namespace must not fall back to legacy owners");
            first.createGroup(GROUP);
            first.pushFile(GROUP, path, source, Map.of());
            second.pushFile(GROUP, path, source, Map.of());
            final String hash = S3ContentAddressedStorage.hash(source);
            for (String namespace : List.of("Client-Prod", "Client-Prod2")) {
                assertEquals(hash, client.getObjectMetadata(bucket,
                        "asset-namespaces/" + namespace + "/" + GROUP + "/" + path)
                        .getUserMetaDataOf(S3ContentAddressedStorage.HASH_HEADER));
            }
            assertEquals(2, client.listObjectsV2(bucket, S3ContentAddressedStorage.BLOB_PREFIX).getKeyCount(),
                    "Shared originals plus the different root-layout bytes");
            assertEquals(List.of(path), first.listObjectPaths(GROUP, "a/b/abc123/HeroImage/"));
            assertEquals("same original bytes", readRemote(first, GROUP, path));

            final var extractions = new java.util.concurrent.atomic.AtomicInteger();
            java.util.function.Function<File, Map<String, java.io.Serializable>> extract = file -> {
                extractions.incrementAndGet();
                return Map.of("content", "same extracted content");
            };
            new SharedExtractedMetadata(first).get(source, "parser-v1", 1, 1000, extract);
            new SharedExtractedMetadata(second).get(source, "parser-v1", 1, 1000, extract);
            assertEquals(1, extractions.get(), "Byte-derived metadata is shared across namespace owners");
            assertEquals(1, client.listObjectsV2(bucket, SharedExtractedMetadata.GROUP + "/")
                    .getObjectSummaries().stream().filter(object -> !object.getKey().endsWith("/")).count());

            second.pushFile(GROUP, path, replacement, Map.of());
            assertEquals("same original bytes", readRemote(first, GROUP, path));
            assertEquals("other installation bytes", readRemote(second, GROUP, path));
            assertTrue(first.hasDurableCopy(GROUP, path, source));
            assertFalse(second.hasDurableCopy(GROUP, path, source), "Other owners cannot authorize local eviction");
            first.pushFile(GROUP + "-neighbor", path, source, Map.of());
            first.deleteGroup(GROUP);
            assertFalse(first.existsGroup(GROUP), "Deleting a group also invalidates its local existence cache");
            assertTrue(first.listObjectPaths(GROUP, "a/b/abc123/HeroImage/").isEmpty());
            assertNull(first.pullFile(GROUP, path));
            assertEquals("same original bytes", readRemote(first, GROUP + "-neighbor", path));
            final var restarted = namespaced("Client-Prod2", AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE, storage);
            assertEquals("other installation bytes", readRemote(restarted, GROUP, path));
            assertEquals("other installation bytes", readRemote(s3, GROUP, path), "Legacy owner remains intact");
            assertTrue(client.doesObjectExist(bucket, S3ContentAddressedStorage.blobKey(hash)), "Shared pool is not group-owned");
        } finally { secondClient.shutdownTransferManager(); }
    }

    @Test
    void namespaceCoversHashedMetadataAndConditionalStagingRecords() throws Exception {
        final var first = namespaced("Prod", AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.SHA256, storage);
        final var second = namespaced("prod", AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.SHA256, storage);
        final String path = "/a/b/abc123/HeroImage-metadata.json";
        first.pushObject("dotmetadata", path, new JsonWriterDelegate(), "first editorial value", Map.of());
        second.pushObject("dotmetadata", path, new JsonWriterDelegate(), "second editorial value", Map.of());
        assertEquals(List.of(path), first.listObjectPaths("dotmetadata", "/a/b/abc123/"));
        assertEquals("first editorial value", first.pullObject("dotmetadata", path, new JsonReaderDelegate<>(String.class)));
        first.deleteObjectAndReferences("dotmetadata", path);
        assertEquals("second editorial value", second.pullObject("dotmetadata", path, new JsonReaderDelegate<>(String.class)));

        final var stagingA = namespaced("Prod", AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE, storage);
        final var stagingB = namespaced("prod", AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE, storage);
        final String record = "entries/Mixed-Case.json";
        final String firstVersion = stagingA.writeObjectIfMatch("webdav-temporary", record, "first upload", null);
        final String secondVersion = stagingB.writeObjectIfMatch("webdav-temporary", record, "second upload", null);
        assertNotNull(firstVersion);
        assertNotNull(secondVersion, "Same logical path can be created independently in another namespace");
        assertNull(stagingB.writeObjectIfMatch("webdav-temporary", record, "stale edit", firstVersion));
        assertEquals(record, stagingA.listObjectSnapshots("webdav-temporary", "entries/").get(0).path());
        stagingA.deleteObjectReference("webdav-temporary", record);
        assertEquals("second upload", stagingB.readObjectSnapshot("webdav-temporary", record,
                new JsonReaderDelegate<>(String.class)).value());
        assertFalse(stagingA.existsObject("webdav-temporary", record));
        assertTrue(stagingB.existsObject("webdav-temporary", record));
    }

    private AmazonS3StoragePersistenceAPIImpl namespaced(String namespace,
            AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode mode,
            com.dotcms.enterprise.publishing.storage.Storage backing) {
        final String previous = Config.getStringProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_NAMESPACE_PROP, null);
        try {
            Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_NAMESPACE_PROP, namespace);
            return new AmazonS3StoragePersistenceAPIImpl(backing, bucket, mode);
        } finally { Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_NAMESPACE_PROP, previous); }
    }

    private String readRemote(AmazonS3StoragePersistenceAPIImpl remote, String group, String path) throws Exception {
        final File file = remote.pullFile(group, path);
        assertNotNull(file);
        try { return Files.readString(file.toPath()); }
        finally { remote.releaseRetrievedFile(file); }
    }

    @Test
    void versionInventoryFailurePropagatesAsCheckedStorageFailure() {
        final var failedStorage = spy(storage);
        final var outage = new com.dotmarketing.exception.DotRuntimeException("S3 listing unavailable");
        doThrow(outage).when(failedStorage).listObjects(bucket, "webdav-temporary/data/");
        final var failed = new AmazonS3StoragePersistenceAPIImpl(failedStorage, bucket,
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        final var failure = assertThrows(com.dotmarketing.exception.DotDataException.class,
                () -> failed.listObjectSnapshots(WebdavTemporaryStorage.GROUP, "data/"));
        assertSame(outage, failure.getCause());
    }

    @Test
    void webdavStagingSurvivesColdNodesOverwriteCopyAndDelete() throws Exception {
        final var remote = spy(s3);
        final var first = new WebdavTemporaryStorage(remote, root.resolve("dav-a"));
        final var second = new WebdavTemporaryStorage(s3, root.resolve("dav-b"));
        final File directory = first.file("example.com/(Mixed-Staging)");
        final String path = "example.com/(Mixed-Staging)/Hero-Mixed.GIF";
        final File source = Files.writeString(root.resolve("completed-upload"), "first complete bytes").toFile();
        first.mkdir(directory);
        first.mkdir(first.file("example.com/(Mixed-Staging)/Empty"));
        first.store(first.file(path), source);
        final var snapshot = second.stat(second.file(path));
        assertEquals("Hero-Mixed.GIF", second.materialize(snapshot).getName());
        assertEquals("first complete bytes", Files.readString(second.materialize(snapshot).toPath()));
        assertFalse(second.file(path).exists(), "Logical lookup must not rely on local pathname files");
        assertEquals(2, second.children(second.file("example.com/(Mixed-Staging)")).size(),
                "S3 entries: " + s3.listObjectPaths(WebdavTemporaryStorage.GROUP, "entries/")
                        + "; keys: " + client.listObjectsV2(bucket).getObjectSummaries().stream().map(o -> o.getKey()).toList());
        assertNull(second.stat(second.file(path.toLowerCase(java.util.Locale.ROOT))));
        assertThrows(java.io.IOException.class, () -> first.store(directory, source));

        Files.writeString(source.toPath(), "replacement bytes");
        doReturn(false).when(remote).backfillFile(anyString(), anyString(), any());
        assertThrows(java.io.IOException.class, () -> first.store(first.file(path), source));
        assertEquals(snapshot.dataKey(), second.stat(second.file(path)).dataKey());
        doCallRealMethod().when(remote).backfillFile(anyString(), anyString(), any());
        first.store(first.file(path), source);
        assertNotEquals(snapshot.dataKey(), second.stat(second.file(path)).dataKey());
        Files.delete(second.materialize(snapshot).toPath());
        assertEquals("first complete bytes", Files.readString(second.materialize(snapshot).toPath()));
        assertEquals("replacement bytes", Files.readString(second.materialize(second.stat(second.file(path))).toPath()));

        final File copied = second.file("example.com/(Copy)");
        second.copy(second.file("example.com/(Mixed-Staging)"), copied);
        assertTrue(second.stat(new File(copied, "Empty")).directory());
        assertEquals("replacement bytes", Files.readString(second.materialize(second.stat(new File(copied, "Hero-Mixed.GIF"))).toPath()));
        second.copy(copied, copied);
        assertNotNull(second.stat(copied));
        assertThrows(java.io.IOException.class, () -> second.copy(copied, new File(copied, "Nested")));
        final File sibling = first.file("example.com/(Copy)-neighbor.TXT");
        first.store(sibling, source);
        // Stale local bytes cannot resurrect a remotely deleted resource.
        Files.createDirectories(copied.toPath());
        Files.writeString(copied.toPath().resolve("Hero-Mixed.GIF"), "stale local bytes");
        first.delete(first.file("example.com/(Copy)"));
        assertNull(second.stat(new File(copied, "Hero-Mixed.GIF")));
        assertNull(second.stat(copied));
        assertNotNull(second.stat(second.file("example.com/(Copy)-neighbor.TXT")));
        first.delete(directory);
        assertNull(second.stat(second.file(path)));
        assertTrue(second.children(second.file("example.com/(Mixed-Staging)")).isEmpty());

        Files.createDirectories(root.resolve("dav-a"));
        Files.createSymbolicLink(root.resolve("dav-a/alias"), root.resolve("dav-a/elsewhere"));
        assertThrows(java.io.IOException.class, () -> first.file("alias/file"));
        assertThrows(java.io.IOException.class, () -> first.file("../escape"));
        assertThrows(java.io.IOException.class, () -> first.file(".webdav-cache/data/fake"));
        clearInvocations(remote);
        Config.setProperty(AssetStorageFeature.FLAG, false);
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> first.mkdir(directory));
        verifyNoInteractions(remote);
    }

    @Test
    void conditionalS3RecordsRejectStaleWritesAndTombstoneRaces() throws Exception {
        final String group = WebdavTemporaryStorage.GROUP;
        s3.createGroup(group);
        final String path = "conditional/Record.json";
        final var first = new java.util.HashMap<String, java.io.Serializable>(Map.of("value", "first"));
        final var second = new java.util.HashMap<String, java.io.Serializable>(Map.of("value", "second"));
        final String version = s3.writeObjectIfMatch(group, path, first, null);
        assertNotNull(version);
        assertNull(s3.writeObjectIfMatch(group, path, second, null));
        assertNotNull(s3.writeObjectIfMatch(group, path, second, version));
        assertNull(s3.writeObjectIfMatch(group, path, first, version));
        final var tombstone = new java.util.HashMap<String, java.io.Serializable>(Map.of("deleted", UUID.randomUUID().toString()));
        assertNull(s3.writeObjectIfMatch(group, path, tombstone, version));
        final var current = s3.readObjectSnapshot(group, path, new JsonReaderDelegate<>(Map.class));
        assertEquals(second, current.value());
        assertTrue(current.modified() > 0);
        assertNotNull(s3.writeObjectIfMatch(group, path, tombstone, current.version()));
        assertNull(s3.writeObjectIfMatch(group, path, first, current.version()), "A late writer cannot resurrect a tombstoned reservation");
        assertEquals(tombstone, s3.readObjectSnapshot(group, path, new JsonReaderDelegate<>(Map.class)).value());
    }

    @Test
    void webdavCleanupRetainsReferencedOldPayloadThenExpiresTheRecordAndBytes() throws Exception {
        final var delayed = spy(s3);
        final var dav = new WebdavTemporaryStorage(delayed, root.resolve("dav"));
        final File logical = dav.file("example.com/(Temporary)/Mixed%2E.GIF");
        final File source = Files.writeString(root.resolve("completed"), "referenced bytes").toFile();
        doAnswer(call -> {
            final boolean uploaded = (boolean) call.callRealMethod();
            Thread.sleep(1100); // Make payload older than the subsequently published record at S3's timestamp precision.
            return uploaded;
        }).when(delayed).backfillFile(anyString(), anyString(), any());
        dav.store(logical, source);
        final var current = dav.stat(logical);
        final var cleanupTime = new java.util.concurrent.atomic.AtomicLong(
                s3.listObjectSnapshots(WebdavTemporaryStorage.GROUP, "entries/").get(0).modified() / 1000 * 1000);
        pinWebdavCleanupClock(delayed, cleanupTime);
        cleanWebdavNow(dav);
        assertEquals("referenced bytes", Files.readString(dav.materialize(current).toPath()));
        assertNotNull(dav.stat(logical), "An aged payload remains protected by its newer completed record");
        Thread.sleep(1100);
        cleanupTime.set(0);
        cleanWebdavNow(dav);
        assertNull(dav.stat(logical));
        assertWebdavTombstones();
        assertTrue(s3.listObjectPaths(WebdavTemporaryStorage.GROUP, "data/").isEmpty());
        assertTrue(s3.listObjectPaths(WebdavTemporaryStorage.GROUP, "maintenance/").isEmpty());
    }

    @Test
    void webdavCleanupFencesAnExpiredUploadBeforeItCanPublish() throws Exception {
        final var delayed = spy(s3);
        final var writer = new WebdavTemporaryStorage(delayed, root.resolve("writer"));
        final var cleaner = new WebdavTemporaryStorage(s3, root.resolve("cleaner"));
        final File logical = writer.file("example.com/(Temporary)/Late.TXT");
        final File source = Files.writeString(root.resolve("late-upload"), "late bytes").toFile();
        doAnswer(call -> {
            final boolean uploaded = (boolean) call.callRealMethod();
            Thread.sleep(1100);
            cleanWebdavNow(cleaner);
            return uploaded;
        }).when(delayed).backfillFile(anyString(), anyString(), any());
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> writer.store(logical, source));
        assertNull(writer.stat(logical));
        assertEquals("late bytes", Files.readString(source.toPath()), "A rejected publication retains the caller's source");
        assertWebdavTombstones();
        assertTrue(s3.listObjectPaths(WebdavTemporaryStorage.GROUP, "data/").isEmpty());
    }

    @Test
    void webdavCleanupAbortsIfAWriterReplacesTheInspectedRecord() throws Exception {
        final var cleanupRemote = spy(s3);
        final var writer = new WebdavTemporaryStorage(s3, root.resolve("writer"));
        final var cleaner = new WebdavTemporaryStorage(cleanupRemote, root.resolve("cleaner"));
        final File logical = writer.file("example.com/(Temporary)/Replaced.TXT");
        final File source = Files.writeString(root.resolve("before-cleanup"), "old bytes").toFile();
        writer.store(logical, source);
        Thread.sleep(1100);
        final var raced = new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(call -> {
            final String path = call.getArgument(1);
            if (path.startsWith("entries/") && raced.compareAndSet(false, true)) {
                Files.writeString(source.toPath(), "new bytes");
                writer.store(logical, source);
            }
            return call.callRealMethod();
        }).when(cleanupRemote).writeObjectIfMatch(anyString(), anyString(), any(), anyString());
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> cleanWebdavNow(cleaner));
        assertTrue(raced.get());
        assertEquals("new bytes", Files.readString(writer.materialize(writer.stat(logical)).toPath()));
        // The failed pass must not have collected any payloads using its stale reference inventory.
        assertEquals(2, s3.listObjectPaths(WebdavTemporaryStorage.GROUP, "data/").size());
        pinWebdavCleanupClock(cleanupRemote, new java.util.concurrent.atomic.AtomicLong(
                s3.listObjectSnapshots(WebdavTemporaryStorage.GROUP, "entries/").get(0).modified() / 1000 * 1000));
        cleanWebdavNow(cleaner);
        assertEquals("new bytes", Files.readString(writer.materialize(writer.stat(logical)).toPath()));
        assertEquals(1, s3.listObjectPaths(WebdavTemporaryStorage.GROUP, "data/").size());
    }

    private void assertWebdavTombstones() throws Exception {
        for (String path : s3.listObjectPaths(WebdavTemporaryStorage.GROUP, "entries/")) {
            final var value = (Map<?, ?>) s3.readObjectSnapshot(WebdavTemporaryStorage.GROUP, path,
                    new JsonReaderDelegate<>(Map.class)).value();
            assertFalse(value.containsKey("dataKey"));
            assertFalse(value.containsKey("pending"));
            assertTrue(value.containsKey("mutation"), "Retain the fencing token for delayed writers");
        }
    }

    private static void pinWebdavCleanupClock(AmazonS3StoragePersistenceAPIImpl remote,
            java.util.concurrent.atomic.AtomicLong time) throws Exception {
        doAnswer(call -> {
            final var actual = (AmazonS3StoragePersistenceAPIImpl.ObjectSnapshot) call.callRealMethod();
            if (actual != null && actual.path().startsWith("maintenance/") && time.get() != 0) {
                return new AmazonS3StoragePersistenceAPIImpl.ObjectSnapshot(actual.path(), actual.value(), actual.version(), time.get());
            }
            return actual;
        }).when(remote).readObjectSnapshot(anyString(), anyString(), any());
    }

    private static void cleanWebdavNow(WebdavTemporaryStorage dav) throws Exception {
        final String previous = Config.getStringProperty("CLEANUP_TMP_FILES_OLDER_THAN_HOURS", null);
        try {
            Config.setProperty("CLEANUP_TMP_FILES_OLDER_THAN_HOURS", 0);
            dav.cleanupExpired();
        } finally {
            Config.setProperty("CLEANUP_TMP_FILES_OLDER_THAN_HOURS", previous);
        }
    }

    @Test
    void temporaryUploadsSurviveColdCacheAndExpireWithTheirAccessRecord() throws Exception {
        TemporaryAssetStorageTest.assertRoundTrip(s3, root);
    }

    @Test
    void temporaryMetadataUsesPortableS3KeysAndBypassesNodeCaches() throws Exception {
        final var remote = new AmazonS3StoragePersistenceAPIImpl(storage, bucket,
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.SHA256);
        final var providers = mock(StoragePersistenceProvider.class);
        when(providers.getStorage(StorageType.S3)).thenReturn(remote);
        when(providers.getStorage(StorageType.FILE_SYSTEM)).thenReturn(fs);
        fs.addGroupMapping(FileMetadataAPI.DOT_METADATA, root.toFile());
        final var cache = mock(com.dotmarketing.portlets.contentlet.business.MetadataCache.class);
        when(cache.getMetadataMap(anyString())).thenReturn(Map.of("dot:focalPoint", "stale"));
        final var files = new FileStorageAPIImpl(new JsonReaderDelegate<>(Map.class), new JsonWriterDelegate(),
                mock(MetadataGenerator.class), providers, cache);
        try (var locator = mockStatic(com.dotmarketing.business.APILocator.class);
             var caches = mockStatic(com.dotmarketing.business.CacheLocator.class)) {
            locator.when(com.dotmarketing.business.APILocator::getFileStorageAPI).thenReturn(files);
            caches.when(com.dotmarketing.business.CacheLocator::getMetadataCache).thenReturn(cache);
            configUtils.when(ConfigUtils::getAssetTempPath).thenReturn(root.resolve("node-a/tmp_upload").toString());
            final var nodeA = new FileMetadataAPIImpl();
            final String id = "temp_Mixed-Case";
            nodeA.putCustomMetadataAttributes(id, Map.of("HeroImage", Map.of("credit", "Author", "focalPoint", "0.25,0.5")));
            configUtils.when(ConfigUtils::getAssetTempPath).thenReturn(root.resolve("node-b/other/tmp_upload").toString());
            final var nodeB = new FileMetadataAPIImpl();
            assertEquals("0.25,0.5", nodeB.getMetadata(id).orElseThrow().getCustomMeta().get("focalPoint"));
            nodeB.putCustomMetadataAttributes(id, Map.of("HeroImage", Map.of("focalPoint", "0.75,0.5")));
            assertEquals(Map.of("credit", "Author", "focalPoint", "0.75,0.5"),
                    nodeA.getMetadata(id).orElseThrow().getCustomMeta());
            nodeB.putCustomMetadataAttributes(id, Map.of("HeroImage", Map.of()));
            assertTrue(nodeA.getMetadata(id).orElseThrow().getCustomMeta().isEmpty());
            verify(cache, never()).getMetadataMap(anyString());
            remote.deleteObjectAndReferences(FileMetadataAPI.DOT_METADATA,
                    "/tmp_upload/" + id + "/" + id + FileMetadataAPI.META_TMP);
            assertTrue(nodeA.getMetadata(id).isEmpty());
        }
    }

    @Test
    void publishingArchivesPreserveCaseAndLastCompletedUpload() throws Exception {
        final var archives = new com.dotcms.publishing.output.BundleArchiveStorage(
                new ChainableStoragePersistenceAPI(new JsonWriterDelegate(), List.of(fs, s3), mock(Chainable404StorageCache.class)));
        final String id = "Mixed-Case-Bundle";
        final String key = com.dotcms.publishing.output.BundleArchiveStorage.GROUP + "/" + id + ".tar.gz";
        final File source = Files.writeString(root.resolve("archive.tmp"), "last complete archive").toFile();
        archives.store(id, source);
        final File local = archives.get(id);
        assertEquals(id + ".tar.gz", local.getName());
        assertEquals(Files.readString(source.toPath()), client.getObjectAsString(bucket, key));
        Files.delete(local.toPath()); // Only this verified test cache copy is cleared.
        assertTrue(archives.exists(id));
        assertFalse(local.exists(), "An existence check must not download the archive");
        assertEquals("last complete archive", Files.readString(archives.get(id).toPath()));

        try (var incomplete = new java.io.InputStream() {
            int remaining = 10;
            @Override public int read() throws java.io.IOException {
                if (--remaining < 0) throw new java.io.IOException("Interrupted upload");
                return 'x';
            }
        }) {
            assertThrows(java.io.IOException.class, () -> archives.receive(id + ".tar.gz", incomplete));
        }
        assertEquals("last complete archive", Files.readString(local.toPath()));
        assertEquals("last complete archive", client.getObjectAsString(bucket, key));
        try (var files = Files.list(local.toPath().getParent())) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".bundle-upload-")));
        }
        assertThrows(IllegalArgumentException.class, () -> archives.get("../outside"));
        final Path alias = local.toPath().resolveSibling("Alias.tar.gz");
        Files.createSymbolicLink(alias, local.toPath());
        assertThrows(IllegalArgumentException.class, () -> archives.get("Alias"));
        Files.delete(alias);
        archives.store(id + "-neighbor", source);
        archives.store(id + ".tar.gz-shadow", source);
        try (var input = Files.newInputStream(local.toPath())) {
            archives.delete(id);
            assertEquals("last complete archive", new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
        assertFalse(archives.get(id).exists());
        assertFalse(archives.exists(id), "A longer neighboring object key is not the requested archive");
        assertFalse(client.doesObjectExist(bucket, key));
        assertEquals("last complete archive", Files.readString(archives.get(id + "-neighbor").toPath()));

        final var failing = mock(StoragePersistenceAPI.class);
        when(failing.pullFile(anyString(), anyString())).thenThrow(new com.dotmarketing.exception.DotDataException("S3 unavailable"));
        assertThrows(com.dotmarketing.exception.DotRuntimeException.class,
                () -> new com.dotcms.publishing.output.BundleArchiveStorage(failing).get(id));
    }

    @Test
    void immutableReplacementCommitsAndRollsBackWithRealPostgres() throws Exception {
        String jdbc = System.getProperty("s3.test.jdbc");
        org.junit.jupiter.api.Assumptions.assumeTrue(jdbc != null, "Supply s3.test.jdbc for transaction coverage");
        String schema = "binary_revisions_" + UUID.randomUUID().toString().replace("-", "");
        var contentApi = new BinaryAssetStorageAPIImpl(new ChainableStoragePersistenceAPI(
                new JsonWriterDelegate(), List.of(fs, s3), mock(Chainable404StorageCache.class)), true);
        try (var writer = java.sql.DriverManager.getConnection(jdbc, CREDENTIAL, CREDENTIAL);
             var observer = java.sql.DriverManager.getConnection(jdbc, CREDENTIAL, CREDENTIAL)) {
            writer.createStatement().execute("create schema " + schema);
            try {
                writer.createStatement().execute("set search_path to " + schema);
                observer.createStatement().execute("set search_path to " + schema);
                writer.createStatement().execute("create table contentlet (inode varchar(255) primary key, contentlet_as_json jsonb)");
                writer.createStatement().execute("insert into contentlet values ('abc123', '{}')");
                File source = root.resolve("upload.tmp").toFile();
                Files.writeString(source.toPath(), "last committed pixels");
                File original = contentApi.storeRevision("abc123", "HeroImage", "Friday.GIF", source);
                String originalKey = com.dotcms.storage.binary.BinaryAssetReference.keyOf(original);
                writeReference(writer, originalKey);

                writer.setAutoCommit(false);
                Files.writeString(source.toPath(), "uncommitted replacement");
                File replacement = contentApi.storeRevision("abc123", "HeroImage", "Friday.GIF", source);
                String replacementKey = com.dotcms.storage.binary.BinaryAssetReference.keyOf(replacement);
                assertNotEquals(originalKey, replacementKey);
                writeReference(writer, replacementKey);
                com.dotmarketing.db.DbConnectionFactory.setConnection(observer);
                assertEquals("last committed pixels", Files.readString(contentApi.getBinaryFile("abc123", "HeroImage").toPath()));
                writer.rollback();
                assertTrue(contentApi.evictLocalFile(original));
                assertTrue(contentApi.evictLocalFile(replacement));
                assertEquals("last committed pixels", Files.readString(contentApi.getBinaryFile("abc123", "HeroImage", "Friday.GIF").toPath()));

                // The same immutable replacement can be referenced by a later successful save.
                writeReference(writer, replacementKey);
                writer.commit();
                File current = contentApi.getBinaryFile("abc123", "HeroImage");
                assertEquals("Friday.GIF", current.getName());
                assertEquals("uncommitted replacement", Files.readString(current.toPath()));
                assertTrue(contentApi.evictLocalFile(original));
                try (var locator = mockStatic(com.dotmarketing.business.APILocator.class)) {
                    locator.when(com.dotmarketing.business.APILocator::getBinaryAssetStorageAPI).thenReturn(contentApi);
                    var snapshot = new com.dotmarketing.portlets.contentlet.model.Contentlet();
                    snapshot.setInode("abc123");
                    snapshot.getMap().put("HeroImage", original);
                    assertEquals("last committed pixels", Files.readString(snapshot.getBinary("HeroImage").toPath()));
                }
                assertEquals("last committed pixels", readStoredBinary(originalKey));

                // An authoritative cleared field must not discover an older revision by listing.
                writer.createStatement().execute("update contentlet set contentlet_as_json = '{}'");
                writer.commit();
                assertNull(contentApi.getBinaryFile("abc123", "HeroImage"));
                contentApi.deleteAllBinaries("abc123");
                assertTrue(client.listObjectsV2(bucket, GROUP + "/a/b/abc123/").getObjectSummaries().isEmpty());
            } finally {
                writer.rollback();
                writer.setAutoCommit(true);
                writer.createStatement().execute("drop schema " + schema + " cascade");
            }
        } finally {
            com.dotmarketing.db.DbConnectionFactory.closeConnection();
        }
    }

    private void writeReference(java.sql.Connection connection, String key) throws Exception {
        var binary = com.dotcms.content.model.type.system.BinaryFieldType.builder()
                .value("Friday.GIF").storageKey(key).build();
        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(Map.of("fields", Map.of("HeroImage", binary)));
        try (var statement = connection.prepareStatement("update contentlet set contentlet_as_json = ?::jsonb where inode = 'abc123'")) {
            statement.setString(1, json);
            assertEquals(1, statement.executeUpdate());
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "s3.test.sts.accessKey", matches = ".+")
    void temporaryRoleCredentialsCanRefreshDuringBinaryLifecycle() throws Exception {
        final var sts = com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        System.getProperty("s3.test.endpoint"), "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                        System.getProperty("s3.test.sts.accessKey"), System.getProperty("s3.test.sts.secretKey"))))
                .build();
        final var credentials = new com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider.Builder(
                "arn:aws:iam::123456789012:role/BinaryStorageTest", "binary-storage-test")
                .withStsClient(sts).withRoleSessionDurationSeconds(900)
                .withScopeDownPolicy("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:*\",\"Resource\":[\"arn:aws:s3:::"
                        + bucket + "\",\"arn:aws:s3:::" + bucket + "/*\"]}]}")
                .build();
        final var roleStorage = new AWSS3Storage(credentials, System.getProperty("s3.test.endpoint"), "us-east-1");
        try {
            final var remote = new AmazonS3StoragePersistenceAPIImpl(roleStorage, bucket,
                    AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
            final var binaries = new BinaryAssetStorageAPIImpl(new ChainableStoragePersistenceAPI(
                    new JsonWriterDelegate(), List.of(fs, remote), mock(Chainable404StorageCache.class)));
            final File source = Files.writeString(root.resolve("role-upload"), "temporary role credentials").toFile();
            binaries.storeBinary("abc123", "MixedField", "Role.TXT", source);
            final String firstAccess = credentials.getCredentials().getAWSAccessKeyId();
            assertFalse(credentials.getCredentials().getSessionToken().isEmpty());
            credentials.refresh();
            assertNotEquals(firstAccess, credentials.getCredentials().getAWSAccessKeyId());
            final File cached = binaries.getBinaryFile("abc123", "MixedField", "Role.TXT");
            assertTrue(binaries.evictLocalFile(cached));
            assertEquals("temporary role credentials", Files.readString(
                    binaries.getBinaryFile("abc123", "MixedField", "Role.TXT").toPath()));
            binaries.deleteBinary("abc123", "MixedField");
            assertTrue(client.listObjectsV2(bucket, GROUP + "/a/b/abc123/MixedField/").getObjectSummaries().isEmpty());
        } finally {
            roleStorage.shutdownTransferManager();
            credentials.close();
            sts.shutdown();
        }
    }

    @Test
    void configuredDefaultCredentialsSupportUploadEvictionRetrievalAndDeletion() throws Exception {
        final var previous = new java.util.HashMap<String, String>();
        final var properties = Map.of(
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_BUCKET_NAME_PROP, bucket,
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_REGION_PROP, "us-east-1",
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_ENDPOINT_PROP, System.getProperty("s3.test.endpoint"),
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_ACCESS_KEY_PROP, "",
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_SECRET_ACCESS_KEY_PROP, "");
        properties.forEach((key, value) -> {
            previous.put(key, Config.getStringProperty(key, null));
            Config.setProperty(key, value);
        });
        final String oldKey = System.getProperty("aws.accessKeyId");
        final String oldSecret = System.getProperty("aws.secretKey");
        System.setProperty("aws.accessKeyId", CREDENTIAL);
        System.setProperty("aws.secretKey", CREDENTIAL);
        AWSS3Storage configuredStorage = null;
        try {
            final var configured = AmazonS3StoragePersistenceAPIImpl.withPlainPaths();
            configuredStorage = S3StorageConfigurationTest.storage(configured);
            final var binaries = new BinaryAssetStorageAPIImpl(new ChainableStoragePersistenceAPI(
                    new JsonWriterDelegate(), List.of(fs, configured), mock(Chainable404StorageCache.class)));
            final File source = Files.writeString(root.resolve("source.txt"), "SDK credential chain").toFile();
            binaries.storeBinary("abc123", "MixedField", "Mixed.TXT", source);
            final File cached = binaries.getBinaryFile("abc123", "MixedField", "Mixed.TXT");
            assertTrue(binaries.evictLocalFile(cached));
            assertFalse(cached.exists());
            assertEquals("SDK credential chain", Files.readString(
                    binaries.getBinaryFile("abc123", "MixedField", "Mixed.TXT").toPath()));
            binaries.deleteBinary("abc123", "MixedField");
            assertTrue(client.listObjectsV2(bucket, GROUP + "/a/b/abc123/MixedField/").getObjectSummaries().isEmpty());
            assertNull(binaries.getBinaryFile("abc123", "MixedField", "Mixed.TXT"));
        } finally {
            if (configuredStorage != null) configuredStorage.shutdownTransferManager();
            previous.forEach(Config::setProperty);
            if (oldKey == null) System.clearProperty("aws.accessKeyId"); else System.setProperty("aws.accessKeyId", oldKey);
            if (oldSecret == null) System.clearProperty("aws.secretKey"); else System.setProperty("aws.secretKey", oldSecret);
        }
    }

    @Test
    void uploadEvictRetrieveCopyAndDeleteMixedCaseBinary() throws Exception {
        final File source = Files.writeString(root.resolve("temporary-upload.tmp"), "binary payload").toFile();
        final String path = "a/b/abc123/HeroImage/MyReport.PDF";
        api.storeBinary("abc123", "HeroImage", "MyReport.PDF", source);

        assertTrue(client.doesObjectExist(bucket, GROUP + "/" + path));
        assertEquals("binary payload", readStoredBinary(path));
        assertEquals(List.of("MyReport.PDF"), List.of(root.resolve("a/b/abc123/HeroImage").toFile().list()));
        assertTrue(api.evictLocalFile(root.resolve(path).toFile()));
        assertFalse(Files.exists(root.resolve(path)));
        assertTrue(api.existsBinary("abc123", "HeroImage"));

        final File restored = api.getBinaryFile("abc123", "HeroImage");
        assertTrue(Files.isSameFile(root.resolve(path), restored.toPath()));
        assertEquals("MyReport.PDF", restored.getName());
        assertEquals("binary payload", Files.readString(restored.toPath()));
        api.copyBinary("abc123", "de456", "HeroImage", "MyReport.PDF");
        assertTrue(client.doesObjectExist(bucket, GROUP + "/d/e/de456/HeroImage/MyReport.PDF"));

        // A remote-only sibling must be deleted even though the first file is cached.
        client.putObject(bucket, GROUP + "/a/b/abc123/HeroImage/nested/Second.TXT", "second");
        client.putObject(bucket, GROUP + "/a/b/abc123/HeroImageExtra/Keep.TXT", "keep");
        api.deleteBinary("abc123", "HeroImage");
        assertTrue(client.listObjectsV2(bucket, GROUP + "/a/b/abc123/HeroImage/").getObjectSummaries().isEmpty());
        assertFalse(Files.exists(root.resolve(path)));
        assertFalse(api.existsBinary("abc123", "HeroImage"));
        assertNull(api.getBinaryFile("abc123", "HeroImage", "MyReport.PDF"));
        assertNull(api.getBinaryFile("abc123", "HeroImage"));
        assertTrue(client.doesObjectExist(bucket, GROUP + "/a/b/abc123/HeroImageExtra/Keep.TXT"));
        api.deleteAllBinaries("abc123");
        api.deleteAllBinaries("de456");
        assertTrue(client.listObjectsV2(bucket, GROUP + "/a/b/abc123/").getObjectSummaries().isEmpty());
        assertTrue(client.listObjectsV2(bucket, GROUP + "/d/e/de456/").getObjectSummaries().isEmpty());
    }

    @Test
    void backfillIsIdempotentAndConcurrentWritersCannotReplaceEachOther() throws Exception {
        client.setBucketVersioningConfiguration(new com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest(
                bucket, new com.amazonaws.services.s3.model.BucketVersioningConfiguration("Enabled")));
        versioned = true;
        final var chain = new ChainableStoragePersistenceAPI(new JsonWriterDelegate(), List.of(fs, s3),
                mock(Chainable404StorageCache.class));
        final File first = Files.writeString(root.resolve("First.Txt"), "first candidate bytes").toFile();
        final File second = Files.writeString(root.resolve("Second.Txt"), "other candidate bytes").toFile();
        final String path = "a/b/abc123/MixedCase/Import.Txt";
        final var start = new java.util.concurrent.CountDownLatch(1);
        final java.util.function.Function<File, java.util.concurrent.CompletableFuture<Boolean>> upload = file ->
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();
                        return chain.backfillFile(GROUP, path, file);
                    } catch (com.dotmarketing.exception.DotDataException conflict) {
                        return false;
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new java.util.concurrent.CompletionException(interrupted);
                    }
                });
        final var firstResult = upload.apply(first);
        final var secondResult = upload.apply(second);
        start.countDown();
        final boolean firstWon = firstResult.get(30, java.util.concurrent.TimeUnit.SECONDS);
        final boolean secondWon = secondResult.get(30, java.util.concurrent.TimeUnit.SECONDS);
        assertNotEquals(firstWon, secondWon, "Exactly one distinct candidate may create the key");
        final File winner = firstWon ? first : second;
        final File loser = firstWon ? second : first;
        assertEquals(Files.readString(winner.toPath()), readStoredBinary(path));
        assertTrue(chain.backfillFile(GROUP, path, winner), "Retrying the same bytes must succeed");
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> chain.backfillFile(GROUP, path, loser));
        assertEquals(Files.readString(winner.toPath()), readStoredBinary(path));
        assertTrue(first.exists());
        assertTrue(second.exists());
        assertEquals(1, client.listVersions(bucket, GROUP + "/" + path).getVersionSummaries().size(),
                "The object history must contain only one successful PUT across races, retries and conflicts");
    }

    @Test
    void multipartBackfillCanBeVerifiedEvictedAndRestoredWithoutOverwrite() throws Exception {
        final String path = "a/b/abc123/fileAsset/Large.Bin";
        final Path source = root.resolve(path);
        Files.createDirectories(source.getParent());
        final byte[] block = new byte[1024 * 1024];
        java.util.Arrays.fill(block, (byte) 0x4d);
        try (final var output = Files.newOutputStream(source)) {
            for (int i = 0; i < 40; i++) {
                output.write(block);
            }
        }
        assertTrue(s3.backfillFile(GROUP, path, source.toFile()));
        final String key = S3ContentAddressedStorage.blobKey(client.getObjectMetadata(bucket, GROUP + "/" + path)
                .getUserMetaDataOf(S3ContentAddressedStorage.HASH_HEADER));
        final String etag = client.getObjectMetadata(bucket, key).getETag();
        assertTrue(etag.contains("-"), "Exercise actual multipart storage, not a single PUT");
        assertTrue(s3.backfillFile(GROUP, path, source.toFile()));
        assertTrue(api.evictLocalFile(source.toFile()), "A verified multipart copy permits safe eviction");
        final File restored = api.getBinaryFile("abc123", "fileAsset", "Large.Bin");
        assertEquals(40L * 1024 * 1024, restored.length());
        assertTrue(s3.hasDurableCopy(GROUP, path, restored));
        try (final var changed = new java.io.RandomAccessFile(restored, "rw")) {
            changed.write(0);
        }
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> s3.backfillFile(GROUP, path, restored));
        assertEquals(etag, client.getObjectMetadata(bucket, key).getETag());
        assertTrue(client.listMultipartUploads(new com.amazonaws.services.s3.model.ListMultipartUploadsRequest(bucket))
                .getMultipartUploads().isEmpty(), "A failed conditional completion must abort its multipart upload");
        assertFalse(api.evictLocalFile(restored), "A differing local file must be retained");
    }

    @Test
    void metadataBackfillIgnoresSerializationOrderButRejectsDifferentValues() throws Exception {
        final String path = "/a/b/abc123/fileAsset-metadata.json";
        final var writer = new JsonWriterDelegate();
        final var reader = new JsonReaderDelegate<>(Map.class);
        assertThrows(com.dotmarketing.exception.DotDataException.class,
                () -> s3.backfillObject("metadata", path, writer, reader, null));
        final java.util.LinkedHashMap<String, java.io.Serializable> first = new java.util.LinkedHashMap<>();
        first.put("length", 12L);
        first.put("dot:credit", "Original");
        assertTrue(s3.backfillObject("metadata", path, writer, reader, first));
        final java.util.LinkedHashMap<String, java.io.Serializable> reordered = new java.util.LinkedHashMap<>();
        reordered.put("dot:credit", "Original");
        reordered.put("length", 12);
        assertTrue(s3.backfillObject("metadata", path, writer, reader, reordered));
        reordered.put("dot:credit", "Conflicting");
        assertThrows(com.dotmarketing.exception.DotDataException.class,
                () -> s3.backfillObject("metadata", path, writer, reader, reordered));
        assertEquals("Original", ((Map<?, ?>) s3.pullObject("metadata", path, reader)).get("dot:credit"));
    }

    @Test
    void missingBucketIsNotReportedAsAMissingObject() throws Exception {
        final String absentBucket = bucket + "-missing";
        final var missing = new AmazonS3StoragePersistenceAPIImpl(storage, absentBucket,
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> missing.pullFile(GROUP, "Missing.Txt"));
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> missing.existsObject(GROUP, "Missing.Txt"));
        final File source = Files.writeString(root.resolve("Import.Txt"), "preserve me").toFile();
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> missing.backfillFile(GROUP, "Import.Txt", source));
        assertFalse(client.doesBucketExistV2(absentBucket));
        assertEquals("preserve me", Files.readString(source.toPath()));
    }

    @Test
    void evictionRetainsLegacyStaleAndPrefixOnlyCopies() throws Exception {
        final Path file = root.resolve("a/b/abc123/MixedCase/File.TXT");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "local");
        assertFalse(api.evictLocalFile(file.toFile()), "Legacy file has no durable copy");
        final String key = GROUP + "/a/b/abc123/MixedCase/File.TXT";
        client.putObject(bucket, key + ".backup", "local");
        assertFalse(api.evictLocalFile(file.toFile()), "A matching prefix is not the same object");
        client.putObject(bucket, key, "stale");
        assertFalse(api.evictLocalFile(file.toFile()), "Same size alone is not proof of a durable copy");
        client.putObject(bucket, key, "local");
        assertTrue(api.evictLocalFile(file.toFile()));
    }

    @Test
    void activeConsumerDefersEvictionWithoutBlockingOtherReaders() throws Exception {
        final Path upload = Files.writeString(root.resolve("source.txt"), "leased bytes");
        api.storeBinary("abc123", "HeroImage", "MixedCase.Txt", upload.toFile());
        final File binary = api.getBinaryFile("abc123", "HeroImage", "MixedCase.Txt");
        final var acquired = new java.util.concurrent.CountDownLatch(1);
        final var consume = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            final var consumer = executor.submit(() -> {
                try (var lease = api.acquireCacheLease()) {
                    acquired.countDown();
                    if (!consume.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                        throw new AssertionError("Consumer was not released");
                    }
                    return Files.readString(binary.toPath());
                }
            });
            try {
                assertTrue(acquired.await(10, java.util.concurrent.TimeUnit.SECONDS));
                assertFalse(api.evictLocalFile(binary), "Resolved file must survive until the consumer opens it");
                assertEquals("leased bytes", Files.readString(api.getBinaryFile("abc123", "HeroImage", "MixedCase.Txt").toPath()));
            } finally {
                consume.countDown();
            }
            assertEquals("leased bytes", consumer.get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertTrue(api.evictLocalFile(binary), "Closing the lease must permit verified eviction");
        assertEquals("leased bytes", Files.readString(api.getBinaryFile("abc123", "HeroImage", "MixedCase.Txt").toPath()));
    }

    @Test
    void physicalPathOpeningRestoresLegacyAndRevisionFiles() throws Exception {
        final File upload = Files.writeString(root.resolve("input.txt"), "template bytes").toFile();
        api.storeBinary("abc123", "MixedField", "Theme.CSS", upload);
        final File legacy = api.getBinaryFile("abc123", "MixedField", "Theme.CSS");
        final File revision = api.storeRevision("abc123", "MixedField", "Theme.CSS", upload);
        for (final File file : List.of(legacy, revision)) {
            assertTrue(api.evictLocalFile(file));
            try (var input = api.openLocalFile(file)) {
                assertTrue(api.evictLocalFile(file), "An opened stream keeps its bytes even after eviction");
                assertEquals("template bytes", new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        final File unrelated = root.resolve("server/ops/config/File.CSS").toFile();
        assertThrows(java.nio.file.NoSuchFileException.class, () -> api.openLocalFile(unrelated));
    }

    @Test
    void listingAndFieldDeletionIncludeEveryS3Page() throws Exception {
        final String prefix = GROUP + "/a/b/abc123/Files/";
        for (int i = 0; i < 1005; i++) {
            client.putObject(bucket, prefix + i + ".txt", "data");
        }
        assertEquals(1005, s3.listObjectPaths(GROUP, "a/b/abc123/Files").size());
        api.deleteBinary("abc123", "Files");
        assertTrue(client.listObjectsV2(bucket, prefix).getObjectSummaries().isEmpty());
    }

    public static class CountingResizeImageFilter extends com.dotmarketing.image.filter.ResizeImageFilter {
        static int generations;
        @Override
        protected String getFilterName() { return "resize"; }
        @Override
        public File runFilter(final File file, final Map<String, String[]> parameters) {
            generations++;
            return super.runFilter(file, parameters);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"PNG", "gif"})
    void generatedImageRoundTripsWithoutRegenerationAndInvalidatesRemoteOnlyCopies(final String extension) throws Exception {
        final String previousMode = Config.getStringProperty("BINARY_ASSET_STORAGE_TYPE", null);
        Config.setProperty("BINARY_ASSET_STORAGE_TYPE", "BINARY_CHAIN");
        final File source = root.resolve("Upload." + extension).toFile();
        final var image = new java.awt.image.BufferedImage(80, 60, java.awt.image.BufferedImage.TYPE_INT_RGB);
        javax.imageio.ImageIO.write(image, extension, source);
        api.storeBinary("abc123", "HeroImage", "Photo." + extension, source);
        final var imageAPI = mock(com.dotmarketing.image.filter.ImageFilterAPI.class);
        when(imageAPI.resolveFilters(any())).thenReturn(Map.of("resize", CountingResizeImageFilter.class));
        CountingResizeImageFilter.generations = 0;
        try (var locator = mockStatic(com.dotmarketing.business.APILocator.class);
             var engine = mockStatic(com.dotmarketing.image.ImageEngine.class)) {
            locator.when(com.dotmarketing.business.APILocator::getBinaryAssetStorageAPI).thenReturn(api);
            engine.when(com.dotmarketing.image.ImageEngine::resolve).thenReturn(imageAPI);
            final var exporter = new com.dotmarketing.portlets.contentlet.business.exporter.ImageFilterExporter();
            final Map<String, String[]> params = new java.util.HashMap<>();
            params.put("resize_w", new String[]{"32"});
            params.put("fieldVarName", new String[]{"HeroImage"});
            params.put("assetInodeOrIdentifier", new String[]{"abc123"});
            File original = api.getBinaryFile("abc123", "HeroImage", "Photo." + extension);
            final File rendition = exporter.exportContent(original, params).getDataFile();
            assertEquals(32, javax.imageio.ImageIO.read(rendition).getWidth());
            assertEquals(1, CountingResizeImageFilter.generations);
            final String key = BinaryAssetStorageAPI.GENERATED_ASSETS_GROUP + "/"
                    + root.resolve("dotGenerated").toRealPath().relativize(rendition.toPath());
            assertTrue(client.doesObjectExist(bucket, key));
            final byte[] expected = Files.readAllBytes(rendition.toPath());
            assertTrue(api.evictLocalFile(original));
            assertTrue(api.evictLocalFile(rendition));
            original = api.getBinaryFile("abc123", "HeroImage", "Photo." + extension);
            final File restored = exporter.exportContent(original, params).getDataFile();
            assertArrayEquals(expected, Files.readAllBytes(restored.toPath()));
            assertEquals(1, CountingResizeImageFilter.generations, "Cold rendition comes from S3, not pixel work");

            // Warm reads must not resurrect an object invalidated by another node.
            client.deleteObject(bucket, key);
            exporter.exportContent(original, params);
            assertFalse(client.doesObjectExist(bucket, key));
            assertEquals(1, CountingResizeImageFilter.generations);
            assertFalse(api.evictLocalFile(restored), "No durable copy: retain the local rendition");
            api.deleteGeneratedFiles("abc123");
            assertFalse(client.doesObjectExist(bucket, key));
            assertNull(api.getGeneratedFile(restored), "Invalidated renditions must not return from S3");
            exporter.exportContent(original, params);
            assertEquals(2, CountingResizeImageFilter.generations);
            api.deleteBinary("abc123", "HeroImage");
            assertFalse(client.doesObjectExist(bucket, key), "Deleting the source also invalidates S3 renditions");
        } finally {
            Config.setProperty("BINARY_ASSET_STORAGE_TYPE", previousMode);
        }
    }

    @Test
    void chainedCropPinsSnapshotFocalPointAndRestoresChangedCropFromS3() throws Exception {
        final File source = root.resolve("MixedCase.PNG").toFile();
        final var pixels = new java.awt.image.BufferedImage(80, 60, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 80; x++) {
            for (int y = 0; y < 60; y++) {
                pixels.setRGB(x, y, (x < 40 ? java.awt.Color.RED : java.awt.Color.BLUE).getRGB());
            }
        }
        javax.imageio.ImageIO.write(pixels, "PNG", source);
        final File original = api.storeRevision("abc123", "HeroImage", source.getName(), source);
        final var metadataAPI = mock(FileMetadataAPI.class);
        final var point = new java.util.concurrent.atomic.AtomicReference<String>("0.25,0.5");
        when(metadataAPI.getMetadata(any(), eq("HeroImage"))).thenAnswer(call -> {
            final com.dotmarketing.portlets.contentlet.model.Contentlet snapshot = call.getArgument(0);
            assertEquals("abc123", snapshot.getInode());
            assertEquals(original, snapshot.get("HeroImage"), "Use the requested revision, not the current DB version");
            final var metadata = mock(com.dotcms.storage.model.Metadata.class);
            when(metadata.getCustomMeta()).thenReturn(point.get() == null ? Map.of()
                    : Map.of("focalPoint", point.get()));
            return metadata;
        });
        final var imageAPI = mock(com.dotmarketing.image.filter.ImageFilterAPI.class);
        final Map<String, Class<? extends com.dotmarketing.image.filter.ImageFilter>> filters = new java.util.LinkedHashMap<>();
        filters.put("resize", CountingResizeImageFilter.class);
        filters.put("crop", com.dotmarketing.image.filter.CropImageFilter.class);
        when(imageAPI.resolveFilters(any())).thenReturn(filters);
        try (var locator = mockStatic(com.dotmarketing.business.APILocator.class);
             var engine = mockStatic(com.dotmarketing.image.ImageEngine.class)) {
            locator.when(com.dotmarketing.business.APILocator::getBinaryAssetStorageAPI).thenReturn(api);
            locator.when(com.dotmarketing.business.APILocator::getFileMetadataAPI).thenReturn(metadataAPI);
            engine.when(com.dotmarketing.image.ImageEngine::resolve).thenReturn(imageAPI);
            final var exporter = new com.dotmarketing.portlets.contentlet.business.exporter.ImageFilterExporter();
            final Map<String, String[]> params = new java.util.LinkedHashMap<>();
            params.put("resize_w", new String[]{"40"});
            params.put("crop_w", new String[]{"10"});
            params.put("crop_h", new String[]{"10"});
            params.put("fieldVarName", new String[]{"HeroImage"});
            params.put("assetInodeOrIdentifier", new String[]{"abc123"});
            final File red = exporter.exportContent(original, params).getDataFile();
            assertEquals(java.awt.Color.RED.getRGB(), javax.imageio.ImageIO.read(red).getRGB(5, 5));
            assertFalse(params.containsKey("fp"));
            assertFalse(params.containsKey(com.dotmarketing.image.filter.ImageFilter.RESOLVED_CROP_FOCAL_POINT));
            verify(metadataAPI, times(1)).getMetadata(any(), eq("HeroImage"));

            point.set("0.75,0.5");
            final File blue = exporter.exportContent(original, params).getDataFile();
            assertNotEquals(red, blue, "A changed focal point must select a different crop after resize");
            assertEquals(java.awt.Color.BLUE.getRGB(), javax.imageio.ImageIO.read(blue).getRGB(5, 5));
            final byte[] expected = Files.readAllBytes(blue.toPath());
            assertTrue(api.evictLocalFile(blue));
            assertArrayEquals(expected, Files.readAllBytes(exporter.exportContent(original, params).getDataFile().toPath()));
            verify(metadataAPI, times(3)).getMetadata(any(), eq("HeroImage"));

            params.put("fp", new String[]{"0.25,0.5"});
            assertEquals(red, exporter.exportContent(original, params).getDataFile(), "Explicit fp overrides stored metadata");
            verify(metadataAPI, times(3)).getMetadata(any(), eq("HeroImage"));
            params.remove("fp");
            point.set(null);
            final File noPoint = exporter.exportContent(original, params).getDataFile();
            assertEquals(java.awt.Color.RED.getRGB(), javax.imageio.ImageIO.read(noPoint).getRGB(5, 5));
            verify(metadataAPI, times(4)).getMetadata(any(), eq("HeroImage"));
            when(metadataAPI.getMetadata(any(), eq("HeroImage")))
                    .thenThrow(new com.dotmarketing.exception.DotDataException("metadata unavailable"));
            assertThrows(com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException.class,
                    () -> exporter.exportContent(original, params), "Do not cache a crop with an unknown focal point");
        }
    }

    @Test
    void generatedEvictionRetainsUnbackedFilesAndRejectsOutsidePaths() throws Exception {
        final Path rendition = root.resolve("dotGenerated/a/b/abc123/dotGenerated_resize_0123456789abcdef.PNG");
        Files.createDirectories(rendition.getParent());
        Files.writeString(rendition, "rendered image");
        assertFalse(api.evictLocalFile(rendition.toFile()));
        api.storeGeneratedFile(rendition.toFile());
        assertTrue(api.evictLocalFile(rendition.toFile()));
        assertEquals("rendered image", Files.readString(api.getGeneratedFile(rendition.toFile()).toPath()));
        final File outside = Files.writeString(root.resolve("dotGenerated2.png"), "outside").toFile();
        assertThrows(IllegalArgumentException.class, () -> api.storeGeneratedFile(outside));
        final File temporary = Files.writeString(rendition.resolveSibling("dotGenerated_resize_abc.png_123.png"), "partial").toFile();
        assertThrows(IllegalArgumentException.class, () -> api.storeGeneratedFile(temporary));
    }

    @Test
    void renditionInvalidationPreservesNeighboringAssetAndItsColdRead() throws Exception {
        final String filename = "dotGenerated_resize_0123456789abcdef.png";
        final Path target = root.resolve("dotGenerated/a/b/abc123/" + filename);
        final Path neighbor = root.resolve("dotGenerated/a/b/abc123extra/" + filename);
        Files.createDirectories(target.getParent());
        Files.createDirectories(neighbor.getParent());
        Files.writeString(target, "target pixels");
        Files.writeString(neighbor, "neighbor pixels");
        api.storeGeneratedFile(target.toFile());
        api.storeGeneratedFile(neighbor.toFile());
        final String group = BinaryAssetStorageAPI.GENERATED_ASSETS_GROUP;
        final String remoteOnly = group + "/a/b/abc123/dotGenerated_resize_1111.png";
        client.putObject(bucket, remoteOnly, "remote-only rendition");
        assertTrue(api.evictLocalFile(target.toFile()));
        api.deleteGeneratedFiles("abc123");
        assertNull(api.getGeneratedFile(target.toFile()));
        assertFalse(client.doesObjectExist(bucket, remoteOnly));
        assertEquals("neighbor pixels", Files.readString(neighbor));
        assertTrue(api.evictLocalFile(neighbor.toFile()));
        assertEquals("neighbor pixels", Files.readString(api.getGeneratedFile(neighbor.toFile()).toPath()));
    }

    @Test
    void metadataOutageDoesNotRegenerateAndRecoveryRecreatesTheMappedCache() throws Exception {
        final String group = "metadata";
        final String path = "/a/b/abc123/asset-metadata.json";
        final Path cacheRoot = Files.createDirectories(root.resolve("custom-cache"));
        fs.addGroupMapping(group, cacheRoot.toFile());
        final var chain = new ChainableStoragePersistenceAPI(new JsonWriterDelegate(), List.of(fs, s3),
                mock(Chainable404StorageCache.class));
        final var provider = mock(StoragePersistenceProvider.class);
        when(provider.getStorage(any())).thenReturn(chain);
        final var generator = mock(MetadataGenerator.class);
        final var metadata = new FileStorageAPIImpl(new JsonReaderDelegate<>(Map.class), new JsonWriterDelegate(),
                generator, provider, mock(com.dotmarketing.portlets.contentlet.business.MetadataCache.class));
        final var key = new StorageKey.Builder().group(group).path(path).storage(StorageType.DEFAULT_CHAIN).build();
        final var request = new FetchMetadataParams.Builder().cache(false).storageKey(key).build();
        metadata.setMetadata(request, Map.of("dot:focalPoint", "0.75,0.5"));
        org.apache.commons.io.FileUtils.deleteDirectory(cacheRoot.toFile());

        final AWSS3Storage unavailable = spy(storage);
        final var outage = new com.amazonaws.services.s3.model.AmazonS3Exception("injected read outage");
        outage.setStatusCode(503);
        doThrow(outage).when(unavailable).downloadFile(eq(bucket), anyString(), any(File.class));
        final var failingS3 = new AmazonS3StoragePersistenceAPIImpl(unavailable, bucket,
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        when(provider.getStorage(any())).thenReturn(new ChainableStoragePersistenceAPI(new JsonWriterDelegate(),
                List.of(fs, failingS3), mock(Chainable404StorageCache.class)));
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> metadata.retrieveMetaData(request));
        final var sourceReads = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.function.Supplier<File> source = () -> {
            sourceReads.incrementAndGet();
            return root.resolve("must-not-be-read.PNG").toFile();
        };
        final var config = new GenerateMetadataConfig.Builder().storageKey(key).build();
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> metadata.generateMetaData(source, config));
        assertEquals(0, sourceReads.get(), "An outage must not start metadata regeneration");
        verifyNoInteractions(generator);
        assertFalse(Files.exists(cacheRoot));
        when(provider.getStorage(any())).thenReturn(chain);
        assertEquals("0.75,0.5", metadata.retrieveMetaData(request).get("dot:focalPoint"));
        final Path local = cacheRoot.resolve(path.substring(1));
        assertTrue(Files.isRegularFile(local), "Restore must preserve the custom cache mapping");
        assertFalse(Files.exists(root.resolve(group)), "Restore must not silently relocate the cache");
        final var absent = new FetchMetadataParams.Builder().cache(false).storageKey(new StorageKey.Builder()
                .group(group).path("/missing.json").storage(StorageType.DEFAULT_CHAIN).build()).build();
        assertNull(metadata.retrieveMetaData(absent), "A real S3 404 is still normal absence");

        Files.writeString(local, "broken JSON");
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> metadata.retrieveMetaData(request));
        assertEquals("broken JSON", Files.readString(local), "Read errors must not delete evidence or regenerate metadata");
        Files.delete(local);
        assertEquals("0.75,0.5", metadata.retrieveMetaData(request).get("dot:focalPoint"));
        org.apache.commons.io.FileUtils.deleteDirectory(cacheRoot.toFile());
        assertEquals(local.toFile().getCanonicalFile(), chain.pullFile(group, path),
                "File restoration must also preserve a removed cache directory's mapping");
    }

    @Test
    void metadataReplacementSurvivesUploadFailureAndColdRead() throws Exception {
        final String group = "metadata";
        final String path = "/a/b/abc123/HeroImage-metadata.json";
        final var chain = new ChainableStoragePersistenceAPI(new JsonWriterDelegate(), List.of(fs, s3),
                mock(Chainable404StorageCache.class));
        final var provider = mock(StoragePersistenceProvider.class);
        when(provider.getStorage(any())).thenReturn(chain);
        final var metadata = new FileStorageAPIImpl(new JsonReaderDelegate<>(Map.class), new JsonWriterDelegate(),
                mock(MetadataGenerator.class), provider,
                mock(com.dotmarketing.portlets.contentlet.business.MetadataCache.class));
        final var request = new FetchMetadataParams.Builder().cache(false)
                .storageKey(new StorageKey.Builder().group(group).path(path).storage(StorageType.DEFAULT_CHAIN).build()).build();
        metadata.setMetadata(request, Map.of("dot:credit", "Original"));
        metadata.setMetadata(request, Map.of("dot:credit", "Replacement"));
        assertEquals("Replacement", metadata.retrieveMetaData(request).get("dot:credit"),
                "The local cache must receive the replacement without a pre-delete");

        final AWSS3Storage failingUpload = spy(storage);
        doThrow(new com.amazonaws.AmazonClientException("injected upload failure"))
                .when(failingUpload).uploadFile(any(com.amazonaws.services.s3.model.PutObjectRequest.class));
        final var failingS3 = new AmazonS3StoragePersistenceAPIImpl(failingUpload, bucket,
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        when(provider.getStorage(any())).thenReturn(new ChainableStoragePersistenceAPI(new JsonWriterDelegate(),
                List.of(fs, failingS3), mock(Chainable404StorageCache.class)));
        assertThrows(com.dotmarketing.exception.DotDataException.class,
                () -> metadata.setMetadata(request, Map.of("dot:credit", "Lost update")));
        when(provider.getStorage(any())).thenReturn(chain);
        assertEquals("Replacement", metadata.retrieveMetaData(request).get("dot:credit"));
        Files.delete(fs.pullFile(group, path).toPath());
        assertEquals("Replacement", metadata.retrieveMetaData(request).get("dot:credit"),
                "The prior remote value must survive the rejected upload");
        metadata.putCustomMetadataAttributes(request, Map.of("credit", "Recovered"));
        Files.delete(fs.pullFile(group, path).toPath());
        assertEquals("Recovered", metadata.retrieveMetaData(request).get("dot:credit"));
    }

    @Test
    void s3ObjectSerializationUsesIndependentStagingAndCleansUpFailures() throws Exception {
        final var staged = new java.util.concurrent.CopyOnWriteArrayList<File>();
        final var backing = spy(storage);
        doAnswer(call -> {
            final com.amazonaws.services.s3.model.PutObjectRequest request = call.getArgument(0);
            staged.add(request.getFile());
            return storage.uploadFile(request);
        }).when(backing).uploadFile(any(com.amazonaws.services.s3.model.PutObjectRequest.class));
        final var adapter = new AmazonS3StoragePersistenceAPIImpl(backing, bucket,
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        for (final String value : List.of("first", "second")) {
            adapter.pushObject("metadata", "/same.json", new JsonWriterDelegate(), value, Map.of());
        }
        assertEquals(2, staged.size());
        assertNotEquals(staged.get(0), staged.get(1), "Independent requests must not share a staging file");
        assertTrue(staged.stream().noneMatch(File::exists));
        assertEquals("second", adapter.pullObject("metadata", "/same.json", new JsonReaderDelegate<>(String.class)));
        assertThrows(com.dotmarketing.exception.DotDataException.class,
                () -> adapter.pushObject("metadata", "/same.json", (out, value) -> {
                    out.write(123);
                    throw new java.io.IOException("injected serialization failure");
                }, "partial", Map.of()));
        assertEquals("second", adapter.pullObject("metadata", "/same.json", new JsonReaderDelegate<>(String.class)));
    }

    @Test
    void slashPrefixedMetadataKeysStillRoundTripWithHashing() throws Exception {
        final AmazonS3StoragePersistenceAPIImpl metadata = new AmazonS3StoragePersistenceAPIImpl(
                storage, bucket, AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.SHA256);
        final File source = Files.writeString(root.resolve("upload.tmp"), "metadata").toFile();
        final String path = "/a/b/repeated.json/repeated.json";
        metadata.pushFile("metadata", path, source, Map.of());
        final String expectedKey = "metadata/" + org.apache.commons.codec.digest.DigestUtils
                .sha256Hex("a/b/repeated.json/") + "/repeated.json";
        assertTrue(client.doesObjectExist(bucket, expectedKey));
        assertEquals(List.of(path), metadata.listObjectPaths("metadata", "/a/b/repeated.json/"));
        assertEquals("metadata", Files.readString(metadata.pullFile("metadata", path).toPath()));
        metadata.deleteObjectReference("metadata", path);
        assertFalse(client.doesObjectExist(bucket, expectedKey));
    }
    private String readStoredBinary(String path) throws Exception {
        final File file = s3.pullFile(GROUP, path);
        try { return Files.readString(file.toPath()); }
        finally { s3.releaseRetrievedFile(file); }
    }

    @Test
    void identicalBytesShareOneBlobAcrossNamesOwnersAndRenditions() throws Exception {
        final File source = Files.writeString(root.resolve("shared-source"), "shared immutable bytes").toFile();
        final File first = api.storeRevision("abc123", "HeroImage", "Original.Txt", source);
        final File second = api.storeRevision("def456", "Attachment", "Different-Name.TXT", source);
        final String firstKey = com.dotcms.storage.binary.BinaryAssetReference.keyOf(first);
        final String secondKey = com.dotcms.storage.binary.BinaryAssetReference.keyOf(second);
        final String hash = S3ContentAddressedStorage.hash(source);
        final String blobKey = S3ContentAddressedStorage.blobKey(hash);
        assertEquals("asset-blobs/sha256/" + hash.substring(0, 2) + "/" + hash.substring(2, 4)
                + "/" + hash.substring(4, 6) + "/" + hash.substring(6, 8) + "/" + hash, blobKey);
        assertNotEquals(firstKey, secondKey);
        assertEquals(hash, client.getObjectMetadata(bucket, GROUP + "/" + firstKey)
                .getUserMetaDataOf(S3ContentAddressedStorage.HASH_HEADER));
        assertEquals(hash, client.getObjectMetadata(bucket, GROUP + "/" + secondKey)
                .getUserMetaDataOf(S3ContentAddressedStorage.HASH_HEADER));
        s3.pushFile(BinaryAssetStorageAPI.GENERATED_ASSETS_GROUP, "a/b/abc123/dotGenerated_same.txt", source, Map.of());
        assertEquals(1, client.listObjectsV2(bucket, S3ContentAddressedStorage.BLOB_PREFIX).getKeyCount());
        assertEquals("shared immutable bytes", client.getObjectAsString(bucket, blobKey));
        assertTrue(api.evictLocalFile(first));
        assertTrue(api.evictLocalFile(second));
        api.deleteBinary("abc123", "HeroImage");
        assertEquals("shared immutable bytes", Files.readString(api.getRevisionFile("def456", "Attachment", secondKey).toPath()));
        assertFalse(client.doesObjectExist(bucket, GROUP + "/" + firstKey));
        assertTrue(client.doesObjectExist(bucket, blobKey));
        api.deleteBinary("def456", "Attachment");
        assertTrue(client.doesObjectExist(bucket, blobKey), "Shared blobs are retained until reference-aware reclamation is implemented");
    }

    @Test
    void legacyOneLevelBlobsRemainReadableAndBackfillIntoFourLevels() throws Exception {
        final File source = Files.writeString(root.resolve("legacy-blob-source"), "legacy blob bytes").toFile();
        final File binary = api.storeRevision("abc123", "HeroImage", "Legacy.Txt", source);
        final String ownerKey = com.dotcms.storage.binary.BinaryAssetReference.keyOf(binary);
        final String hash = S3ContentAddressedStorage.hash(source);
        final String key = S3ContentAddressedStorage.blobKey(hash);
        final String legacyKey = "asset-blobs/sha256/" + hash.substring(0, 2) + "/" + hash;
        client.copyObject(bucket, key, bucket, legacyKey);
        client.deleteObject(bucket, key);
        assertTrue(api.evictLocalFile(binary));
        assertEquals("legacy blob bytes", Files.readString(api.getRevisionFile("abc123", "HeroImage", ownerKey).toPath()));
        assertFalse(client.doesObjectExist(bucket, key), "Reading must not migrate blobs");
        client.putObject(bucket, key, "corrupt new blob");
        assertFalse(api.evictLocalFile(binary), "A valid legacy blob must not hide corruption at the new key");
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> s3.pullFile(GROUP, ownerKey));
        client.deleteObject(bucket, key);
        assertTrue(s3.backfillFile(GROUP, ownerKey, source));
        assertEquals("legacy blob bytes", client.getObjectAsString(bucket, key));
        assertTrue(client.doesObjectExist(bucket, legacyKey), "Backfill must preserve existing blobs");
        client.deleteObject(bucket, key);
        client.deleteObject(bucket, legacyKey);
        assertFalse(api.evictLocalFile(binary));
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> s3.pullFile(GROUP, ownerKey));
    }

    @Test
    void missingOrCorruptSharedBlobCannotPermitEvictionOrReturnPartialBytes() throws Exception {
        final File source = Files.writeString(root.resolve("integrity-source"), "original bytes").toFile();
        final File binary = api.storeRevision("abc123", "HeroImage", "Mixed.Txt", source);
        final String ownerKey = com.dotcms.storage.binary.BinaryAssetReference.keyOf(binary);
        final String blobKey = S3ContentAddressedStorage.blobKey(S3ContentAddressedStorage.hash(source));
        client.putObject(bucket, blobKey, "tampered bytes");
        assertFalse(api.evictLocalFile(binary));
        assertEquals("original bytes", Files.readString(binary.toPath()));
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> s3.pullFile(GROUP, ownerKey));
        client.deleteObject(bucket, blobKey);
        assertFalse(api.evictLocalFile(binary));
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> s3.pullFile(GROUP, ownerKey));
        assertTrue(Files.isRegularFile(binary.toPath()));
    }

    @Test
    void extractedMetadataIsSharedByBytesAndVersionWithoutSharingEditorialChanges() throws Exception {
        final var cache = new SharedExtractedMetadata(s3);
        final File first = Files.writeString(root.resolve("First-Name.Txt"), "same extracted bytes").toFile();
        final File second = Files.writeString(root.resolve("Another-Name.txt"), "same extracted bytes").toFile();
        final var count = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.function.Function<File, Map<String, java.io.Serializable>> extract = file -> {
            count.incrementAndGet();
            return Map.of("content", "extracted text", "width", 123);
        };
        final var firstMetadata = cache.get(first, "tika-test-1", 1, 1000, extract);
        firstMetadata.put("credit", "Editorial credit for the first use");
        final var secondMetadata = cache.get(second, "tika-test-1", 1, 1000, extract);
        assertEquals(1, count.get(), "A different filename must reuse completed extraction");
        assertEquals("extracted text", secondMetadata.get("content"));
        assertFalse(secondMetadata.containsKey("credit"));
        cache.get(second, "tika-test-2", 1, 1000, extract);
        cache.get(second, "tika-test-2", 1, 2000, extract);
        cache.get(second, "tika-test-2", 2, 2000, extract);
        assertEquals(4, count.get(), "Parser version, schema and text limit must distinguish cached extractions");
        assertEquals(4, client.listObjectsV2(bucket, SharedExtractedMetadata.GROUP + "/").getObjectSummaries().stream()
                .filter(object -> !object.getKey().endsWith("/")).count());
        Config.setProperty(AssetStorageFeature.FLAG, false);
        final var offline = new SharedExtractedMetadata(mock(AmazonS3StoragePersistenceAPIImpl.class));
        offline.get(first, "unused", 1, 1, extract);
        assertEquals(5, count.get(), "Disabled mode continues to extract directly");
    }

    @Test
    void rawAssetsAndRenditionsMigrateToOneBlobWithoutChangingTheirKeys() throws Exception {
        final File source = Files.writeString(root.resolve("legacy-source"), "legacy shared bytes").toFile();
        final String path = "a/b/abc123/HeroImage/Legacy-Mixed.Txt";
        client.putObject(bucket, GROUP + "/" + path, source);
        assertEquals("legacy shared bytes", readStoredBinary(path));
        assertTrue(s3.backfillFile(GROUP, path, source));
        final String hash = S3ContentAddressedStorage.hash(source);
        assertEquals(hash, client.getObjectMetadata(bucket, GROUP + "/" + path)
                .getUserMetaDataOf(S3ContentAddressedStorage.HASH_HEADER));
        final String version = client.getObjectMetadata(bucket, GROUP + "/" + path).getETag();
        assertTrue(s3.backfillFile(GROUP, path, source));
        assertEquals(version, client.getObjectMetadata(bucket, GROUP + "/" + path).getETag());
        final String rendition = "a/b/abc123/dotGenerated_resize_ab12.png";
        client.putObject(bucket, BinaryAssetStorageAPI.GENERATED_ASSETS_GROUP + "/" + rendition, source);
        api.backfillGeneratedFiles("abc123");
        assertEquals(hash, client.getObjectMetadata(bucket, BinaryAssetStorageAPI.GENERATED_ASSETS_GROUP + "/" + rendition)
                .getUserMetaDataOf(S3ContentAddressedStorage.HASH_HEADER));
        assertEquals(1, client.listObjectsV2(bucket, S3ContentAddressedStorage.BLOB_PREFIX).getKeyCount());
        final File local = api.getGeneratedFile(root.resolve("dotGenerated").resolve(rendition).toFile());
        assertTrue(api.evictLocalFile(local));
        assertEquals("legacy shared bytes", Files.readString(api.getGeneratedFile(local).toPath()));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"replace", "delete"})
    void migrationCannotOverwriteOrResurrectAConcurrentOwnerChange(String change) throws Exception {
        final File source = Files.writeString(root.resolve("migration-source"), "original raw bytes").toFile();
        final String path = "a/b/abc123/HeroImage/Legacy.Txt";
        final String owner = GROUP + "/" + path;
        client.putObject(bucket, owner, source);
        final var racingStorage = spy(storage);
        final var raced = new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(call -> {
            final com.amazonaws.services.s3.model.PutObjectRequest request = call.getArgument(0);
            if (request.getKey().equals(owner) && raced.compareAndSet(false, true)) {
                if (change.equals("replace")) client.putObject(bucket, owner, "newer raw bytes");
                else client.deleteObject(bucket, owner);
            }
            return call.callRealMethod();
        }).when(racingStorage).uploadFile(any(com.amazonaws.services.s3.model.PutObjectRequest.class));
        final var migrating = new AmazonS3StoragePersistenceAPIImpl(racingStorage, bucket,
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        assertThrows(com.dotmarketing.exception.DotDataException.class, () -> migrating.backfillFile(GROUP, path, source));
        assertTrue(raced.get(), "Exercise a real conditional update against MinIO");
        assertEquals("original raw bytes", Files.readString(source.toPath()));
        if (change.equals("replace")) assertEquals("newer raw bytes", client.getObjectAsString(bucket, owner));
        else assertFalse(client.doesObjectExist(bucket, owner));
    }
}

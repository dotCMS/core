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

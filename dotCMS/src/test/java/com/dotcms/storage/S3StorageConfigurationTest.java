package com.dotcms.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.dotcms.enterprise.publishing.storage.AWSS3Storage;
import com.dotcms.enterprise.publishing.storage.Storage;
import com.dotcms.storage.repository.LocalFileRepositoryManager;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class S3StorageConfigurationTest {
    private final Map<String, String> previous = new HashMap<>();

    @BeforeEach
    void configure() {
        for (String key : new String[] {AssetStorageFeature.FLAG,
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_ACCESS_KEY_PROP,
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_SECRET_ACCESS_KEY_PROP,
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_REGION_PROP,
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_NAMESPACE_PROP,
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_ENDPOINT_PROP}) {
            previous.put(key, Config.getStringProperty(key, null));
            Config.setProperty(key, null);
        }
        Config.setProperty(AssetStorageFeature.FLAG, true);
        Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_REGION_PROP, "eu-west-1");
    }

    @AfterEach
    void restore() { previous.forEach(Config::setProperty); }

    @Test
    void defaultCredentialsRetainRegionAndEndpointAndDisabledConfigurationStaysLegacy() throws Exception {
        var provider = new AmazonS3StoragePersistenceAPIImpl();
        var storage = storage(provider);
        try {
            assertEquals("eu-west-1", client(storage).getRegionName());
            assertTrue(client(storage).getUrl("test-bucket", "file").getHost().contains("eu-west-1"));
        } finally { storage.shutdownTransferManager(); }

        Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_ENDPOINT_PROP, "http://127.0.0.1:19077");
        storage = storage(new AmazonS3StoragePersistenceAPIImpl());
        try {
            assertEquals("eu-west-1", client(storage).getRegionName());
            assertEquals("127.0.0.1", client(storage).getUrl("test-bucket", "file").getHost());
            assertEquals(19077, client(storage).getUrl("test-bucket", "file").getPort());
        } finally { storage.shutdownTransferManager(); }

        Config.setProperty(AssetStorageFeature.FLAG, false);
        storage = storage(new AmazonS3StoragePersistenceAPIImpl());
        try {
            assertEquals("test-bucket.s3.amazonaws.com", client(storage).getUrl("test-bucket", "file").getHost());
        } finally { storage.shutdownTransferManager(); }
    }

    @Test
    void partialExplicitCredentialsFailBeforeFallingBackToIam() throws Exception {
        for (String key : new String[] {AmazonS3StoragePersistenceAPIImpl.AWS_S3_ACCESS_KEY_PROP,
                AmazonS3StoragePersistenceAPIImpl.AWS_S3_SECRET_ACCESS_KEY_PROP}) {
            Config.setProperty(key, "incomplete-credential");
            assertThrows(DotRuntimeException.class, AmazonS3StoragePersistenceAPIImpl::new);
            Config.setProperty(key, null);
        }
        Config.setProperty(AssetStorageFeature.FLAG, false);
        Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_ACCESS_KEY_PROP, "legacy-partial-key");
        final var storage = storage(new AmazonS3StoragePersistenceAPIImpl());
        try { assertEquals("test-bucket.s3.amazonaws.com", client(storage).getUrl("test-bucket", "file").getHost()); }
        finally { storage.shutdownTransferManager(); }
    }

    @Test
    void customEndpointNeedsAValidUrlAndSigningRegion() throws Exception {
        for (String endpoint : new String[] {"not a URL", "ftp://127.0.0.1", "http:///missing-host"}) {
            Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_ENDPOINT_PROP, endpoint);
            assertThrows(DotRuntimeException.class, AmazonS3StoragePersistenceAPIImpl::new);
        }
        Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_ENDPOINT_PROP, "http://127.0.0.1:19077");
        Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_REGION_PROP, null);
        assertThrows(DotRuntimeException.class, AmazonS3StoragePersistenceAPIImpl::new);

        Config.setProperty(AssetStorageFeature.FLAG, false);
        Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_ENDPOINT_PROP, "not a URL");
        final var storage = storage(new AmazonS3StoragePersistenceAPIImpl());
        try { assertEquals("test-bucket.s3.amazonaws.com", client(storage).getUrl("test-bucket", "file").getHost()); }
        finally { storage.shutdownTransferManager(); }
    }

    @Test
    void namespaceValidationIsGatedAndRunsBeforeConnecting() throws Exception {
        for (String namespace : new String[] {"../other", "/absolute", "nested/path", "a.b", "a b", "x".repeat(65)}) {
            Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_NAMESPACE_PROP, namespace);
            assertThrows(DotRuntimeException.class, AmazonS3StoragePersistenceAPIImpl::new, namespace);
        }
        Config.setProperty(AssetStorageFeature.FLAG, false);
        final var storage = storage(new AmazonS3StoragePersistenceAPIImpl());
        try { assertEquals("test-bucket.s3.amazonaws.com", client(storage).getUrl("test-bucket", "file").getHost()); }
        finally { storage.shutdownTransferManager(); }
    }

    @Test
    void publishingEndpointUsesItsOwnConfiguration() throws Exception {
        Config.setProperty(AmazonS3StoragePersistenceAPIImpl.AWS_S3_ENDPOINT_PROP, "http://127.0.0.1:19077");
        final var storage = new AWSS3Storage(new AWSS3Configuration.Builder()
                .accessKey("publisher-key").secretKey("publisher-secret")
                .region("ap-southeast-2").endPoint("http://127.0.0.1:19078").build());
        try {
            assertEquals("ap-southeast-2", client(storage).getRegionName());
            assertEquals(19078, client(storage).getUrl("test-bucket", "file").getPort());
        } finally { storage.shutdownTransferManager(); }
    }

    @Test
    void staticPushClientsWithoutARegionUseTheGlobalEndpointWithRegionLookup() throws Exception {
        // Keys without a region or endpoint used to fail with "Unable to find a region", and the
        // credential-chain path was pinned to us-west-2. Both now use the global endpoint, with no
        // signer override, so the SDK can look up each bucket's region and sign SigV4 for it.
        var keysOnly = new AWSS3Storage(new AWSS3Configuration.Builder()
                .accessKey("publisher-key").secretKey("publisher-secret").build());
        var credentialChain = new AWSS3Storage(NoWebIdentityCredentialsProviderChain.defaultChain());
        try {
            for (var storage : List.of(keysOnly, credentialChain)) {
                assertEquals("test-bucket.s3.amazonaws.com", client(storage).getUrl("test-bucket", "file").getHost());
                assertNull(signerOverride(storage));
            }
        } finally {
            keysOnly.shutdownTransferManager();
            credentialChain.shutdownTransferManager();
        }

        Config.setProperty(AssetStorageFeature.FLAG, false);
        var legacy = new AWSS3Storage(NoWebIdentityCredentialsProviderChain.defaultChain());
        try {
            assertEquals("test-bucket.s3.amazonaws.com", client(legacy).getUrl("test-bucket", "file").getHost());
            assertEquals("S3SignerType", signerOverride(legacy));
        } finally { legacy.shutdownTransferManager(); }
    }

    @Test
    void enabledUploadsDoNotHoldTheSharedIdentifierLock(@TempDir Path root) throws Exception {
        // The identifier lock is shared with content saves; an upload must not make them time out.
        final var storage = mock(Storage.class);
        final var upload = mock(Upload.class);
        final var result = new UploadResult();
        result.setETag("etag");
        when(storage.uploadFile(any(PutObjectRequest.class))).thenReturn(upload);
        final var provider = new AmazonS3StoragePersistenceAPIImpl(storage, "bucket",
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        final File file = Files.writeString(root.resolve("upload.tmp"), "contents").toFile();
        try (var executor = Executors.newSingleThreadExecutor()) {
            when(upload.waitForUploadResult()).thenAnswer(call -> executor.submit(() -> {
                try {
                    return DotConcurrentFactory.getInstance().getIdentifierStripedLock()
                            .tryLock("s3_dotmetadata/a/b/file", () -> result);
                } catch (Throwable lockTimeout) {
                    throw new Exception(lockTimeout);
                }
            }).get(10, TimeUnit.SECONDS));
            assertEquals("etag", provider.pushFile("dotmetadata", "/a/b/file", file, Map.of()));
        }
    }

    @Test
    void enabledDownloadsUseTheirOwnFileWhateverTheRepositoryAndAreDeletedOnRelease(@TempDir Path root) throws Exception {
        final var storage = mock(Storage.class);
        when(storage.downloadFile(eq("bucket"), eq("dotmetadata/a/b/file"), any(File.class))).thenAnswer(call -> {
            Files.writeString(call.<File>getArgument(2).toPath(), "remote");
            return mock(Download.class);
        });
        final var provider = new AmazonS3StoragePersistenceAPIImpl(storage, "bucket",
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);
        // S3_STORAGE_FILE_REPO_TYPE=LOCAL maps every download of a key to one fixed path.
        final Path repository = root.resolve("local-repo");
        Files.createDirectories(repository.resolve("a/b"));
        final var field = AmazonS3StoragePersistenceAPIImpl.class.getDeclaredField("fileRepositoryManager");
        field.setAccessible(true);
        field.set(provider, new LocalFileRepositoryManager(repository.toString()) { });

        final File first = provider.pullFile("dotmetadata", "/a/b/file");
        final File second = provider.pullFile("dotmetadata", "/a/b/file");
        assertNotEquals(first, second, "Concurrent downloads of one key must not share a file");
        assertEquals("remote", Files.readString(first.toPath()));
        assertEquals("remote", Files.readString(second.toPath()));
        provider.releaseRetrievedFile(first);
        provider.releaseRetrievedFile(second);
        assertFalse(first.exists());
        assertFalse(second.exists());
        try (var files = Files.walk(repository)) {
            assertEquals(0, files.filter(Files::isRegularFile).count(), "Downloads are not kept in the local repository");
        }
    }

    @Test
    void backfillTreatsAConflictingConcurrentWriteLikeAnExistingObject(@TempDir Path root) throws Exception {
        // AWS answers 409 ConditionalRequestConflict, not 412, when two conditional writes race.
        final var conflict = new AmazonS3Exception("ConditionalRequestConflict");
        conflict.setStatusCode(409);
        final var storage = mock(Storage.class);
        doThrow(conflict).when(storage).uploadFileIfAbsent(eq("bucket"), anyString(), any(File.class));
        final var provider = new AmazonS3StoragePersistenceAPIImpl(storage, "bucket",
                AmazonS3StoragePersistenceAPIImpl.PathEncryptionMode.NONE);

        final File file = Files.writeString(root.resolve("source"), "contents").toFile();
        final var written = new S3ObjectSummary();
        written.setKey("dotmetadata/a/b/file");
        written.setSize(file.length());
        written.setETag(DigestUtils.md5Hex("contents"));
        when(storage.listFirstObject("bucket", "dotmetadata/a/b/file")).thenReturn(null, written);
        assertTrue(provider.backfillFile("dotmetadata", "/a/b/file", file));

        final var missing = new AmazonS3Exception("missing");
        missing.setStatusCode(404);
        missing.setErrorCode("NoSuchKey");
        when(storage.downloadFile(eq("bucket"), eq("dotmetadata/a/b/meta.json"), any(File.class)))
                .thenThrow(missing)
                .thenAnswer(call -> {
                    Files.writeString(call.<File>getArgument(2).toPath(), "\"value\"");
                    return mock(Download.class);
                });
        assertTrue(provider.backfillObject("dotmetadata", "/a/b/meta.json", new JsonWriterDelegate(),
                new JsonReaderDelegate<>(String.class), "value"));
    }

    private static String signerOverride(AWSS3Storage storage) throws Exception {
        return ((com.amazonaws.AmazonWebServiceClient) client(storage)).getClientConfiguration().getSignerOverride();
    }

    // Inspect the real SDK configuration without making network requests or exposing production getters.
    static AWSS3Storage storage(AmazonS3StoragePersistenceAPIImpl provider) throws Exception {
        final var field = AmazonS3StoragePersistenceAPIImpl.class.getDeclaredField("storage");
        field.setAccessible(true);
        return (AWSS3Storage) field.get(provider);
    }

    private static AmazonS3 client(AWSS3Storage storage) throws Exception {
        final var field = AWSS3Storage.class.getDeclaredField("s3client");
        field.setAccessible(true);
        return (AmazonS3) field.get(storage);
    }
}

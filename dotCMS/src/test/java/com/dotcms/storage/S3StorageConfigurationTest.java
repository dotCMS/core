package com.dotcms.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.dotcms.enterprise.publishing.storage.AWSS3Storage;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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

package com.dotcms.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.dotcms.enterprise.publishing.storage.AWSS3Storage;
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

    private static final String CREDENTIAL = "binary-storage-test";
    @TempDir Path root;
    private AmazonS3 client;
    private AWSS3Storage storage;
    private AmazonS3StoragePersistenceAPIImpl s3;
    private FileSystemStoragePersistenceAPIImpl fs;
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
}

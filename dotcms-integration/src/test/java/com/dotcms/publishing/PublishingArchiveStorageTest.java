package com.dotcms.publishing;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.manifest.CSVManifestBuilder;
import com.dotcms.publishing.manifest.ManifestReaderFactory;
import com.dotcms.publishing.manifest.ManifestUtil;
import com.dotcms.publishing.output.BundleArchiveCleanupProcessor;
import com.dotcms.publishing.output.BundleArchiveStorage;
import com.dotcms.publishing.output.TarGzipBundleOutput;
import com.dotcms.storage.AmazonS3StoragePersistenceAPIImpl;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PublishingArchiveStorageTest {
    @BeforeAll
    static void initialize() throws Exception {
        IntegrationTestInitService.getInstance().init();
        assertEquals(Boolean.getBoolean("s3.cms.enabled"), AssetStorageFeature.isEnabled());
    }

    @Test
    void publicBundleGenerationRestoresManifestAndPayloadAndCleansAfterCommit(@TempDir Path temporary) throws Exception {
        final var folder = new FolderDataGen().nextPersisted();
        final var bundleAPI = APILocator.getBundleAPI();
        final var user = APILocator.systemUser();
        final var archives = BundleArchiveStorage.getInstance();
        final Bundle bundle = new Bundle("S3 archive acceptance", null, null, user.getUserId());
        bundle.setId("Mix-" + UUID.randomUUID().toString().replace("-", ""));
        bundle.setOperation(PublisherConfig.Operation.PUBLISH.ordinal());
        final String filterKey = "archive-" + bundle.getId() + ".yml";
        APILocator.getPublisherAPI().addFilterDescriptor(new FilterDescriptor(filterKey,
                "Archive acceptance", java.util.Map.of("dependencies", true, "relationships", true), false, ""));
        bundle.setFilterKey(filterKey);
        bundleAPI.saveBundle(bundle);
        try {
            final byte[] payload = "Mixed-case publishing payload from S3".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            final File upload = Files.write(temporary.resolve("ArchivePayload.TXT"), payload).toFile();
            final var content = new FileAssetDataGen(folder, upload).nextPersisted();
            APILocator.getContentletAPI().publish(content, user, false);
            com.dotcms.publisher.business.PublisherAPI.getInstance().saveBundleAssets(
                    List.of(content.getIdentifier()), bundle.getId(), user);
            if (AssetStorageFeature.isEnabled()) {
                assertTrue(APILocator.getBinaryAssetStorageAPI().evictLocalFile(content.getBinary(FileAssetAPI.BINARY_FIELD)));
            }
            final File archive = bundleAPI.generateTarGzipBundleFile(bundle);
            final byte[] complete = Files.readAllBytes(archive.toPath());
            assertTrue(complete.length > payload.length);
            assertPayload(archive, payload);

            if (AssetStorageFeature.isEnabled()) {
                assertRemoteCopyAndClearLocal(bundle.getId(), archive, complete);
            }
            assertTrue(bundle.bundleTgzExists());
            if (AssetStorageFeature.isEnabled()) {
                assertFalse(archive.exists(), "Listing a bundle must not download its archive");
            }
            assertTrue(ManifestUtil.manifestExists(bundle.getId()));
            final var manifest = ManifestReaderFactory.INSTANCE.createCSVManifestReader(bundle.getId());
            assertEquals("PUBLISH", manifest.getMetadata(CSVManifestBuilder.OPERATION_METADATA_NAME));
            assertTrue(manifest.getIncludedAssets().stream().anyMatch(asset -> asset.id().equals(content.getIdentifier())));
            assertArrayEquals(complete, Files.readAllBytes(TarGzipBundleOutput.getBundleTarGzipFile(bundle.getId()).toPath()));

            if (AssetStorageFeature.isEnabled()) {
                final var config = new PublisherConfig();
                config.setId(bundle.getId());
                assertThrows(IOException.class, () -> {
                    try (var output = new TarGzipBundleOutput(config)) {
                        output.create();
                        try (var part = output.addFile("incomplete.txt")) { part.write(payload); }
                        throw new IOException("Bundler failed before completion");
                    }
                });
                assertArrayEquals(complete, Files.readAllBytes(archive.toPath()));
                assertRemoteCopyAndClearLocal(bundle.getId(), archive, complete);
                try (var manifestInput = ManifestUtil.getManifestInputStream(archive).orElseThrow()) {
                    assertTrue(org.apache.commons.io.IOUtils.toString(manifestInput).contains("PUBLISH"));
                }
                assertPayload(archive, payload);

                HibernateUtil.startTransaction();
                try {
                    bundleAPI.deleteBundleAndDependencies(bundle.getId(), user);
                    assertFalse(cleanupJobs(bundle.getId()).isEmpty());
                    assertArrayEquals(complete, Files.readAllBytes(archives.get(bundle.getId()).toPath()));
                } finally {
                    HibernateUtil.rollbackTransaction();
                }
                assertNotNull(bundleAPI.getBundleById(bundle.getId()));
                assertTrue(cleanupJobs(bundle.getId()).isEmpty());
                bundleAPI.deleteBundleAndDependencies(bundle.getId(), user);
                final var jobs = cleanupJobs(bundle.getId());
                assertFalse(jobs.isEmpty());
                final var job = APILocator.getJobQueueManagerAPI().getJob(jobs.getFirst().get("id").toString());
                final var failedProvider = mock(StoragePersistenceAPI.class);
                when(failedProvider.deleteObjectAndReferences(anyString(), anyString())).thenThrow(new DotDataException("S3 unavailable"));
                final var failedStorage = new BundleArchiveStorage(failedProvider);
                try (var storage = mockStatic(BundleArchiveStorage.class, CALLS_REAL_METHODS)) {
                    storage.when(BundleArchiveStorage::getInstance).thenReturn(failedStorage);
                    assertThrows(com.dotcms.jobs.business.error.JobProcessingException.class,
                            () -> new BundleArchiveCleanupProcessor().process(job));
                }
                assertArrayEquals(complete, Files.readAllBytes(archive.toPath()));
                new BundleArchiveCleanupProcessor().process(job);
                new BundleArchiveCleanupProcessor().process(job);
                assertFalse(archives.get(bundle.getId()).exists());
                assertFalse(ManifestUtil.manifestExists(bundle.getId()));
            } else {
                assertTrue(cleanupJobs(bundle.getId()).isEmpty());
                bundleAPI.deleteBundleAndDependencies(bundle.getId(), user);
                assertTrue(cleanupJobs(bundle.getId()).isEmpty());
                assertArrayEquals(complete, Files.readAllBytes(archive.toPath()), "Disabled deletion retains main's archive behavior");
            }
        } finally {
            if (bundleAPI.getBundleById(bundle.getId()) != null) {
                bundleAPI.deleteBundleAndDependencies(bundle.getId(), user);
            }
            if (AssetStorageFeature.isEnabled()) archives.delete(bundle.getId());
            else Files.deleteIfExists(BundleArchiveStorage.localArchive(bundle.getId()).toPath());
            FolderDataGen.remove(folder);
        }
    }

    private static List<java.util.Map<String, Object>> cleanupJobs(String id) throws DotDataException {
        return new DotConnect().setSQL("select id from job where queue_name = ? and parameters ->> 'bundleId' = ?")
                .addParam(BundleArchiveCleanupProcessor.QUEUE).addParam(id).loadObjectResults();
    }

    private static void assertRemoteCopyAndClearLocal(String id, File local, byte[] expected) throws Exception {
        final var remote = AmazonS3StoragePersistenceAPIImpl.withPlainPaths();
        final File copy = remote.pullFile(BundleArchiveStorage.GROUP, id + ".tar.gz");
        assertNotNull(copy);
        try {
            assertArrayEquals(expected, Files.readAllBytes(copy.toPath()));
        } finally {
            remote.releaseRetrievedFile(copy);
        }
        Files.delete(local.toPath());
    }

    private static void assertPayload(File archive, byte[] expected) throws IOException {
        boolean found = false;
        try (var input = new TarArchiveInputStream(new GzipCompressorInputStream(Files.newInputStream(archive.toPath())))) {
            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entry.isFile() && entry.getName().endsWith("/ArchivePayload.TXT")) {
                    assertArrayEquals(expected, input.readAllBytes());
                    found = true;
                }
            }
        }
        assertTrue(found, "The real content bundler must include the cold binary payload");
    }
}

package com.dotcms.storage.binary;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.FileMetadataAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BinaryAssetCleanupProcessorTest {
    @TempDir Path root;
    private String previousFlag;
    private static final String INODE = "abc123";
    private static final String OLD_REVISION =
            "a/b/abc123/heroImage/.revisions/11111111-1111-1111-1111-111111111111/old.png";
    private static final String NEW_REVISION =
            "a/b/abc123/heroImage/.revisions/22222222-2222-2222-2222-222222222222/new.png";
    private static final String OLD_METADATA = "/" + OLD_REVISION + FileMetadataAPI.METADATA_JSON;

    @BeforeEach void enable() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
    }

    @AfterEach void restore() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    private Job job(final Map<String, Object> parameters) {
        Job job = mock(Job.class);
        when(job.id()).thenReturn("cleanup-test");
        when(job.parameters()).thenReturn(com.google.common.collect.ImmutableMap.copyOf(parameters));
        return job;
    }

    private Job job() {
        return job(Map.of("inode", INODE, "binaries", List.of(OLD_REVISION), "metadata", List.of(OLD_METADATA)));
    }

    @Test void enqueueRequiresTransactionAndPropagatesJournalFailure() throws Exception {
        var queue = mock(JobQueueManagerAPI.class);
        var storage = mock(BinaryAssetStorageAPI.class);
        try (var locator = mockStatic(APILocator.class);
             var connection = mockStatic(DbConnectionFactory.class)) {
            locator.when(APILocator::getJobQueueManagerAPI).thenReturn(queue);
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(mock(FileMetadataAPI.class));
            assertThrows(DotDataException.class, () -> BinaryAssetCleanupProcessor.enqueue(INODE));
            verifyNoInteractions(queue, storage);
            connection.when(DbConnectionFactory::inTransaction).thenReturn(true);
            when(queue.createJob(eq(BinaryAssetCleanupProcessor.QUEUE), anyMap()))
                    .thenThrow(new DotDataException("database unavailable"));
            assertThrows(DotDataException.class, () -> BinaryAssetCleanupProcessor.enqueue(INODE));
            verify(storage, never()).deleteBinaryPaths(anyString(), anyString(), anyList());
        }
    }

    @Test void pathsListedBeforeTheTransactionAreRecordedWithoutListingAgain() throws Exception {
        var queue = mock(JobQueueManagerAPI.class);
        var storage = mock(BinaryAssetStorageAPI.class);
        var metadata = mock(FileMetadataAPI.class);
        when(storage.listBinaryPaths(INODE)).thenReturn(List.of(OLD_REVISION));
        when(metadata.listMetadataForInode(INODE, List.of(OLD_REVISION))).thenReturn(List.of(OLD_METADATA));
        try (var locator = mockStatic(APILocator.class);
             var connection = mockStatic(DbConnectionFactory.class)) {
            locator.when(APILocator::getJobQueueManagerAPI).thenReturn(queue);
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);
            // Listing needs no transaction; recording still does.
            final var paths = BinaryAssetCleanupProcessor.storedPaths(INODE);
            assertThrows(DotDataException.class, () -> BinaryAssetCleanupProcessor.enqueue(INODE, paths));
            verifyNoInteractions(queue);

            connection.when(DbConnectionFactory::inTransaction).thenReturn(true);
            BinaryAssetCleanupProcessor.enqueue(INODE, paths);
            verify(queue).createJob(BinaryAssetCleanupProcessor.QUEUE,
                    Map.of("inode", INODE, "binaries", List.of(OLD_REVISION), "metadata", List.of(OLD_METADATA)));
            verify(storage, times(1)).listBinaryPaths(INODE);
        }
    }

    @Test void disabledFlagDoesNotCreateOrExecuteCleanup() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, false);
        try (var locator = mockStatic(APILocator.class)) {
            BinaryAssetCleanupProcessor.enqueue(INODE);
            assertThrows(JobProcessingException.class, () -> new BinaryAssetCleanupProcessor().process(job()));
            locator.verifyNoInteractions();
        }
    }

    @Test void publicJobEndpointSubmissionsAreRejected() {
        var processor = new BinaryAssetCleanupProcessor();
        // The public endpoint always adds userId; content deletion never does.
        assertThrows(JobValidationException.class,
                () -> processor.validate(Map.of("inode", INODE, "userId", "backend-user")));
        assertDoesNotThrow(() -> processor.validate(
                Map.of("inode", INODE, "binaries", List.of(), "metadata", List.of())));
        Config.setProperty(AssetStorageFeature.FLAG, false);
        assertThrows(JobValidationException.class, () -> processor.validate(Map.of("inode", INODE)));
    }

    @Test void revisionUploadedAfterEnqueueSurvivesCleanup() throws Exception {
        var queue = mock(JobQueueManagerAPI.class);
        var storage = mock(BinaryAssetStorageAPI.class);
        var metadata = mock(FileMetadataAPI.class);
        var files = mock(FileAssetAPI.class);
        when(files.getRealAssetsRootPath()).thenReturn(root.toString());
        // At deletion time only the old revision exists.
        when(storage.listBinaryPaths(INODE)).thenReturn(List.of(OLD_REVISION));
        when(metadata.listMetadataForInode(INODE, List.of(OLD_REVISION))).thenReturn(List.of(OLD_METADATA));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> recorded = ArgumentCaptor.forClass(Map.class);
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of()));
             var locator = mockStatic(APILocator.class);
             var connection = mockStatic(DbConnectionFactory.class)) {
            connection.when(DbConnectionFactory::inTransaction).thenReturn(true);
            locator.when(APILocator::getJobQueueManagerAPI).thenReturn(queue);
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);
            locator.when(APILocator::getFileAssetAPI).thenReturn(files);
            BinaryAssetCleanupProcessor.enqueue(INODE);
            verify(queue).createJob(eq(BinaryAssetCleanupProcessor.QUEUE), recorded.capture());

            // A push-publish receiver re-creates the inode and uploads a new revision before the job runs.
            when(storage.listBinaryPaths(INODE)).thenReturn(List.of(OLD_REVISION, NEW_REVISION));
            new BinaryAssetCleanupProcessor().process(job(recorded.getValue()));

            verify(metadata).removeMetadataPaths(INODE, List.of(OLD_METADATA));
            verify(storage).deleteBinaryPaths(INODE, "heroImage", List.of(OLD_REVISION));
            verify(storage, never()).deleteBinaryPaths(anyString(), anyString(),
                    argThat(paths -> paths.contains(NEW_REVISION)));
        }
    }

    private static final String KEPT_REVISION =
            "a/b/abc123/heroImage/.revisions/33333333-3333-3333-3333-333333333333/kept.png";
    private static final String LEGACY_BINARY = "a/b/abc123/heroImage/legacy.png";
    private static final String LEGACY_METADATA = "/a/b/abc123/heroImage-metadata.json";

    /**
     * Runs the processor against a contentlet row with this inode that exists again, carrying
     * {@code json}, with storage, metadata and transaction calls mocked.
     */
    private void processReturnedInode(final Job job, final String json, final BinaryAssetStorageAPI storage,
                                      final FileMetadataAPI metadata, final FileAssetAPI files) throws Exception {
        final Map<String, Object> row = new java.util.HashMap<>();
        row.put("inode", INODE);
        row.put("contentlet_as_json", json);
        final String previousAssets = Config.getStringProperty("ASSET_REAL_PATH", null);
        Config.setProperty("ASSET_REAL_PATH", root.toString());
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of(row)));
             var transactions = mockStatic(com.dotmarketing.db.LocalTransaction.class);
             var locator = mockStatic(APILocator.class)) {
            transactions.when(() -> com.dotmarketing.db.LocalTransaction.wrapReturn(any())).thenAnswer(call ->
                    ((com.dotcms.util.ReturnableDelegate<?>) call.getArgument(0)).execute());
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);
            locator.when(APILocator::getFileAssetAPI).thenReturn(files);
            new BinaryAssetCleanupProcessor().process(job);
        } finally {
            Config.setProperty("ASSET_REAL_PATH", previousAssets);
        }
    }

    @Test void returnedInodeKeepsWhatItReferencesAndDeletesOtherRecordedRevisions() throws Exception {
        final var storage = mock(BinaryAssetStorageAPI.class);
        final var metadata = mock(FileMetadataAPI.class);
        final var files = mock(FileAssetAPI.class);
        when(files.getRealAssetsRootPath()).thenReturn(root.toString());
        final Path cache = root.resolve("cache/a/b/abc123/old-image.png");
        Files.createDirectories(cache.getParent());
        Files.writeString(cache, "cached pixels");
        // The filesystem lists metadata in lower case; a custom key in the kept revision's folder.
        final String keptMetadata = "/" + KEPT_REVISION.toLowerCase() + ".44444444-4444-4444-4444-444444444444"
                + FileMetadataAPI.METADATA_JSON;
        final Job job = job(Map.of("inode", INODE,
                "binaries", List.of(OLD_REVISION, KEPT_REVISION, LEGACY_BINARY),
                "metadata", List.of(OLD_METADATA, "/" + KEPT_REVISION + FileMetadataAPI.METADATA_JSON,
                        keptMetadata, LEGACY_METADATA)));
        // The returned row references one recorded revision and a new one recorded nowhere.
        final String json = "{\"fields\":{"
                + "\"heroImage\":{\"type\":\"Binary\",\"value\":\"kept.png\",\"storageKey\":\"" + KEPT_REVISION + "\"},"
                + "\"attachment\":{\"type\":\"Binary\",\"value\":\"new.png\",\"storageKey\":"
                + "\"a/b/abc123/attachment/.revisions/55555555-5555-5555-5555-555555555555/new.png\"}}}";

        processReturnedInode(job, json, storage, metadata, files);

        verify(metadata).removeMetadataPaths(INODE, List.of(OLD_METADATA));
        verify(storage).deleteBinaryPaths(INODE, "heroImage", List.of(OLD_REVISION));
        verify(storage, times(1)).deleteBinaryPaths(anyString(), anyString(), anyList());
        verify(storage, never()).deleteGeneratedFiles(anyString());
        assertTrue(Files.exists(cache), "Legacy cache stays while the inode exists");
    }

    @Test void returnedInodeWithoutReadableReferencesKeepsEverything() throws Exception {
        final var storage = mock(BinaryAssetStorageAPI.class);
        final var metadata = mock(FileMetadataAPI.class);
        final var files = mock(FileAssetAPI.class);
        processReturnedInode(job(), null, storage, metadata, files);
        verify(metadata, never()).removeMetadataPaths(anyString(), argThat(paths -> !paths.isEmpty()));
        verify(storage, never()).deleteBinaryPaths(anyString(), anyString(), anyList());
        verify(storage, never()).deleteGeneratedFiles(anyString());
    }

    @Test void inodeThatDisappearsDuringCleanupIsRetried() throws Exception {
        // The first query sees the row; the locked re-read finds it gone, so the job retries and
        // the next run takes the whole-inode path.
        final var storage = mock(BinaryAssetStorageAPI.class);
        final var calls = new java.util.concurrent.atomic.AtomicInteger();
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenAnswer(call ->
                             calls.getAndIncrement() == 0 ? List.of(Map.of("inode", INODE)) : List.of()));
             var transactions = mockStatic(com.dotmarketing.db.LocalTransaction.class);
             var locator = mockStatic(APILocator.class)) {
            transactions.when(() -> com.dotmarketing.db.LocalTransaction.wrapReturn(any())).thenAnswer(call ->
                    ((com.dotcms.util.ReturnableDelegate<?>) call.getArgument(0)).execute());
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(mock(FileMetadataAPI.class));
            assertThrows(JobProcessingException.class, () -> new BinaryAssetCleanupProcessor().process(job()));
            verify(storage, never()).deleteBinaryPaths(anyString(), anyString(), anyList());
        }
    }

    @Test void jobWithoutInventoryDeletesNothing() throws Exception {
        try (var locator = mockStatic(APILocator.class)) {
            assertThrows(JobProcessingException.class,
                    () -> new BinaryAssetCleanupProcessor().process(job(Map.of("inode", INODE))));
            locator.verify(APILocator::getBinaryAssetStorageAPI, never());
        }
    }

    @Test void remoteFailureIsRetryableAndPreservesLegacyCache() throws Exception {
        var storage = mock(BinaryAssetStorageAPI.class);
        var files = mock(FileAssetAPI.class);
        when(files.getRealAssetsRootPath()).thenReturn(root.toString());
        Path cache = root.resolve("cache/a/b/abc123/old-image.png");
        Files.createDirectories(cache.getParent());
        Files.writeString(cache, "cached pixels");
        doThrow(new DotDataException("S3 unavailable")).doNothing()
                .when(storage).deleteBinaryPaths(INODE, "heroImage", List.of(OLD_REVISION));
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of()));
             var locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileAssetAPI).thenReturn(files);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(mock(FileMetadataAPI.class));
            var processor = new BinaryAssetCleanupProcessor();
            assertThrows(JobProcessingException.class, () -> processor.process(job()));
            assertTrue(Files.exists(cache));
            processor.process(job());
            assertFalse(Files.exists(cache));
            processor.process(job()); // Retry after completion is harmless.
        }
    }

    @Test void invalidInodeCannotEscapeAssetRoot() {
        Job job = job(Map.of("inode", "../../outside", "binaries", List.of(), "metadata", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new BinaryAssetCleanupProcessor().process(job));
    }

    @Test void metadataFailureRetainsSourcesForRetry() throws Exception {
        var storage = mock(BinaryAssetStorageAPI.class);
        var metadata = mock(FileMetadataAPI.class);
        var files = mock(FileAssetAPI.class);
        when(files.getRealAssetsRootPath()).thenReturn(root.toString());
        doThrow(new DotDataException("metadata bucket unavailable")).doNothing()
                .when(metadata).removeMetadataPaths(INODE, List.of(OLD_METADATA));
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of()));
             var locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);
            locator.when(APILocator::getFileAssetAPI).thenReturn(files);
            var processor = new BinaryAssetCleanupProcessor();
            assertThrows(JobProcessingException.class, () -> processor.process(job()));
            verify(storage, never()).deleteBinaryPaths(anyString(), anyString(), anyList());
            processor.process(job());
            verify(storage).deleteBinaryPaths(INODE, "heroImage", List.of(OLD_REVISION));
        }
    }

    @Test void rollbackDeletesOnlyTheJustUploadedRevisionAndNeverThrows() throws Exception {
        var storage = mock(BinaryAssetStorageAPI.class);
        var metadata = mock(FileMetadataAPI.class);
        AtomicReference<Runnable> listener = new AtomicReference<>();
        try (var transactions = mockStatic(HibernateUtil.class);
             var locator = mockStatic(APILocator.class)) {
            transactions.when(() -> HibernateUtil.addRollbackListener(any(Runnable.class)))
                    .thenAnswer(call -> {
                        listener.set(call.getArgument(0));
                        return null;
                    });
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);

            BinaryAssetCleanupProcessor.deleteRevisionOnRollback(INODE, "heroImage", null);
            assertNull(listener.get(), "A file that is not a new revision is never deleted");

            BinaryAssetCleanupProcessor.deleteRevisionOnRollback(INODE, "heroImage", NEW_REVISION);
            verifyNoInteractions(storage, metadata);
            listener.get().run();
            verify(metadata).removeMetadataPaths(INODE, List.of("/" + NEW_REVISION + FileMetadataAPI.METADATA_JSON));
            verify(storage).deleteBinaryPaths(INODE, "heroImage", List.of(NEW_REVISION));

            doThrow(new DotDataException("S3 unavailable")).when(storage)
                    .deleteBinaryPaths(INODE, "heroImage", List.of(NEW_REVISION));
            assertDoesNotThrow(() -> listener.get().run());
        }
    }
}

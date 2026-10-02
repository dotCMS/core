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
            verify(storage, never()).deleteAllBinaries(anyString());
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
            verify(storage, never()).deleteAllBinaries(anyString());
        }
    }

    @Test void referencedVersionIsNeverDeleted() throws Exception {
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of(Map.of("inode", INODE))));
             var locator = mockStatic(APILocator.class)) {
            assertThrows(JobProcessingException.class, () -> new BinaryAssetCleanupProcessor().process(job()));
            locator.verify(APILocator::getBinaryAssetStorageAPI, never());
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

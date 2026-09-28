package com.dotcms.storage.binary;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BinaryAssetCleanupProcessorTest {
    @TempDir Path root;
    private String previousFlag;
    private static final String INODE = "abc123";

    @BeforeEach void enable() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
    }

    @AfterEach void restore() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    private Job job() {
        Job job = mock(Job.class);
        when(job.id()).thenReturn("cleanup-test");
        when(job.parameters()).thenReturn(com.google.common.collect.ImmutableMap.of("inode", INODE));
        return job;
    }

    @Test void enqueueRequiresTransactionAndPropagatesJournalFailure() throws Exception {
        var queue = mock(JobQueueManagerAPI.class);
        try (var locator = mockStatic(APILocator.class);
             var connection = mockStatic(DbConnectionFactory.class)) {
            locator.when(APILocator::getJobQueueManagerAPI).thenReturn(queue);
            assertThrows(DotDataException.class, () -> BinaryAssetCleanupProcessor.enqueue(INODE));
            verifyNoInteractions(queue);
            connection.when(DbConnectionFactory::inTransaction).thenReturn(true);
            when(queue.createJob(BinaryAssetCleanupProcessor.QUEUE, Map.of("inode", INODE)))
                    .thenThrow(new DotDataException("database unavailable"));
            assertThrows(DotDataException.class, () -> BinaryAssetCleanupProcessor.enqueue(INODE));
            locator.verify(APILocator::getBinaryAssetStorageAPI, never());
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

    @Test void referencedVersionIsNeverDeleted() throws Exception {
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of(Map.of("inode", INODE))));
             var locator = mockStatic(APILocator.class)) {
            assertThrows(JobProcessingException.class, () -> new BinaryAssetCleanupProcessor().process(job()));
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
        doThrow(new DotDataException("S3 unavailable")).doNothing().when(storage).deleteAllBinaries(INODE);
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of()));
             var locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileAssetAPI).thenReturn(files);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(mock(com.dotcms.storage.FileMetadataAPI.class));
            var processor = new BinaryAssetCleanupProcessor();
            assertThrows(JobProcessingException.class, () -> processor.process(job()));
            assertTrue(Files.exists(cache));
            processor.process(job());
            assertFalse(Files.exists(cache));
            processor.process(job()); // Retry after completion is harmless.
        }
    }

    @Test void invalidInodeCannotEscapeAssetRoot() {
        Job job = job();
        when(job.parameters()).thenReturn(com.google.common.collect.ImmutableMap.of("inode", "../../outside"));
        assertThrows(IllegalArgumentException.class, () -> new BinaryAssetCleanupProcessor().process(job));
    }

    @Test void metadataFailureRetainsOriginalKeysForRetry() throws Exception {
        var storage = mock(BinaryAssetStorageAPI.class);
        var metadata = mock(com.dotcms.storage.FileMetadataAPI.class);
        var files = mock(FileAssetAPI.class);
        when(files.getRealAssetsRootPath()).thenReturn(root.toString());
        var paths = List.of("a/b/abc123/HeroImage/.revisions/revision/File.PNG");
        when(storage.listBinaryPaths(INODE)).thenReturn(paths);
        doThrow(new DotDataException("metadata bucket unavailable")).doNothing()
                .when(metadata).removeMetadataForInode(INODE, paths);
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of()));
             var locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);
            locator.when(APILocator::getFileAssetAPI).thenReturn(files);
            var processor = new BinaryAssetCleanupProcessor();
            assertThrows(JobProcessingException.class, () -> processor.process(job()));
            verify(storage, never()).deleteAllBinaries(INODE);
            processor.process(job());
            verify(storage).deleteAllBinaries(INODE);
        }
    }
}

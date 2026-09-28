package com.dotcms.storage.binary;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.job.CleanUpFieldReferencesJob;
import com.dotmarketing.util.Config;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BinaryFieldCleanupProcessorTest {
    @Test void disabledFieldDeletionRetainsLegacySchedulingAndRejectsS3Work() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var locator = mockStatic(APILocator.class); var transactions = mockStatic(HibernateUtil.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, false);
            CleanUpFieldReferencesJob.triggerCleanUpJob(mock(BinaryField.class), mock(com.liferay.portal.model.User.class));
            transactions.verify(() -> HibernateUtil.addCommitListenerNoThrow(any()));
            assertThrows(DotDataException.class, () -> BinaryFieldCleanupProcessor.enqueue("type", "image", new Date()));
            assertThrows(DotDataException.class, () -> BinaryFieldCleanupProcessor.clean("type", null, "image"));
            final Job job = mock(Job.class);
            assertThrows(JobProcessingException.class, () -> new BinaryFieldCleanupProcessor().process(job));
            locator.verifyNoInteractions();
        } finally { Config.setProperty(AssetStorageFeature.FLAG, previous); }
    }

    @Test void fieldRemovalRequiresDurableQueueWriteInItsTransaction() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var locator = mockStatic(APILocator.class); var connection = mockStatic(DbConnectionFactory.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            assertThrows(DotDataException.class, () -> BinaryFieldCleanupProcessor.enqueue("type", "image", new Date()));
            connection.when(DbConnectionFactory::inTransaction).thenReturn(true);
            assertThrows(com.dotcms.jobs.business.error.JobValidationException.class,
                    () -> new BinaryFieldCleanupProcessor().validate(Map.of("userId", "backend-user")));
            final var queue = mock(com.dotcms.jobs.business.api.JobQueueManagerAPI.class);
            locator.when(APILocator::getJobQueueManagerAPI).thenReturn(queue);
            when(queue.createJob(eq(BinaryFieldCleanupProcessor.QUEUE), anyMap())).thenThrow(new DotDataException("queue unavailable"));
            assertThrows(DotDataException.class, () -> BinaryFieldCleanupProcessor.enqueue("type", "image", new Date()));
            locator.verify(APILocator::getBinaryAssetStorageAPI, never());
        } finally { Config.setProperty(AssetStorageFeature.FLAG, previous); }
    }

    @Test void exactInventoryRejectsSiblingAndTraversalBeforeAnyDeletion() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            final var storage = mock(StoragePersistenceAPI.class);
            final var api = new BinaryAssetStorageAPIImpl(storage);
            for (String path : List.of("a/b/abc/imageOther/x", "a/b/abc/image/../other/x", "a/b/abc/image")) {
                assertThrows(IllegalArgumentException.class, () -> api.deleteBinaryPaths("abc", "image",
                        List.of("a/b/abc/image/good", path)));
            }
            verifyNoInteractions(storage);
            Config.setProperty(AssetStorageFeature.FLAG, false);
            assertThrows(DotDataException.class, () -> api.deleteBinaryPaths("abc", "image", List.of()));
            verifyNoInteractions(storage);
        } finally { Config.setProperty(AssetStorageFeature.FLAG, previous); }
    }
}

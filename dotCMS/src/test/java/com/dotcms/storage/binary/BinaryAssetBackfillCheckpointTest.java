package com.dotcms.storage.binary;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.queue.PostgresJobQueue;
import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.runonce.Task250113CreatePostgresJobQueueTables;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Real PostgreSQL checkpoints with controlled copy failures; S3 byte verification has separate tests. */
class BinaryAssetBackfillCheckpointTest {
    @Test
    void checkpointSurvivesReconnectionAndStaleStatusUpdates() throws Exception {
        final String jdbc = System.getProperty("s3.test.jdbc");
        assumeTrue(jdbc != null, "Supply s3.test.jdbc for an isolated PostgreSQL database");
        final String previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        final String schema = "binary_backfill_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection writer = DriverManager.getConnection(jdbc, "binary-storage-test", "binary-storage-test");
             Connection observer = DriverManager.getConnection(jdbc, "binary-storage-test", "binary-storage-test");
             var locator = mockStatic(APILocator.class);
             var copies = mockStatic(BinaryAssetBackfill.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            writer.createStatement().execute("create schema " + schema);
            try {
                writer.createStatement().execute("set search_path to " + schema);
                observer.createStatement().execute("set search_path to " + schema);
                DbConnectionFactory.setConnection(writer);
                new Task250113CreatePostgresJobQueueTables().executeUpgrade();
                final var server = mock(ServerAPI.class);
                when(server.readServerId()).thenReturn("backfill-checkpoint-test");
                locator.when(APILocator::getServerAPI).thenReturn(server);
                final var admin = mock(User.class);
                when(admin.isAdmin()).thenReturn(true);
                when(admin.isActive()).thenReturn(true);
                final var users = mock(UserAPI.class);
                when(users.loadUserById("administrator")).thenReturn(admin);
                locator.when(APILocator::getUserAPI).thenReturn(users);
                final var queue = new PostgresJobQueue();
                final var manager = mock(JobQueueManagerAPI.class);
                when(manager.getJob(anyString())).thenAnswer(call -> queue.getJob(call.getArgument(0)));
                locator.when(APILocator::getJobQueueManagerAPI).thenReturn(manager);
                final String id = queue.createJob(BinaryAssetBackfillProcessor.QUEUE,
                        Map.of("userId", "administrator", "batchSize", 1));
                final var staleJob = queue.getJob(id).markAsRunning();
                queue.updateJobStatus(staleJob);
                copies.when(() -> BinaryAssetBackfill.runBatch("", 1))
                        .thenReturn(new BinaryAssetBackfill.Result("aa", 2, false));
                copies.when(() -> BinaryAssetBackfill.runBatch("aa", 1))
                        .thenThrow(new DotDataException("S3 unavailable"))
                        .thenReturn(new BinaryAssetBackfill.Result("bb", 3, true));

                final var interrupted = assertThrows(JobProcessingException.class,
                        () -> new BinaryAssetBackfillProcessor().process(staleJob));
                assertEquals("S3 unavailable", interrupted.getCause().getMessage(), interrupted.toString());
                assertEquals("aa", checkpoint(observer, id, "afterInode"));
                assertEquals("2", checkpoint(observer, id, "verifiedBinaries"));
                queue.putJobBackInQueue(staleJob);
                assertEquals("aa", queue.nextJob().parameters().get("afterInode"));

                // Reconnect and construct another worker, retaining only the database checkpoint.
                DbConnectionFactory.setConnection(observer);
                com.dotmarketing.business.CacheLocator.getJobCache().put(staleJob);
                final var resumed = new BinaryAssetBackfillProcessor();
                resumed.process(staleJob);
                assertEquals("bb", checkpoint(writer, id, "afterInode"));
                assertEquals("5", checkpoint(writer, id, "verifiedBinaries"));
                copies.verify(() -> BinaryAssetBackfill.runBatch("", 1), times(1));
                queue.updateJobStatus(staleJob.markAsSuccessful(null));
                assertEquals("bb", checkpoint(writer, id, "afterInode"),
                        "The manager's original Job must not overwrite the committed checkpoint");
                assertEquals("administrator", queue.getJob(id).parameters().get("userId"));

                final String competing = queue.createJob(BinaryAssetBackfillProcessor.QUEUE,
                        Map.of("userId", "administrator", "batchSize", 1));
                copies.when(() -> BinaryAssetBackfill.runBatch("", 1)).thenAnswer(call -> {
                    try (var update = writer.prepareStatement(
                            "update job set parameters = parameters || '{\"afterInode\":\"cc\",\"verifiedBinaries\":4}'::jsonb where id = ?")) {
                        update.setString(1, competing);
                        assertEquals(1, update.executeUpdate());
                    }
                    return new BinaryAssetBackfill.Result("aa", 2, false);
                });
                assertThrows(JobProcessingException.class,
                        () -> new BinaryAssetBackfillProcessor().process(queue.getJob(competing)));
                assertEquals("cc", checkpoint(writer, competing, "afterInode"));
                assertEquals("4", checkpoint(writer, competing, "verifiedBinaries"));
            } finally {
                writer.createStatement().execute("drop schema " + schema + " cascade");
            }
        } finally {
            DbConnectionFactory.closeConnection();
            Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
        }
    }

    private String checkpoint(Connection connection, String id, String key) throws Exception {
        try (var query = connection.prepareStatement("select parameters->>? from job where id = ?")) {
            query.setString(1, key);
            query.setString(2, id);
            try (var rows = query.executeQuery()) {
                assertTrue(rows.next());
                return rows.getString(1);
            }
        }
    }
}

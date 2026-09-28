package com.dotcms.storage.binary;

import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.queue.PostgresJobQueue;
import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.runonce.Task250113CreatePostgresJobQueueTables;
import com.dotmarketing.util.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/** Runs against a disposable PostgreSQL database; no demo data is needed. */
class BinaryAssetCleanupTransactionTest {
    @Test void cleanupIntentCommitsAndRollsBackWithContentDeletion() throws Exception {
        String jdbc = System.getProperty("s3.test.jdbc");
        assumeTrue(jdbc != null, "Supply s3.test.jdbc for an isolated PostgreSQL database");
        String previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        String schema = "binary_cleanup_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection writer = DriverManager.getConnection(jdbc, "binary-storage-test", "binary-storage-test");
             Connection observer = DriverManager.getConnection(jdbc, "binary-storage-test", "binary-storage-test");
             var locator = mockStatic(APILocator.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            writer.createStatement().execute("create schema " + schema);
            try {
                writer.createStatement().execute("set search_path to " + schema);
                observer.createStatement().execute("set search_path to " + schema);
                DbConnectionFactory.setConnection(writer);
                new Task250113CreatePostgresJobQueueTables().executeUpgrade();
                writer.createStatement().execute("create table contentlet (inode varchar(255) primary key)");
                writer.createStatement().execute("insert into contentlet values ('abc123')");
                var server = mock(ServerAPI.class);
                when(server.readServerId()).thenReturn("binary-cleanup-test");
                locator.when(APILocator::getServerAPI).thenReturn(server);
                var queue = new PostgresJobQueue();
                var manager = mock(JobQueueManagerAPI.class);
                when(manager.createJob(eq(BinaryAssetCleanupProcessor.QUEUE), anyMap()))
                        .thenAnswer(call -> {
                            try {
                                return queue.createJob(call.getArgument(0), call.getArgument(1));
                            } catch (com.dotcms.jobs.business.queue.error.JobQueueException e) {
                                throw new com.dotmarketing.exception.DotDataException("Unable to record cleanup", e);
                            }
                        });
                locator.when(APILocator::getJobQueueManagerAPI).thenReturn(manager);

                writer.setAutoCommit(false);
                writer.createStatement().execute("delete from contentlet where inode = 'abc123'");
                BinaryAssetCleanupProcessor.enqueue("abc123");
                assertEquals(1, count(writer, "job_queue"));
                assertEquals(0, count(observer, "job_queue"), "Workers cannot see uncommitted cleanup");
                assertEquals(1, count(observer, "contentlet"));
                writer.rollback();
                assertEquals(0, count(observer, "job_queue"));
                assertEquals(0, count(observer, "job"));
                assertEquals(0, count(observer, "job_history"));
                assertEquals(1, count(observer, "contentlet"));

                // Fail the last queue insert: neither the content deletion nor partial intent may commit.
                writer.createStatement().execute("drop table job_history");
                writer.createStatement().execute("delete from contentlet where inode = 'abc123'");
                assertThrows(com.dotmarketing.exception.DotDataException.class,
                        () -> BinaryAssetCleanupProcessor.enqueue("abc123"));
                writer.rollback();
                assertEquals(1, count(observer, "contentlet"));
                assertEquals(0, count(observer, "job"));
                assertEquals(0, count(observer, "job_queue"));

                writer.createStatement().execute("delete from contentlet where inode = 'abc123'");
                BinaryAssetCleanupProcessor.enqueue("abc123");
                writer.commit();
                assertEquals(0, count(observer, "contentlet"));
                assertEquals(1, count(observer, "job_queue"));
                assertEquals(1, count(observer, "job"));
                assertEquals(1, count(observer, "job_history"));
                locator.verify(APILocator::getBinaryAssetStorageAPI, never());
            } finally {
                writer.rollback();
                writer.setAutoCommit(true);
                writer.createStatement().execute("drop schema " + schema + " cascade");
            }
        } finally {
            DbConnectionFactory.closeConnection();
            Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
        }
    }

    private int count(Connection connection, String table) throws Exception {
        try (var statement = connection.createStatement();
             var results = statement.executeQuery("select count(*) from " + table)) {
            assertTrue(results.next());
            return results.getInt(1);
        }
    }
}

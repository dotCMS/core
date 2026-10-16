package com.dotcms.storage.binary;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

import com.dotcms.business.interceptor.CoreDatabaseConnectionOps;
import com.dotcms.business.interceptor.CoreInterceptorLogger;
import com.dotcms.business.interceptor.CoreLicenseOps;
import com.dotcms.business.interceptor.InterceptorServiceProvider;
import com.dotcms.business.interceptor.TransactionOps;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.queue.PostgresJobQueue;
import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.runonce.Task250113CreatePostgresJobQueueTables;
import com.dotmarketing.util.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Real PostgreSQL checks for how a removed binary field is archived across a content type: each row
 * commits on its own with a saved cursor, and only the row being archived is locked. The per-row
 * archive step itself (ZIP upload, JSON rewrite) is replaced here; its behavior has separate CMS tests.
 */
class BinaryFieldCleanupCheckpointTest {
    private static final String TYPE = "type-a";
    private static final String FIELD = "image";
    private static final String STORED = "{\"fields\":{\"image\":{\"type\":\"Binary\",\"value\":\"Hero.PNG\"}}}";
    private static final String CLEARED = "{\"fields\":{}}";

    @Test
    void eachRowCommitsWithItsCursorAndARetryResumesAfterTheLastCommittedRow() throws Exception {
        final String jdbc = System.getProperty("s3.test.jdbc");
        assumeTrue(jdbc != null, "Supply s3.test.jdbc for an isolated PostgreSQL database");
        final String previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        final String schema = "binary_field_cleanup_" + UUID.randomUUID().toString().replace("-", "");
        // Unit tests do not run application startup, which installs the transaction handling behind
        // LocalTransaction. Install connection-level transactions so each row's commit and lock are real.
        final var previousDb = InterceptorServiceProvider.getDatabaseOps();
        final var previousTx = InterceptorServiceProvider.getTransactionOps();
        final var previousLicense = InterceptorServiceProvider.getLicenseOps();
        final var previousLogger = InterceptorServiceProvider.getLogger();
        InterceptorServiceProvider.init(CoreDatabaseConnectionOps.INSTANCE, new ConnectionTransactions(),
                CoreLicenseOps.INSTANCE, CoreInterceptorLogger.INSTANCE);
        try (Connection writer = DriverManager.getConnection(jdbc, "binary-storage-test", "binary-storage-test");
             Connection observer = DriverManager.getConnection(jdbc, "binary-storage-test", "binary-storage-test");
             var locator = mockStatic(APILocator.class);
             var rows = mockStatic(BinaryFieldCleanupProcessor.class, CALLS_REAL_METHODS)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            writer.createStatement().execute("create schema " + schema);
            try {
                writer.createStatement().execute("set search_path to " + schema);
                observer.createStatement().execute("set search_path to " + schema);
                DbConnectionFactory.setConnection(writer);
                new Task250113CreatePostgresJobQueueTables().executeUpgrade();
                writer.createStatement().execute("create table contentlet (inode varchar(255) primary key, "
                        + "identifier varchar(255), structure_inode varchar(255), mod_date timestamp, "
                        + "contentlet_as_json jsonb, note text)");
                for (String inode : List.of("c1", "c2", "c3", "c4", "c5")) insert(writer, inode, TYPE, "1 day");
                insert(writer, "c6", TYPE, "-1 day");     // edited after the field was removed
                insert(writer, "o1", "type-b", "1 day");  // another content type

                final var server = mock(ServerAPI.class);
                when(server.readServerId()).thenReturn("field-cleanup-checkpoint-test");
                locator.when(APILocator::getServerAPI).thenReturn(server);
                final var queue = new PostgresJobQueue();
                final var manager = mock(JobQueueManagerAPI.class);
                when(manager.getJob(anyString())).thenAnswer(call -> queue.getJob(call.getArgument(0)));
                when(manager.createJob(anyString(), anyMap())).thenAnswer(call -> queue.createJob(
                        call.getArgument(0), call.getArgument(1)));
                locator.when(APILocator::getJobQueueManagerAPI).thenReturn(manager);
                final String id = queue.createJob(BinaryFieldCleanupProcessor.QUEUE, Map.of("type", TYPE,
                        "field", FIELD, "deletedBefore", System.currentTimeMillis()));

                final List<String> archived = new ArrayList<>();
                final boolean[] failOnC3 = {true};
                rows.when(() -> BinaryFieldCleanupProcessor.archiveRow(anyMap(), eq(FIELD))).thenAnswer(call -> {
                    final String inode = ((Map<?, ?>) call.getArgument(0)).get("inode").toString();
                    archived.add(inode);
                    // Only the row being archived may be locked: finished and waiting rows stay editable.
                    assertFalse(canEdit(observer, inode), inode + " must be locked while it is archived");
                    for (String other : List.of("c1", "c2", "c3", "c4", "c5")) {
                        if (!other.equals(inode)) assertTrue(canEdit(observer, other), other + " must not be locked while " + inode + " is archived");
                    }
                    new DotConnect().setSQL("update contentlet set contentlet_as_json = ?::jsonb where inode = ?")
                            .addParam(CLEARED).addParam(inode).loadResult();
                    if (inode.equals("c3") && failOnC3[0]) throw new DotDataException("S3 unavailable");
                    return null;
                });

                final var interrupted = assertThrows(JobProcessingException.class,
                        () -> new BinaryFieldCleanupProcessor().process(queue.getJob(id)));
                assertEquals("S3 unavailable", rootCause(interrupted).getMessage(), interrupted.toString());
                assertEquals(List.of("c1", "c2", "c3"), archived);
                assertEquals(CLEARED, json(observer, "c1"), "Rows archived before the failure stay committed");
                assertEquals(CLEARED, json(observer, "c2"));
                assertEquals(STORED, json(observer, "c3"), "The failed row rolls back on its own");
                assertEquals(STORED, json(observer, "c4"));
                assertEquals("c2", cursor(observer, id), "The cursor commits with the last archived row");

                failOnC3[0] = false;
                archived.clear();
                new BinaryFieldCleanupProcessor().process(queue.getJob(id));
                assertEquals(List.of("c3", "c4", "c5"), archived, "A retry resumes after the committed cursor");
                for (String inode : List.of("c1", "c2", "c3", "c4", "c5")) assertEquals(CLEARED, json(observer, inode));
                assertEquals(STORED, json(observer, "c6"), "Edits made after the field was removed are kept");
                assertEquals(STORED, json(observer, "o1"));
            } finally {
                writer.setAutoCommit(true);
                writer.createStatement().execute("drop schema " + schema + " cascade");
            }
        } finally {
            DbConnectionFactory.closeConnection();
            Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
            InterceptorServiceProvider.init(previousDb, previousTx, previousLicense, previousLogger);
        }
    }

    /**
     * The transaction calls LocalTransaction makes, applied directly to the test's JDBC connection.
     * The application's implementation goes through Hibernate, which needs full startup.
     */
    private static final class ConnectionTransactions implements TransactionOps {
        @Override public boolean startLocalTransactionIfNeeded() throws Exception {
            final Connection connection = DbConnectionFactory.getConnection();
            if (!connection.getAutoCommit()) return false;
            connection.setAutoCommit(false);
            return true;
        }
        @Override public void commitTransaction() throws Exception { DbConnectionFactory.getConnection().commit(); }
        @Override public void rollbackTransaction() {
            try { DbConnectionFactory.getConnection().rollback(); } catch (SQLException e) { throw new IllegalStateException(e); }
        }
        @Override public void handleTransactionInterruption(Connection connection, StackTraceElement[] stack) { }
        @Override public void throwException(Throwable t) throws Exception { throw t instanceof Exception e ? e : new Exception(t); }
        @Override public String getConfigProperty(String key, String defaultValue) { return defaultValue; }
        @Override public void closeSessionSilently() { throw new UnsupportedOperationException(); }
        @Override public void startTransaction() { throw new UnsupportedOperationException(); }
        @Override public Object getSession() { throw new UnsupportedOperationException(); }
        @Override public void setSession(Object session) { throw new UnsupportedOperationException(); }
        @Override public Object createNewSession(Connection connection) { throw new UnsupportedOperationException(); }
    }

    private static void insert(Connection connection, String inode, String type, String age) throws SQLException {
        try (var insert = connection.prepareStatement("insert into contentlet values (?, ?, ?, "
                + "current_timestamp - ?::interval, ?::jsonb, null)")) {
            insert.setString(1, inode);
            insert.setString(2, "id-" + inode);
            insert.setString(3, type);
            insert.setString(4, age);
            insert.setString(5, STORED);
            insert.executeUpdate();
        }
    }

    /** Tries a short edit from another connection; a held row lock makes it time out. */
    private static boolean canEdit(Connection observer, String inode) throws SQLException {
        observer.createStatement().execute("set lock_timeout = '300ms'");
        try (var update = observer.prepareStatement("update contentlet set note = 'probe' where inode = ?")) {
            update.setString(1, inode);
            update.executeUpdate();
            return true;
        } catch (SQLException locked) {
            return false;
        }
    }

    private static String json(Connection connection, String inode) throws SQLException {
        try (var query = connection.prepareStatement("select contentlet_as_json::text from contentlet where inode = ?")) {
            query.setString(1, inode);
            try (var rows = query.executeQuery()) {
                assertTrue(rows.next());
                return rows.getString(1).replace(" ", "");
            }
        }
    }

    private static String cursor(Connection connection, String id) throws SQLException {
        try (var query = connection.prepareStatement("select parameters->>'afterInode' from job where id = ?")) {
            query.setString(1, id);
            try (var rows = query.executeQuery()) {
                assertTrue(rows.next());
                return rows.getString(1);
            }
        }
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause;
    }
}

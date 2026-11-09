package com.dotmarketing.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TaskLocatorUtil;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Simulates separate restarting nodes with independent connections and stale version snapshots. */
class StartupTasksExecutorSchemaLockTest {

    private static final AtomicInteger BUILDS = new AtomicInteger();
    private static CountDownLatch buildStarted;
    private static CountDownLatch finishBuild;

    /** Holds the first simulated node inside its upgrade while the other node starts. */
    public static class Task999999SimulatedIndexBuild implements StartupTask {
        /** Runs this fixture when its version has not been recorded. */
        @Override
        public boolean forceRun() {
            return true;
        }

        /** Counts actual upgrade execution, not attempts that later discover completion. */
        @Override
        public void executeUpgrade() throws DotDataException {
            BUILDS.incrementAndGet();
            buildStarted.countDown();
            try {
                if (!finishBuild.await(10, TimeUnit.SECONDS)) {
                    throw new DotDataException("Simulated build timed out");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DotDataException(e);
            }
        }
    }

    /** The waiting node rechecks the database version and neither rebuilds nor inserts it twice. */
    @Test
    void simultaneousNodesBuildOnceAndRecheckCompletionUnderLock() throws Exception {
        final int previousVersion = Config.DB_VERSION;
        final var version = new AtomicInteger(999998);
        final var recorded = new AtomicInteger();
        final var unlocked = new AtomicInteger();
        final var lock = new ReentrantLock();
        final var secondAttemptedLock = new CountDownLatch(1);
        buildStarted = new CountDownLatch(1);
        finishBuild = new CountDownLatch(1);
        BUILDS.set(0);
        final var workers = Executors.newFixedThreadPool(2);
        try {
            final var first = workers.submit(() -> runNode(version, recorded, unlocked, lock, null));
            assertTrue(buildStarted.await(5, TimeUnit.SECONDS));
            final var second = workers.submit(() ->
                    runNode(version, recorded, unlocked, lock, secondAttemptedLock));
            assertTrue(secondAttemptedLock.await(5, TimeUnit.SECONDS));
            assertEquals(1, BUILDS.get(), "Only the lock owner may build");
            assertEquals(0, recorded.get(), "Do not record an unfinished build");
            finishBuild.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            assertEquals(1, BUILDS.get());
            assertEquals(1, recorded.get(), "The waiter must not insert a duplicate version");
            assertEquals(999999, version.get());
            assertEquals(2, unlocked.get(), "Both pooled connections must release their lock");
            assertTrue(!lock.isLocked());
        } finally {
            finishBuild.countDown();
            workers.shutdownNow();
            workers.awaitTermination(5, TimeUnit.SECONDS);
            Config.DB_VERSION = previousVersion;
        }
    }

    /** Unlocks after failure; if unlocking itself fails, aborts rather than pooling a locked session. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void releasesOrAbortsLockedSessionOnFailure(final boolean failUnlock) throws Exception {
        final int previousVersion = Config.DB_VERSION;
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        final DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createStatement()).thenReturn(statement);
        final ResultSet acquired = mock(ResultSet.class);
        when(acquired.next()).thenReturn(true);
        when(acquired.getBoolean(1)).thenReturn(true);
        when(statement.executeQuery("SELECT pg_try_advisory_lock(1146049603, 1)"))
                .thenReturn(acquired);
        if (failUnlock) {
            when(statement.execute("SELECT pg_advisory_unlock(1146049603, 1)"))
                    .thenThrow(new SQLException("unlock failed"));
        }
        try (var database = mockStatic(DbConnectionFactory.class);
                var tasks = mockStatic(TaskLocatorUtil.class);
                var logger = mockStatic(Logger.class);
                var reindex = mockStatic(ReindexThread.class);
                var queries = mockConstruction(DotConnect.class,
                        withSettings().defaultAnswer(RETURNS_SELF), (query, context) -> {
                            when(query.loadInt(anyString(), any(Connection.class)))
                                    .thenAnswer(invocation -> {
                                        if (!failUnlock) {
                                            throw new DotDataException("version read failed");
                                        }
                                        return 999999;
                                    });
                        })) {
            database.when(DbConnectionFactory::isPostgres).thenReturn(true);
            database.when(DbConnectionFactory::getDataSource).thenReturn(source);
            tasks.when(TaskLocatorUtil::getStartupRunOnceTaskClasses).thenReturn(List.of());
            assertThrows(DotDataException.class,
                    () -> mock(StartupTasksExecutor.class, CALLS_REAL_METHODS).executeSchemaUpgrades());
            verify(statement).execute("SELECT pg_advisory_unlock(1146049603, 1)");
            if (failUnlock) {
                verify(connection).abort(any());
            } else {
                verify(connection, never()).abort(any());
            }
            verify(connection).close();
        } finally {
            Config.DB_VERSION = previousVersion;
        }
    }

    /** Gives one logical node its own JDBC connection and thread-local application mocks. */
    private void runNode(final AtomicInteger version, final AtomicInteger recorded,
            final AtomicInteger unlocked, final ReentrantLock lock,
            final CountDownLatch attempted) {
        final Connection connection = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        final DataSource source = mock(DataSource.class);
        try {
            when(source.getConnection()).thenReturn(connection);
            when(connection.getAutoCommit()).thenReturn(true);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery("SELECT pg_try_advisory_lock(1146049603, 1)"))
                    .thenAnswer(invocation -> {
                        if (attempted != null) {
                            attempted.countDown();
                        }
                        final boolean acquired = lock.tryLock();
                        if (acquired) {
                            // Model a different JVM with a stale cached version after waiting.
                            Config.DB_VERSION = 999998;
                        }
                        final ResultSet result = mock(ResultSet.class);
                        when(result.next()).thenReturn(true);
                        when(result.getBoolean(1)).thenReturn(acquired);
                        return result;
                    });
            when(statement.execute("SELECT pg_advisory_unlock(1146049603, 1)"))
                    .thenAnswer(invocation -> {
                        assertTrue(lock.isHeldByCurrentThread());
                        unlocked.incrementAndGet();
                        lock.unlock();
                        return false;
                    });
            doAnswer(invocation -> {
                assertTrue(!lock.isHeldByCurrentThread(), "Never return a locked pooled session");
                return null;
            }).when(connection).close();
            try (var database = mockStatic(DbConnectionFactory.class);
                    var tasks = mockStatic(TaskLocatorUtil.class);
                    var hibernate = mockStatic(HibernateUtil.class);
                    var logger = mockStatic(Logger.class);
                    var reindex = mockStatic(ReindexThread.class);
                    var queries = mockConstruction(DotConnect.class,
                            withSettings().defaultAnswer(RETURNS_SELF), (query, context) -> {
                                when(query.loadInt(anyString(), any(Connection.class)))
                                        .thenAnswer(invocation -> version.get());
                                doAnswer(invocation -> {
                                    assertTrue(lock.isHeldByCurrentThread());
                                    assertTrue(version.compareAndSet(999998, 999999),
                                            "Duplicate db_version insert");
                                    recorded.incrementAndGet();
                                    return null;
                                }).when(query).loadResult();
                            })) {
                database.when(DbConnectionFactory::isPostgres).thenReturn(true);
                database.when(DbConnectionFactory::getDataSource).thenReturn(source);
                tasks.when(TaskLocatorUtil::getStartupRunOnceTaskClasses)
                        .thenReturn(List.of(Task999999SimulatedIndexBuild.class));
                tasks.when(TaskLocatorUtil::getTaskClassesNoTransaction)
                        .thenReturn(List.of(Task999999SimulatedIndexBuild.class));
                mock(StartupTasksExecutor.class, CALLS_REAL_METHODS).executeSchemaUpgrades();
            }
        } catch (Exception e) {
            throw new AssertionError("Simulated startup failed", e);
        }
    }
}

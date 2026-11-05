package com.dotcms.junit;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

/**
 * Fails a test that runs longer than a time limit, and first logs what it was doing: the stack of every thread, with
 * the locks each one holds and waits for, then the sessions of the test database and the lock requests that are not
 * granted. A test that hangs then fails within the limit and its log shows whether it was spinning in Java or waiting
 * on the database, instead of running until the CI job times out with no output.
 *
 * <p>Like JUnit's own {@link org.junit.rules.Timeout}, the test (with its {@code @Before} and {@code @After}
 * methods) runs on a separate thread so the limit can be enforced; that thread closes its database connection when
 * the test ends. A test that passes within the limit logs nothing extra.</p>
 *
 * <pre>{@code
 * @Rule
 * public final TimeoutWithDiagnostics timeout = new TimeoutWithDiagnostics(Duration.ofMinutes(5));
 * }</pre>
 */
public class TimeoutWithDiagnostics implements TestRule {

    private final Duration limit;

    /**
     * @param limit How long a test may run before it is failed.
     */
    public TimeoutWithDiagnostics(final Duration limit) {
        this.limit = limit;
    }

    /**
     * Wraps the test so it runs on its own thread, and fails it with a {@link TestTimedOutException} after logging
     * the threads and database waits when it is still running once the limit has passed.
     *
     * @param base        The test, with its {@code @Before} and {@code @After} methods.
     * @param description The test being run.
     *
     * @return The time-limited test.
     */
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final AtomicReference<Throwable> failure = new AtomicReference<>();
                final Thread test = new Thread(() -> {
                    try {
                        base.evaluate();
                    } catch (final Throwable e) {
                        failure.set(e);
                    } finally {
                        DbConnectionFactory.closeConnection();
                    }
                }, "Time-limited test " + description.getMethodName());
                test.setDaemon(true);
                test.start();
                test.join(limit.toMillis());
                if (test.isAlive()) {
                    logThreadsAndDatabaseWaits(description);
                    test.interrupt();
                    throw new TestTimedOutException(limit.toMillis(), TimeUnit.MILLISECONDS);
                }
                if (failure.get() != null) {
                    throw failure.get();
                }
            }
        };
    }

    /**
     * Writes to the log the state, the held and awaited locks and the full stack of every thread, then the sessions
     * of the current database with the sessions blocking them and the lock requests that are not granted. The threads
     * are written first, so they are logged even when the database cannot be read.
     *
     * @param description The test that ran past its limit.
     */
    private void logThreadsAndDatabaseWaits(final Description description) {
        final StringBuilder threads = new StringBuilder(description.getDisplayName())
                .append(" still running after ").append(limit).append(". Threads:\n");
        for (final ThreadInfo thread : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
            threads.append('"').append(thread.getThreadName()).append("\" ").append(thread.getThreadState());
            if (thread.getLockName() != null) {
                threads.append(" waiting on ").append(thread.getLockName());
            }
            if (thread.getLockOwnerName() != null) {
                threads.append(" owned by \"").append(thread.getLockOwnerName()).append('"');
            }
            threads.append('\n');
            for (final StackTraceElement frame : thread.getStackTrace()) {
                threads.append("    at ").append(frame).append('\n');
            }
        }
        Logger.warn(TimeoutWithDiagnostics.class, threads.toString());

        final StringBuilder database = new StringBuilder("Database sessions:\n");
        try {
            database.append(new DotConnect().setSQL(
                    "SELECT pid, state, wait_event_type, wait_event, pg_blocking_pids(pid) AS blocked_by, "
                            + "now() - xact_start AS transaction_age, left(query, 300) AS query "
                            + "FROM pg_stat_activity WHERE datname = current_database()").loadObjectResults());
            database.append("\nLock requests not granted:\n").append(new DotConnect().setSQL(
                    "SELECT l.pid, l.locktype, l.mode, c.relname FROM pg_locks l "
                            + "LEFT JOIN pg_class c ON c.oid = l.relation WHERE NOT l.granted").loadObjectResults());
        } catch (final Exception e) {
            database.append("could not be read: ").append(e.getMessage());
        } finally {
            DbConnectionFactory.closeConnection();
        }
        Logger.warn(TimeoutWithDiagnostics.class, database.toString());
    }

}

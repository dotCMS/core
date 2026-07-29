package com.dotcms.dotpubsub;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Reproducer for the spike in issue #36544 — the Postgres pub/sub listener rebuilds its
 * dedicated connection without bound, exhausting {@code jdbc/dotCMSPool}.
 * <p>
 * In the production incident {@code LISTEN cluster_actions} executed 3,687 times in a single
 * 600s window. That count is a direct {@code PGListener}-instantiation counter, because
 * {@link JDBCPubSubImpl}'s private {@code listener()} re-issues {@code LISTEN} for every
 * subscribed topic each time it constructs a new listener — so ~3,687 instantiations means
 * ~3,687 pooled-connection borrows for the listener alone.
 * <p>
 * <b>These tests assert the desired invariants, so they FAIL on current code by design.</b>
 * That is the point: they are the reproducible gate for the follow-up fixes. They are
 * {@link Ignore}d and deliberately NOT registered in any {@code MainSuite}/{@code Junit5Suite}
 * so they cannot redden CI before those fixes land. Run them explicitly:
 *
 * <pre>
 * ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false \
 *     -Dit.test=JDBCPubSubImplConnectionChurnReproTest
 * </pre>
 *
 * For the system-level reproduction (2-node cluster, real pool exhaustion) see
 * {@code docker/docker-compose-examples/pubsub-connection-churn/}.
 *
 * @see <a href="https://github.com/dotCMS/core/issues/36544">#36544</a>
 */
@Ignore("Spike #36544 reproducer — asserts desired behavior, fails on current code. Run manually.")
public class JDBCPubSubImplConnectionChurnReproTest extends IntegrationTestBase {

    private static final String LISTENER_THREAD_NAME = "PGListener Pub/Sub Thread";

    /** How many listener deaths to simulate. Enough to make accumulation obvious. */
    private static final int CHURN_CYCLES = 25;

    private static final long THREAD_EXIT_TIMEOUT_MS = 15_000L;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /** Minimal topic; only {@link KeyFilterable#getKey()} is mandatory. */
    private static final class ReproTopic implements DotPubSubTopic {

        private final String key;

        private ReproTopic(final String key) {
            this.key = key;
        }

        @Override
        public Comparable<String> getKey() {
            return this.key;
        }

        @Override
        public void notify(final DotPubSubEvent event) {
            // no-op; this test cares about connection accounting, not delivery
        }
    }

    /**
     * Simulating the listener's connection dying is the whole point of this test, and the only
     * way to do it without a live Postgres backend to terminate is to reach the connection the
     * listener is holding. That connection is intentionally private, so reflection is the
     * mechanism — acceptable for a diagnostic reproducer, and it is confined to these helpers.
     */
    private static Object internalListener(final JDBCPubSubImpl provider) throws Exception {
        final Field field = JDBCPubSubImpl.class.getDeclaredField("internalListener");
        field.setAccessible(true);
        return field.get(provider);
    }

    /**
     * Closes the connection the listener currently holds, mimicking a server-side
     * {@code pg_terminate_backend}, a network drop, or a proxy idle-timeout. Returns false if
     * there is nothing to close.
     * <p>
     * Guarded on {@link Lazy#isEvaluated()} so we never force an unresolved {@code Lazy} to
     * borrow a connection just to close it — that would be this test creating the very leak it
     * is measuring.
     */
    private static boolean killListenerConnection(final JDBCPubSubImpl provider) throws Exception {
        final Object listener = internalListener(provider);
        if (listener == null) {
            return false;
        }
        final Field connField = listener.getClass().getDeclaredField("connection");
        connField.setAccessible(true);

        @SuppressWarnings("unchecked")
        final Lazy<Connection> lazyConnection = (Lazy<Connection>) connField.get(listener);
        if (lazyConnection == null || !lazyConnection.isEvaluated()) {
            return false;
        }
        return Try.of(() -> {
            lazyConnection.get().close();
            return true;
        }).getOrElse(false);
    }

    /**
     * Each {@code PGListener} that reaches {@code Thread.start()} creates one thread with this
     * name, which makes a live thread count a direct, deterministic proxy for listener
     * instantiations that actually started.
     */
    private static long countListenerThreads() {
        final Set<Thread> threads = Thread.getAllStackTraces().keySet();
        return threads.stream()
                .filter(Thread::isAlive)
                .filter(thread -> LISTENER_THREAD_NAME.equals(thread.getName()))
                .count();
    }

    /** Active (checked-out) connections, or -1 when the pool is not HikariCP. */
    private static int activeConnections() {
        final DataSource dataSource = DbConnectionFactory.getDataSource();
        if (!(dataSource instanceof HikariDataSource)) {
            return -1;
        }
        final HikariDataSource hikari = (HikariDataSource) dataSource;
        return hikari.getHikariPoolMXBean() == null
                ? -1
                : hikari.getHikariPoolMXBean().getActiveConnections();
    }

    private static void awaitListenerThreadCount(final long expected) {
        final long deadline = System.currentTimeMillis() + THREAD_EXIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline && countListenerThreads() != expected) {
            Try.run(() -> Thread.sleep(100L));
        }
    }

    /**
     * Churning the listener must not accumulate pooled connections or listener threads.
     * <p>
     * On current code every death is followed by {@code listener()} constructing a brand new
     * {@code PGListener} — borrowing a fresh connection and re-issuing {@code LISTEN} for every
     * topic — with no backoff, no rate limit and no cap. Under pool pressure the constructor's
     * borrow blocks for the full {@code DB_CONNECTION_TIMEOUT} while holding the static
     * {@code PGListener.class} monitor, so every publisher thread convoys behind it.
     */
    @Test
    public void repro_listenerChurnMustNotAccumulateConnectionsOrThreads() throws Exception {

        final long baselineThreads = countListenerThreads();
        final int baselineActive = activeConnections();
        Assume.assumeTrue("Requires a HikariCP pool to measure connection accounting",
                baselineActive >= 0);

        final JDBCPubSubImpl provider = new JDBCPubSubImpl("repro-36544-churn");
        final DotPubSubTopic topic = new ReproTopic("repro_churn_topic");

        provider.subscribe(topic);
        awaitListenerThreadCount(baselineThreads + 1);
        assertNotNull("subscribe() should have built a listener", internalListener(provider));

        int rebuilds = 0;
        try {
            for (int cycle = 0; cycle < CHURN_CYCLES; cycle++) {

                final Object listenerBeforeKill = internalListener(provider);
                if (!killListenerConnection(provider)) {
                    continue;
                }

                // Any publish routes through listener(), which is what rebuilds the listener
                // after a death — exactly how a cache invalidation triggers it in production.
                provider.publish(new DotPubSubEvent.Builder()
                        .withTopic(topic)
                        .withType("REPRO")
                        .build());

                if (internalListener(provider) != listenerBeforeKill) {
                    rebuilds++;
                }
            }
        } finally {
            Try.run(provider::stop);
        }

        final long threadsAfter = countListenerThreads();
        final int activeAfter = activeConnections();

        Logger.info(JDBCPubSubImplConnectionChurnReproTest.class,
                String.format("#36544 churn: cycles=%d rebuilds=%d threads=%d->%d active=%d->%d",
                        CHURN_CYCLES, rebuilds, baselineThreads, threadsAfter,
                        baselineActive, activeAfter));

        assertTrue(String.format(
                "Listener threads must not accumulate across churn: baseline=%d after=%d "
                        + "(%d rebuilds over %d cycles). Each orphaned thread holds a pooled "
                        + "connection.",
                baselineThreads, threadsAfter, rebuilds, CHURN_CYCLES),
                threadsAfter <= baselineThreads + 1);

        assertTrue(String.format(
                "Active connections must not grow with listener churn: baseline=%d after=%d "
                        + "(%d rebuilds). This is the pool-exhaustion mechanism in #36544.",
                baselineActive, activeAfter, rebuilds),
                activeAfter <= baselineActive + 1);
    }

    /**
     * {@code stop()} is {@code listener().stopListening()}. When the listener is already dead,
     * {@code listener()} constructs an entire replacement first — borrowing a connection,
     * re-issuing {@code LISTEN} for every topic and starting a thread — purely to stop it
     * again. Shutting down must never allocate.
     */
    @Test
    public void repro_stopOnDeadListenerMustNotBuildAReplacement() throws Exception {

        final long baselineThreads = countListenerThreads();
        final JDBCPubSubImpl provider = new JDBCPubSubImpl("repro-36544-stop");

        provider.subscribe(new ReproTopic("repro_stop_topic"));
        awaitListenerThreadCount(baselineThreads + 1);

        provider.stop();
        awaitListenerThreadCount(baselineThreads);

        final Object listenerAfterFirstStop = internalListener(provider);
        final int activeAfterFirstStop = activeConnections();

        // Second stop() on an already-stopped provider: must be a no-op.
        provider.stop();

        final Object listenerAfterSecondStop = internalListener(provider);

        assertSame("stop() on an already-stopped provider must not construct a replacement "
                        + "listener (which borrows a connection and re-issues LISTEN just to "
                        + "shut it down again)",
                listenerAfterFirstStop, listenerAfterSecondStop);

        if (activeAfterFirstStop >= 0) {
            assertTrue("A redundant stop() must not leave a connection checked out",
                    activeConnections() <= activeAfterFirstStop);
        }
    }
}

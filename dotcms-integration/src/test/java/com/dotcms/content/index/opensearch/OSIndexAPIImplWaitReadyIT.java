package com.dotcms.content.index.opensearch;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.client.opensearch.OpenSearchClient;

/**
 * Integration tests for the phase-aware OpenSearch startup connection gate
 * {@link OSIndexAPIImpl#waitUtilIndexReady()} (issue #36244).
 *
 * <p>These tests exercise the <strong>retry-exhausted</strong> branch with an
 * {@link OSClientProvider} whose {@code getClient()} always throws, simulating an
 * unreachable / misconfigured OpenSearch cluster. The connection attempts and retry sleep are
 * forced to their minimum so the gate exhausts immediately without a real cluster.</p>
 *
 * <p>Registered in {@link com.dotcms.OpenSearchUpgradeSuite}. Run with:
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true
 * </pre>
 * </p>
 *
 * <h2>What is and isn't covered here</h2>
 * <ul>
 *   <li><strong>Phase 1 / 2 (shadow)</strong> — covered: the gate must NOT kill the JVM; it halts
 *       the migration (phase reset to 0) and returns {@code false}. These two cases use the failing
 *       client provider, so they do not need the live OpenSearch container.</li>
 *   <li><strong>Phase 3 (OS primary)</strong> — covered via a test exit seam
 *       ({@link OSIndexAPIImpl#setExitHandler}): the gate must request an abort (exit code 1) and
 *       must NOT fall back (phase stays {@code PHASE_3}). The seam replaces
 *       {@code SystemExitManager.immediateExit} — which calls {@code Runtime.halt()} and would
 *       otherwise kill the test JVM — so the abort contract is observable without terminating the
 *       process.</li>
 *   <li><strong>Success path</strong> — covered by the other OpenSearch integration tests that run
 *       against the live cluster (a reachable {@code client.info()}).</li>
 * </ul>
 *
 * @author Fabrizzio Araya
 */
public class OSIndexAPIImplWaitReadyIT {

    /** {@link OSClientProvider} that always fails to produce a client (unreachable cluster). */
    private static final class FailingClientProvider implements OSClientProvider {
        @Override
        public OpenSearchClient getClient() {
            throw new RuntimeException("simulated OpenSearch connection failure (test)");
        }
    }

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void fastRetries() {
        // Exhaust the gate immediately: a single attempt with no sleep between retries.
        Config.setProperty(OSIndexProperty.CONNECTION_ATTEMPTS.osKey, "1");
        Config.setProperty(OSIndexProperty.CONNECTION_RETRY_SLEEP_SECONDS.osKey, "0");
    }

    @After
    public void clearProps() {
        Config.setProperty(FLAG_KEY, null);
        Config.setProperty(OSIndexProperty.CONNECTION_ATTEMPTS.osKey, null);
        Config.setProperty(OSIndexProperty.CONNECTION_RETRY_SLEEP_SECONDS.osKey, null);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    /**
     * Given : Phase 1 (dual-write, ES reads) and an unreachable OpenSearch cluster.
     * When  : waitUtilIndexReady() exhausts its retries.
     * Then  : the server is NOT killed — the gate returns {@code false} and the migration is
     *         halted (FEATURE_FLAG_OPEN_SEARCH_PHASE reset to 0), so dotCMS falls back to ES-only.
     */
    @Test
    public void test_phase1_osUnreachable_fallsBackToEs_noExit() {
        setPhase(1);
        final OSIndexAPIImpl api = new OSIndexAPIImpl(new FailingClientProvider());

        final boolean ready = api.waitUtilIndexReady();

        assertFalse("Phase 1 must NOT abort: the gate returns false (ES-only fallback)", ready);
        assertEquals("Migration phase must be reset to PHASE_0 after the ES fallback",
                MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());
    }

    /**
     * Given : Phase 2 (dual-write, OS reads) and an unreachable OpenSearch cluster.
     * When  : waitUtilIndexReady() exhausts its retries.
     * Then  : same shadow-phase behavior as Phase 1 — fall back to ES (halt), return {@code false},
     *         never abort. ES still holds the authoritative state in Phase 2.
     */
    @Test
    public void test_phase2_osUnreachable_fallsBackToEs_noExit() {
        setPhase(2);
        final OSIndexAPIImpl api = new OSIndexAPIImpl(new FailingClientProvider());

        final boolean ready = api.waitUtilIndexReady();

        assertFalse("Phase 2 must NOT abort: the gate returns false (ES-only fallback)", ready);
        assertEquals("Migration phase must be reset to PHASE_0 after the ES fallback",
                MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());
    }

    /**
     * Given : Phase 3 (OpenSearch only) and an unreachable OpenSearch cluster.
     * When  : waitUtilIndexReady() exhausts its retries.
     * Then  : the gate ABORTS (requests exit code 1 via the seam) and does NOT fall back — OS is the
     *         primary store in Phase 3, so the migration phase must remain PHASE_3 (haltMigration is
     *         never called). The exit is captured by the seam instead of killing the test JVM.
     */
    @Test
    public void test_phase3_osUnreachable_aborts_noFallback() {
        setPhase(3);
        final OSIndexAPIImpl api = new OSIndexAPIImpl(new FailingClientProvider());

        final AtomicInteger exitCode = new AtomicInteger(Integer.MIN_VALUE);
        final AtomicReference<String> exitReason = new AtomicReference<>();
        api.setExitHandler((code, reason) -> {
            exitCode.set(code);
            exitReason.set(reason);
        });

        final boolean ready = api.waitUtilIndexReady();

        assertFalse("Phase 3 gate must not report OS as ready when unreachable", ready);
        assertEquals("Phase 3 must request a JVM abort with exit code 1", 1, exitCode.get());
        assertTrue("Abort reason must identify the Phase 3 store",
                exitReason.get() != null && exitReason.get().contains("PHASE_3"));
        assertEquals("Phase 3 must NOT fall back: the migration phase must remain PHASE_3",
                MigrationPhase.PHASE_3_OPENSEARCH_ONLY, MigrationPhase.current());
    }

    /**
     * Given : Phase 3 and an unreachable cluster whose failure looks like a TLS/scheme mismatch.
     * When  : waitUtilIndexReady() exhausts its retries.
     * Then  : the abort reason names the classified cause (TLS_SCHEME_MISMATCH), proving the
     *         error-classification hardening reaches the actionable Phase 3 message.
     */
    @Test
    public void test_phase3_tlsMismatch_abortReasonNamesCause() {
        setPhase(3);
        final OSIndexAPIImpl api = new OSIndexAPIImpl(new OSClientProvider() {
            @Override
            public OpenSearchClient getClient() {
                throw new RuntimeException(
                        new javax.net.ssl.SSLException("Unrecognized SSL message, plaintext connection?"));
            }
        });

        final AtomicReference<String> exitReason = new AtomicReference<>();
        api.setExitHandler((code, reason) -> exitReason.set(reason));

        api.waitUtilIndexReady();

        assertTrue("Abort reason must name the classified TLS/scheme cause",
                exitReason.get() != null && exitReason.get().contains(
                        OSIndexAPIImpl.ConnectionFailureKind.TLS_SCHEME_MISMATCH.name()));
    }
}

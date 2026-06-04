package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotmarketing.util.Config;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link PhaseRouter} fire-and-forget and failure-propagation behaviour.
 *
 * <h2>Scenario simulated</h2>
 * <p>In a common dual-write catch-up scenario the ES and OS indices carry different
 * timestamp suffixes.  For example, ES has {@code working_T0} and OS has {@code working_T1}.
 * Calling an index lifecycle operation ({@code closeIndex}, {@code openIndex}, {@code delete})
 * with the ES index name succeeds on ES but throws
 * {@code [index_not_found_exception]} on OS, because OS does not know about {@code working_T0}.</p>
 *
 * <p>The routing table below shows who is <em>primary</em> (failure propagates) and who is
 * <em>shadow</em> (failure is fire-and-forget) in each phase:</p>
 *
 * <pre>
 * Phase │ Primary │ Shadow │ ES index = T0, OS index = T1  →  closeIndex("T0")
 * ──────┼─────────┼────────┼────────────────────────────────────────────────────
 *   0   │ ES only │  —     │ ES ok                             → success
 *   1   │ ES      │ OS     │ ES ok, OS throws (T0 not in OS)   → success (fire-and-forget)
 *   2   │ OS      │ ES     │ OS throws (T0 not in OS, primary) → THROWS
 *   3   │ OS only │  —     │ OS throws                         → THROWS
 * </pre>
 *
 * <p>The inverse scenario (OS has T1, ES has T0, caller uses T1) is also tested for Phase 2
 * to confirm that ES shadow failures are fire-and-forget when OS is the primary reader.</p>
 */
public class PhaseRouterTest {

    // ── Test setup / teardown ─────────────────────────────────────────────────

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    // =========================================================================
    // write() — void unchecked fan-out
    // Used by: IndexAPIImpl.closeIndex(), openIndex(), createAlias()
    // =========================================================================

    /**
     * Given Scenario: Phase 1 (dual-write, ES reads). ES has "working_T0"; OS has "working_T1".
     * When : closeIndex("working_T0") is fanned out to both providers.
     * Then : ES (primary) succeeds; OS (shadow) throws index_not_found — exception is swallowed.
     *        Both providers were called (shadow is never skipped even on primary success).
     */
    @Test
    public void test_write_phase1_osShadowIndexNotFound_isSwallowed() {
        final AtomicBoolean esCalled = new AtomicBoolean();
        final AtomicBoolean osCalled = new AtomicBoolean();

        // ES has working_T0 → operation succeeds
        final Runnable esAction = () -> esCalled.set(true);

        // OS has working_T1 → operation fails (index_not_found for T0)
        final Runnable osAction = () -> {
            osCalled.set(true);
            throw new RuntimeException(
                    "[index_not_found_exception] no such index [working_T0] — OS index is working_T1");
        };

        final PhaseRouter<Runnable> router = new PhaseRouter<>(esAction, osAction);
        setPhase(1);

        // must not throw — OS shadow failure is fire-and-forget
        router.write(Runnable::run);

        assertTrue("ES (primary) must be called",        esCalled.get());
        assertTrue("OS (shadow)  must also be called",   osCalled.get());
    }

    /**
     * Given Scenario: Phase 1. ES (primary) fails.
     * When : write() is invoked.
     * Then : exception propagates to the caller regardless of OS outcome.
     *        OS is still called (shadow must not be skipped).
     */
    @Test
    public void test_write_phase1_esPrimaryFailure_propagates() {
        final AtomicBoolean osCalled = new AtomicBoolean();

        final Runnable esAction = () -> { throw new RuntimeException("ES cluster unavailable"); };
        final Runnable osAction = () -> osCalled.set(true);

        final PhaseRouter<Runnable> router = new PhaseRouter<>(esAction, osAction);
        setPhase(1);

        assertThrows(RuntimeException.class, () -> router.write(Runnable::run));
        assertTrue("OS (shadow) must be called even when primary fails", osCalled.get());
    }

    /**
     * Given Scenario: Phase 2 (dual-write, OS reads). ES has "working_T0"; OS has "working_T1".
     * When : closeIndex("working_T0") is fanned out.
     * Then : OS is now the primary reader — its failure (index_not_found) must propagate.
     */
    @Test
    public void test_write_phase2_osPrimaryIndexNotFound_propagates() {
        // ES has T0 (it is shadow in Phase 2) → would succeed
        final Runnable esAction = () -> {};

        // OS has T1, not T0 (it is primary in Phase 2) → throws
        final Runnable osAction = () -> {
            throw new RuntimeException(
                    "[index_not_found_exception] no such index [working_T0] — OS index is working_T1");
        };

        final PhaseRouter<Runnable> router = new PhaseRouter<>(esAction, osAction);
        setPhase(2);

        assertThrows(RuntimeException.class, () -> router.write(Runnable::run));
    }

    /**
     * Given Scenario: Phase 2. Caller uses OS index name "working_T1". ES has "working_T0".
     * When : closeIndex("working_T1") is fanned out.
     * Then : OS (primary) succeeds; ES (shadow) throws index_not_found for T1 — swallowed.
     */
    @Test
    public void test_write_phase2_esShadowIndexNotFound_isSwallowed() {
        // OS has T1 (primary in Phase 2) → succeeds
        final Runnable osAction = () -> {};

        // ES has T0 (shadow in Phase 2) → T1 not found on ES
        final Runnable esAction = () -> {
            throw new RuntimeException(
                    "[index_not_found_exception] no such index [working_T1] — ES index is working_T0");
        };

        final PhaseRouter<Runnable> router = new PhaseRouter<>(esAction, osAction);
        setPhase(2);

        // must not throw — ES shadow failure is fire-and-forget in Phase 2
        router.write(Runnable::run);
    }

    /**
     * Given Scenario: Phase 3 (OS only). OS does not have the requested index.
     * When : write() is invoked.
     * Then : exception propagates (single provider, no fire-and-forget).
     *        ES must not be called.
     */
    @Test
    public void test_write_phase3_osFailure_propagates_esNeverCalled() {
        final Runnable esAction = () -> fail("ES must NOT be called in Phase 3");
        final Runnable osAction = () -> {
            throw new RuntimeException("[index_not_found_exception] no such index [working_T0]");
        };

        final PhaseRouter<Runnable> router = new PhaseRouter<>(esAction, osAction);
        setPhase(3);

        assertThrows(RuntimeException.class, () -> router.write(Runnable::run));
    }

    /**
     * Given Scenario: Phase 0 (ES only). OS would fail if called.
     * When : write() is invoked.
     * Then : only ES is called; OS is never reached.
     */
    @Test
    public void test_write_phase0_osNeverCalled() {
        final AtomicBoolean osCalled = new AtomicBoolean();
        final Runnable esAction = () -> {};
        final Runnable osAction = () -> {
            osCalled.set(true);
            throw new RuntimeException("should not reach OS in Phase 0");
        };

        final PhaseRouter<Runnable> router = new PhaseRouter<>(esAction, osAction);
        setPhase(0);

        router.write(Runnable::run);
        assertFalse("OS must NOT be called in Phase 0", osCalled.get());
    }

    // =========================================================================
    // writeBoolean() — boolean fan-out, returns primary result
    // Used by: IndexAPIImpl.delete()
    // =========================================================================

    /**
     * Given Scenario: Phase 1. ES has "working_T0" (delete returns true); OS has "working_T1".
     * When : delete("working_T0") is fanned out.
     * Then : OS throws index_not_found — swallowed; primary (ES) result {@code true} returned.
     */
    @Test
    public void test_writeBoolean_phase1_osShadowIndexNotFound_returnsEsResult() {
        // ES.delete("working_T0") = acknowledged (true)
        final Supplier<Boolean> esDelete = () -> true;

        // OS.delete("working_T0") = index_not_found
        final Supplier<Boolean> osDelete = () -> {
            throw new RuntimeException(
                    "[index_not_found_exception] no such index [working_T0]");
        };

        final PhaseRouter<Supplier<Boolean>> router = new PhaseRouter<>(esDelete, osDelete);
        setPhase(1);

        final boolean result = router.writeBoolean(Supplier::get);
        assertTrue("primary (ES) result must be returned when shadow fails", result);
    }

    /**
     * Given Scenario: Phase 1. ES.delete = false (e.g. not acknowledged); OS also fails.
     * When : delete() is fanned out.
     * Then : ES result false is returned (shadow swallowed); false is authoritative.
     */
    @Test
    public void test_writeBoolean_phase1_esReturnsFalse_shadowIgnored() {
        final Supplier<Boolean> esDelete = () -> false;
        final Supplier<Boolean> osDelete = () -> {
            throw new RuntimeException("[index_not_found_exception] working_T0");
        };

        final PhaseRouter<Supplier<Boolean>> router = new PhaseRouter<>(esDelete, osDelete);
        setPhase(1);

        final boolean result = router.writeBoolean(Supplier::get);
        assertFalse("primary (ES) false result must be preserved", result);
    }

    /**
     * Given Scenario: Phase 2. OS.delete("working_T0") throws (OS is primary).
     * When : delete() is fanned out.
     * Then : exception propagates.
     */
    @Test
    public void test_writeBoolean_phase2_osPrimaryFailure_propagates() {
        final Supplier<Boolean> esDelete = () -> true;
        final Supplier<Boolean> osDelete = () -> {
            throw new RuntimeException("[index_not_found_exception] working_T0");
        };

        final PhaseRouter<Supplier<Boolean>> router = new PhaseRouter<>(esDelete, osDelete);
        setPhase(2);

        assertThrows(RuntimeException.class, () -> router.writeBoolean(Supplier::get));
    }

    // =========================================================================
    // writeChecked() — checked exception fan-out
    // Used by: IndexAPIImpl.clearIndex(), createIndex(), updateReplicas()
    // =========================================================================

    /**
     * Given Scenario: Phase 1. OS throws a checked IOException (index_not_found).
     * When : writeChecked() is invoked.
     * Then : checked exception is swallowed; no exception reaches the caller.
     */
    @Test
    public void test_writeChecked_phase1_osShadowCheckedFailure_isSwallowed() throws Exception {
        final AtomicBoolean esCalled = new AtomicBoolean();

        final PhaseRouter<ThrowingAction> router = new PhaseRouter<>(
                () -> esCalled.set(true),   // ES succeeds
                () -> { throw new IOException("[index_not_found_exception] working_T0"); }
        );
        setPhase(1);

        // must not throw — checked exception from OS shadow is swallowed
        router.writeChecked(ThrowingAction::run);
        assertTrue("ES (primary) must be called", esCalled.get());
    }

    /**
     * Given Scenario: Phase 1. ES (primary) throws a checked IOException.
     * When : writeChecked() is invoked.
     * Then : checked exception propagates to the caller.
     */
    @Test
    public void test_writeChecked_phase1_esPrimaryCheckedFailure_propagates() {
        final PhaseRouter<ThrowingAction> router = new PhaseRouter<>(
                () -> { throw new IOException("ES index update failed"); },
                () -> {}  // OS succeeds (shadow)
        );
        setPhase(1);

        try {
            router.writeChecked(ThrowingAction::run);
            fail("expected IOException to propagate");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("ES index update failed"));
        } catch (Exception unexpected) {
            fail("unexpected exception type: " + unexpected);
        }
    }

    /**
     * Given Scenario: Phase 2. OS (primary) throws a checked IOException.
     * When : writeChecked() is invoked.
     * Then : checked exception propagates.
     */
    @Test
    public void test_writeChecked_phase2_osPrimaryCheckedFailure_propagates() {
        final PhaseRouter<ThrowingAction> router = new PhaseRouter<>(
                () -> {},  // ES (shadow in Phase 2) succeeds
                () -> { throw new IOException("[index_not_found_exception] working_T0"); }
        );
        setPhase(2);

        try {
            router.writeChecked(ThrowingAction::run);
            fail("expected IOException to propagate");
        } catch (IOException expected) {
            // correct
        } catch (Exception unexpected) {
            fail("unexpected exception type: " + unexpected);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Simulates a checked index operation (e.g. clearIndex, createIndex). */
    @FunctionalInterface
    interface ThrowingAction {
        void run() throws Exception;
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }
}

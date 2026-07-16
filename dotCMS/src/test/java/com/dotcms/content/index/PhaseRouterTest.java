package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotmarketing.exception.DotDataException;
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
    // read() / readChecked() — single-provider read with Phase-2 ES fallback
    // Used by: SearchAPIImpl.search(), searchRaw(), searchRelated()
    //
    // TC-030 (L-5): in Phase 2 OS is the read provider but ES is still active.
    // If OS throws, the router logs "OS read failed in Phase 2 — falling back to ES"
    // and retries the read against ES, so a transient OS failure never surfaces.
    // No fallback exists in phases 0/1 (read from ES) or phase 3 (ES decommissioned).
    // =========================================================================

    /**
     * Given Scenario: Phase 2 (OS reads, ES still active). OS.search() throws; ES.search() succeeds.
     * When : read() delegates the search.
     * Then : the call does NOT throw and returns the ES fallback result. Both providers are called
     *        (OS first, then ES). This is the core TC-030 safety mechanism for {@code read()}.
     */
    @Test
    public void test_read_phase2_osFailure_fallsBackToEs() {
        final AtomicBoolean osCalled = new AtomicBoolean();
        final AtomicBoolean esCalled = new AtomicBoolean();

        final Supplier<String> osRead = () -> {
            osCalled.set(true);
            throw new RuntimeException("OS read failed — index stale or unavailable");
        };
        final Supplier<String> esRead = () -> {
            esCalled.set(true);
            return "es-result";
        };

        final PhaseRouter<Supplier<String>> router = new PhaseRouter<>(esRead, osRead);
        setPhase(2);

        final String result = router.read(Supplier::get);

        assertEquals("Phase-2 read must fall back to the ES result when OS throws",
                "es-result", result);
        assertTrue("OS (read provider) must be attempted first", osCalled.get());
        assertTrue("ES (fallback) must be called after the OS failure", esCalled.get());
    }

    /**
     * Given Scenario: Phase 2. OS.search() throws a checked exception; ES.search() succeeds.
     * When : readChecked() delegates the search (this is the path {@code SearchAPIImpl.search()} uses).
     * Then : the checked call does NOT throw and returns the ES fallback result.
     */
    @Test
    public void test_readChecked_phase2_osFailure_fallsBackToEs() throws Exception {
        final AtomicBoolean osCalled = new AtomicBoolean();
        final AtomicBoolean esCalled = new AtomicBoolean();

        final PhaseRouter<CheckedSupplier<String>> router = new PhaseRouter<>(
                () -> { esCalled.set(true); return "es-result"; },
                () -> {
                    osCalled.set(true);
                    throw new DotDataException("OS read failed in Phase 2");
                });
        setPhase(2);

        final String result = router.readChecked(CheckedSupplier::get);

        assertEquals("Phase-2 checked read must fall back to ES when OS throws",
                "es-result", result);
        assertTrue("OS (read provider) must be attempted first", osCalled.get());
        assertTrue("ES (fallback) must be called after the OS failure", esCalled.get());
    }

    /**
     * Given Scenario: Phase 2. Both OS (primary read) and ES (fallback) fail.
     * When : readChecked() delegates the search.
     * Then : the fallback is exhausted — the ES exception propagates to the caller.
     */
    @Test
    public void test_readChecked_phase2_bothFail_esExceptionPropagates() {
        final PhaseRouter<CheckedSupplier<String>> router = new PhaseRouter<>(
                () -> { throw new DotDataException("ES also unavailable"); },
                () -> { throw new DotDataException("OS read failed in Phase 2"); });
        setPhase(2);

        final Exception thrown = assertThrows(Exception.class,
                () -> router.readChecked(CheckedSupplier::get));
        assertTrue("the ES (fallback) failure must be the one that surfaces",
                thrown.getMessage().contains("ES also unavailable"));
    }

    /**
     * Given Scenario: Phase 3 (OS only, ES decommissioned). OS.search() throws.
     * When : read() delegates the search.
     * Then : there is NO fallback — the OS exception propagates and ES is never contacted.
     *        Confirms the fallback is strictly Phase-2 scoped.
     */
    @Test
    public void test_read_phase3_osFailure_propagates_esNeverCalled() {
        final Supplier<String> esRead = () -> { fail("ES must NOT be called in Phase 3"); return null; };
        final Supplier<String> osRead = () -> { throw new RuntimeException("OS read failed"); };

        final PhaseRouter<Supplier<String>> router = new PhaseRouter<>(esRead, osRead);
        setPhase(3);

        assertThrows(RuntimeException.class, () -> router.read(Supplier::get));
    }

    /**
     * Given Scenario: Phase 1 (dual-write, ES reads). ES is the read provider.
     * When : read() delegates the search.
     * Then : the result comes from ES and OS is never contacted — there is no read-path fan-out
     *        and no spurious fallback in phases 0/1.
     */
    @Test
    public void test_read_phase1_readsFromEs_osNeverCalled() {
        final AtomicBoolean osCalled = new AtomicBoolean();
        final Supplier<String> esRead = () -> "es-result";
        final Supplier<String> osRead = () -> { osCalled.set(true); return "os-result"; };

        final PhaseRouter<Supplier<String>> router = new PhaseRouter<>(esRead, osRead);
        setPhase(1);

        assertEquals("Phase-1 reads must come from ES", "es-result", router.read(Supplier::get));
        assertFalse("OS must NOT be contacted on the Phase-1 read path", osCalled.get());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Simulates a checked index operation (e.g. clearIndex, createIndex). */
    @FunctionalInterface
    interface ThrowingAction {
        void run() throws Exception;
    }

    /** Simulates a checked read operation returning a value (e.g. search, searchRaw). */
    @FunctionalInterface
    interface CheckedSupplier<R> {
        R get() throws Exception;
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }
}

package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED;
import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS;
import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS;
import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.PHASE_3_OPENSEARCH_ONLY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotmarketing.util.Config;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link IndexConfigHelper.MigrationPhase}.
 *
 * <p>Verifies that each phase resolves to the correct semantic predicates and
 * that {@link MigrationPhase#current()} correctly reads the feature-flag value.</p>
 */
public class MigrationPhaseTest {

    @After
    public void clearFlag() {
        Config.setProperty(MigrationPhase.FLAG_KEY, null);
    }

    // =========================================================================
    // MigrationPhase.current() — flag resolution
    // =========================================================================

    /**
     * Given Scenario: The feature flag is not set.
     * Expected Result: {@link MigrationPhase#current()} returns PHASE_0 (safe default).
     */
    @Test
    public void test_current_whenFlagAbsent_returnsPhase0() {
        Config.setProperty(MigrationPhase.FLAG_KEY, null);

        assertSame(PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());
    }

    /**
     * Given Scenario: The feature flag is set to each valid ordinal (0–3).
     * Expected Result: {@link MigrationPhase#current()} returns the matching phase constant.
     */
    @Test
    public void test_current_withValidOrdinals_returnsCorrectPhase() {
        setPhase(0);
        assertSame(PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());

        setPhase(1);
        assertSame(PHASE_1_DUAL_WRITE_ES_READS, MigrationPhase.current());

        setPhase(2);
        assertSame(PHASE_2_DUAL_WRITE_OS_READS, MigrationPhase.current());

        setPhase(3);
        assertSame(PHASE_3_OPENSEARCH_ONLY, MigrationPhase.current());
    }

    /**
     * Given Scenario: The feature flag contains an out-of-range value (e.g. 99).
     * Expected Result: {@link MigrationPhase#current()} falls back to PHASE_0.
     */
    @Test
    public void test_current_withOutOfRangeOrdinal_fallsBackToPhase0() {
        setPhase(99);

        assertSame(PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());
    }

    /**
     * Given Scenario: The feature flag contains a negative value.
     * Expected Result: {@link MigrationPhase#current()} falls back to PHASE_0.
     */
    @Test
    public void test_current_withNegativeOrdinal_fallsBackToPhase0() {
        setPhase(-1);

        assertSame(PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());
    }

    // =========================================================================
    // PHASE_0 — migration not started
    // =========================================================================

    /**
     * Given Scenario: Active phase is PHASE_0.
     * Expected Result:
     *   - isMigrationNotStarted() == true
     *   - isDualWrite()           == false
     *   - isReadEnabled()         == false
     *   - isMigrationComplete()   == false
     */
    @Test
    public void test_phase0_predicates() {
        assertTrue(PHASE_0_MIGRATION_NOT_STARTED.isMigrationNotStarted());
        assertFalse(PHASE_0_MIGRATION_NOT_STARTED.isDualWrite());
        assertFalse(PHASE_0_MIGRATION_NOT_STARTED.isReadEnabled());
        assertFalse(PHASE_0_MIGRATION_NOT_STARTED.isMigrationComplete());
    }

    // =========================================================================
    // PHASE_1 — dual-write, reads still from ES
    // =========================================================================

    /**
     * Given Scenario: Active phase is PHASE_1.
     * Expected Result:
     *   - isMigrationNotStarted() == false
     *   - isDualWrite()           == true
     *   - isReadEnabled()         == false  (OS does NOT serve reads yet)
     *   - isMigrationComplete()   == false
     */
    @Test
    public void test_phase1_predicates() {
        assertFalse(PHASE_1_DUAL_WRITE_ES_READS.isMigrationNotStarted());
        assertTrue(PHASE_1_DUAL_WRITE_ES_READS.isDualWrite());
        assertFalse(PHASE_1_DUAL_WRITE_ES_READS.isReadEnabled());
        assertFalse(PHASE_1_DUAL_WRITE_ES_READS.isMigrationComplete());
    }

    // =========================================================================
    // PHASE_2 — dual-write + OS reads
    // =========================================================================

    /**
     * Given Scenario: Active phase is PHASE_2.
     * Expected Result:
     *   - isMigrationNotStarted() == false
     *   - isDualWrite()           == true
     *   - isReadEnabled()         == true   (OS now serves reads)
     *   - isMigrationComplete()   == false
     */
    @Test
    public void test_phase2_predicates() {
        assertFalse(PHASE_2_DUAL_WRITE_OS_READS.isMigrationNotStarted());
        assertTrue(PHASE_2_DUAL_WRITE_OS_READS.isDualWrite());
        assertTrue(PHASE_2_DUAL_WRITE_OS_READS.isReadEnabled());
        assertFalse(PHASE_2_DUAL_WRITE_OS_READS.isMigrationComplete());
    }

    // =========================================================================
    // PHASE_3 — OS only, migration complete
    // =========================================================================

    /**
     * Given Scenario: Active phase is PHASE_3.
     * Expected Result:
     *   - isMigrationNotStarted() == false
     *   - isDualWrite()           == false  (ES is gone, no dual-write)
     *   - isReadEnabled()         == true
     *   - isMigrationComplete()   == true
     */
    @Test
    public void test_phase3_predicates() {
        assertFalse(PHASE_3_OPENSEARCH_ONLY.isMigrationNotStarted());
        assertFalse(PHASE_3_OPENSEARCH_ONLY.isDualWrite());
        assertTrue(PHASE_3_OPENSEARCH_ONLY.isReadEnabled());
        assertTrue(PHASE_3_OPENSEARCH_ONLY.isMigrationComplete());
    }

    // =========================================================================
    // IndexConfigHelper static helpers
    // =========================================================================

    /**
     * Given Scenario: Each phase is active in turn.
     * Expected Result: The static helpers on IndexConfigHelper mirror the phase predicates.
     */
    @Test
    public void test_staticHelpers_matchPhasePredicates() {
        setPhase(0);
        assertTrue(IndexConfigHelper.isMigrationNotStarted());
        assertFalse(IndexConfigHelper.isDualWrite());
        assertFalse(IndexConfigHelper.isReadEnabled());
        assertFalse(IndexConfigHelper.isMigrationComplete());

        setPhase(1);
        assertFalse(IndexConfigHelper.isMigrationNotStarted());
        assertTrue(IndexConfigHelper.isDualWrite());
        assertFalse(IndexConfigHelper.isReadEnabled());
        assertFalse(IndexConfigHelper.isMigrationComplete());

        setPhase(2);
        assertFalse(IndexConfigHelper.isMigrationNotStarted());
        assertTrue(IndexConfigHelper.isDualWrite());
        assertTrue(IndexConfigHelper.isReadEnabled());
        assertFalse(IndexConfigHelper.isMigrationComplete());

        setPhase(3);
        assertFalse(IndexConfigHelper.isMigrationNotStarted());
        assertFalse(IndexConfigHelper.isDualWrite());
        assertTrue(IndexConfigHelper.isReadEnabled());
        assertTrue(IndexConfigHelper.isMigrationComplete());
    }

    // =========================================================================

    private static void setPhase(final int ordinal) {
        Config.setProperty(MigrationPhase.FLAG_KEY, String.valueOf(ordinal));
    }
}

package com.dotcms.content.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotmarketing.util.Config;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link MigrationIndexVisibility}.
 *
 * <p>The policy is purely phase-based (issue #36360 removed the role-gated preview — migration
 * detail now comes from the role-gated readiness endpoint): OS-tagged ({@code .os}) indices are
 * visible to everyone only in Phase&nbsp;3; before it they are hidden from the display sinks. No
 * user, role, or thread-local is consulted.</p>
 */
public class MigrationIndexVisibilityTest {

    private static final String ES_OPEN   = "working_20260406";
    private static final String ES_CLOSED = "cluster_ab12.live_20260101";
    private static final String OS_TAGGED = IndexTag.OS.tag("working_20260406");

    /** Mixed ES + OS list as the API would hand it to a display sink in a dual-write phase. */
    private static List<String> mixedList() {
        return Arrays.asList(ES_OPEN, OS_TAGGED, ES_CLOSED);
    }

    @After
    public void clearConfig() {
        Config.setProperty(MigrationPhase.FLAG_KEY, null);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(MigrationPhase.FLAG_KEY, String.valueOf(ordinal));
    }

    // =========================================================================
    // Phase 3 — OS is the live store, .os is visible to everyone
    // =========================================================================

    /** Phase 3: showMigrationIndices is true and filter returns the list unchanged. */
    @Test
    public void test_phase3_showsAllIncludingOsTagged() {
        setPhase(3);

        assertTrue(MigrationIndexVisibility.showMigrationIndices());

        final List<String> list = mixedList();
        assertSame("Phase 3 must return the same list instance untouched",
                list, MigrationIndexVisibility.filter(list));
    }

    // =========================================================================
    // Phases 0/1/2 — .os hidden from everyone
    // =========================================================================

    /** Phase 0: .os entries are stripped, ES entries remain. */
    @Test
    public void test_phase0_hidesOsTagged() {
        setPhase(0);

        assertFalse(MigrationIndexVisibility.showMigrationIndices());
        assertEquals(Arrays.asList(ES_OPEN, ES_CLOSED),
                MigrationIndexVisibility.filter(mixedList()));
    }

    /** Phase 1 (dual-write): .os hidden. */
    @Test
    public void test_phase1_hidesOsTagged() {
        setPhase(1);

        assertFalse(MigrationIndexVisibility.showMigrationIndices());
        assertEquals(Arrays.asList(ES_OPEN, ES_CLOSED),
                MigrationIndexVisibility.filter(mixedList()));
    }

    /** Phase 2 behaves identically to Phase 1 (still pre-complete). */
    @Test
    public void test_phase2_hidesOsTagged() {
        setPhase(2);

        assertEquals(Arrays.asList(ES_OPEN, ES_CLOSED),
                MigrationIndexVisibility.filter(mixedList()));
    }

    // =========================================================================
    // Null / empty list handling
    // =========================================================================

    /** filter with null or empty input in a hiding phase is returned as-is, no NPE. */
    @Test
    public void test_filter_nullOrEmptyList_returnedAsIs() {
        setPhase(1);

        assertSame(null, MigrationIndexVisibility.filter(null));
        final List<String> empty = Collections.emptyList();
        assertSame(empty, MigrationIndexVisibility.filter(empty));
    }
}

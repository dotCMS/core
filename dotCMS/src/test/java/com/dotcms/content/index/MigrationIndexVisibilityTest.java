package com.dotcms.content.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link MigrationIndexVisibility}.
 *
 * <p>Verifies the phase + role visibility rule for OS-tagged ({@code .os}) indices:
 * always visible in Phase&nbsp;3; otherwise hidden unless the acting user holds the
 * configured QA/preview role. The policy must fail closed when the role is absent or the
 * lookup errors, and must never resolve the user from a thread-local.</p>
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
        Config.setProperty(MigrationIndexVisibility.VISIBILITY_ROLE_KEY, null);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(MigrationPhase.FLAG_KEY, String.valueOf(ordinal));
    }

    // =========================================================================
    // Phase 3 — OS is the live store, everything is visible to everyone
    // =========================================================================

    /**
     * Given Scenario: Phase 3 (OS-only), no user supplied.
     * Expected Result: canSeeMigrationIndices is true and filter returns the list unchanged
     * (the .os entries stay), without consulting the role API.
     */
    @Test
    public void test_phase3_showsAllIncludingOsTagged_evenForNullUser() {
        setPhase(3);

        assertTrue(MigrationIndexVisibility.canSeeMigrationIndices(null));

        final List<String> list = mixedList();
        assertSame("Phase 3 must return the same list instance untouched",
                list, MigrationIndexVisibility.filter(list, null));
    }

    // =========================================================================
    // Phases 0/1/2 — .os hidden unless the user holds the QA role
    // =========================================================================

    /**
     * Given Scenario: Phase 1 (dual-write), no user (e.g. could not be resolved).
     * Expected Result: fail closed — OS-tagged entries are stripped, ES entries remain.
     */
    @Test
    public void test_phase1_nullUser_hidesOsTagged() {
        setPhase(1);

        assertFalse(MigrationIndexVisibility.canSeeMigrationIndices(null));
        assertEquals(Arrays.asList(ES_OPEN, ES_CLOSED),
                MigrationIndexVisibility.filter(mixedList(), null));
    }

    /**
     * Given Scenario: Phase 1, an admin user who does NOT hold the QA role.
     * Expected Result: OS-tagged entries are hidden.
     */
    @Test
    public void test_phase1_userWithoutRole_hidesOsTagged() throws DotDataException {
        setPhase(1);
        final User user = mock(User.class);
        final Role role = mock(Role.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(MigrationIndexVisibility.DEFAULT_VISIBILITY_ROLE_KEY))
                .thenReturn(role);
        when(roleAPI.doesUserHaveRole(user, role)).thenReturn(false);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            assertFalse(MigrationIndexVisibility.canSeeMigrationIndices(user));
            assertEquals(Arrays.asList(ES_OPEN, ES_CLOSED),
                    MigrationIndexVisibility.filter(mixedList(), user));
        }
    }

    /**
     * Given Scenario: Phase 1, a user who holds the configured QA role.
     * Expected Result: the full list (including .os) is returned.
     */
    @Test
    public void test_phase1_userWithRole_showsAll() throws DotDataException {
        setPhase(1);
        final User user = mock(User.class);
        final Role role = mock(Role.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(MigrationIndexVisibility.DEFAULT_VISIBILITY_ROLE_KEY))
                .thenReturn(role);
        when(roleAPI.doesUserHaveRole(user, role)).thenReturn(true);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            assertTrue(MigrationIndexVisibility.canSeeMigrationIndices(user));
            assertEquals(mixedList(), MigrationIndexVisibility.filter(mixedList(), user));
        }
    }

    /**
     * Given Scenario: Phase 2 behaves identically to Phase 1 (still pre-complete).
     * Expected Result: .os hidden for a user without the role.
     */
    @Test
    public void test_phase2_userWithoutRole_hidesOsTagged() throws DotDataException {
        setPhase(2);
        final User user = mock(User.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(MigrationIndexVisibility.DEFAULT_VISIBILITY_ROLE_KEY))
                .thenReturn(mock(Role.class));
        when(roleAPI.doesUserHaveRole(Mockito.eq(user), Mockito.any(Role.class)))
                .thenReturn(false);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            assertEquals(Arrays.asList(ES_OPEN, ES_CLOSED),
                    MigrationIndexVisibility.filter(mixedList(), user));
        }
    }

    // =========================================================================
    // Fail-closed behaviour
    // =========================================================================

    /**
     * Given Scenario: Phase 1 and the configured QA role does not exist (loadRoleByKey null).
     * Expected Result: fail closed — user cannot see .os indices.
     */
    @Test
    public void test_phase1_roleMissing_failsClosed() throws DotDataException {
        setPhase(1);
        final User user = mock(User.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(MigrationIndexVisibility.DEFAULT_VISIBILITY_ROLE_KEY))
                .thenReturn(null);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            assertFalse(MigrationIndexVisibility.canSeeMigrationIndices(user));
        }
    }

    /**
     * Given Scenario: Phase 1 and the role lookup throws.
     * Expected Result: the exception is swallowed and the policy fails closed.
     */
    @Test
    public void test_phase1_roleLookupThrows_failsClosed() throws DotDataException {
        setPhase(1);
        final User user = mock(User.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(MigrationIndexVisibility.DEFAULT_VISIBILITY_ROLE_KEY))
                .thenThrow(new DotDataException("boom"));

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            assertFalse(MigrationIndexVisibility.canSeeMigrationIndices(user));
        }
    }

    // =========================================================================
    // Configurable role key
    // =========================================================================

    /**
     * Given Scenario: a custom role key is configured via {@code OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY}.
     * Expected Result: the policy looks up that key, not the default, when deciding visibility.
     */
    @Test
    public void test_customRoleKey_isHonored() throws DotDataException {
        setPhase(1);
        final String customKey = "my_custom_qa_role";
        Config.setProperty(MigrationIndexVisibility.VISIBILITY_ROLE_KEY, customKey);

        final User user = mock(User.class);
        final Role role = mock(Role.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(customKey)).thenReturn(role);
        when(roleAPI.doesUserHaveRole(user, role)).thenReturn(true);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            assertTrue(MigrationIndexVisibility.canSeeMigrationIndices(user));
        }
    }

    // =========================================================================
    // Null / empty list handling
    // =========================================================================

    /**
     * Given Scenario: filter is called with null or empty input in a hiding phase.
     * Expected Result: the input is returned as-is, no NPE, no role lookup.
     */
    @Test
    public void test_filter_nullOrEmptyList_returnedAsIs() {
        setPhase(1);

        assertSame(null, MigrationIndexVisibility.filter(null, null));
        final List<String> empty = Collections.emptyList();
        assertSame(empty, MigrationIndexVisibility.filter(empty, null));
    }
}
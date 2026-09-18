package com.dotcms.rest.api.v1.experiments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ExperimentCreatorNameResolver}, the rule that turns an Experiment's
 * {@code createdBy} user ID into the display name returned as {@code createdByUserName} (#37304).
 *
 * <p>The whole value of this field is that the Experiments portlet's Created By column is
 * <b>never blank</b>. That promise is not made by the happy path — it is made by the failure
 * branches, so each one gets its own test: a user who no longer exists, a user who exists but has
 * no name set, and an infrastructure failure. All three collapse to the same answer,
 * {@code "unknown"}, and none of them may throw: a decoration failure must never turn a successful
 * experiment read into a failed request (FR-009, FR-010, FR-011).
 *
 * <p>The labels deliberately match {@code BrowserAPIImpl.ownerName}, which answers the same question
 * for the Content Drive folder view: {@code "System"} for the system user, {@code "unknown"} for
 * anyone who cannot be resolved. Two listings in the same product should not label the same
 * orphaned owner differently.
 *
 * <p>The resolver takes its {@link UserAPI} through the constructor precisely so these branches can
 * be exercised without a database; production code reaches it through {@code APILocator}.
 */
class ExperimentCreatorNameResolverTest {

    private static final String CREATOR_ID = "dotcms.org.1";
    private static final String UNKNOWN = "unknown";

    private UserAPI userAPI;
    private ExperimentCreatorNameResolver resolver;

    @BeforeEach
    void setUp() {
        userAPI = mock(UserAPI.class);
        resolver = new ExperimentCreatorNameResolver(() -> userAPI);
    }

    private static User userNamed(final String first, final String middle, final String last) {
        final User user = new User();
        user.setFirstName(first);
        user.setMiddleName(middle);
        user.setLastName(last);
        return user;
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: The creator ID resolves to a User with a first and last name.
     * ExpectedResult: The user's full name is returned.
     */
    @Test
    void resolve_userWithName_returnsFullName() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID)).thenReturn(userNamed("Admin", "", "User"));

        assertEquals("Admin User", resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: The creator resolves to a User whose first, middle and last name are all
     *                 blank. {@code User.getFullName()} joins the blank parts with a space, so it
     *                 returns {@code " "} — a single space, NOT the empty string.
     * ExpectedResult: {@code "unknown"} — the field is never blank (FR-003, FR-010). Note what
     *                 makes this work: {@code UtilMethods.isSet} trims before measuring length.
     *                 A non-trimming emptiness check would let the space through to the column.
     */
    @Test
    void resolve_userWithBlankName_fallsBackToUnknown() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID)).thenReturn(userNamed("", "", ""));

        assertEquals(UNKNOWN, resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: The creator ID points at a user that no longer exists — a deleted user, or an
     *                 orphaned reference left behind by one.
     * ExpectedResult: {@code "unknown"}, and no exception escapes (FR-009).
     */
    @Test
    void resolve_deletedUser_fallsBackToUnknown() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID))
                .thenThrow(new NoSuchUserException("No user matches " + CREATOR_ID));

        assertEquals(UNKNOWN, resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: The user lookup fails for an infrastructure reason rather than a data one.
     * ExpectedResult: {@code "unknown"}. This is the branch that keeps a database hiccup from
     *                 turning {@code GET /v1/experiments} into a 500 (FR-011).
     */
    @Test
    void resolve_lookupFailure_fallsBackToUnknownAndDoesNotThrow() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID)).thenThrow(new DotDataException("boom"));

        assertEquals(UNKNOWN, resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: An unexpected unchecked failure escapes the user layer.
     * ExpectedResult: Still {@code "unknown"}. The catch is deliberately broad because the caller
     *                 is a serializer: anything thrown here would surface as a failed API response
     *                 for a field that is only decoration.
     */
    @Test
    void resolve_unexpectedRuntimeFailure_fallsBackToUnknown() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID)).thenThrow(new IllegalStateException("unexpected"));

        assertEquals(UNKNOWN, resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: The creator has a first name but no last name (or the reverse).
     *                 {@code User.getFullName()} concatenates unconditionally with a space and
     *                 does not trim, so it hands back {@code "Admin "}.
     * ExpectedResult: {@code "Admin"} — the padding must not reach the portlet's Created By
     *                 column. This is the branch adjacent to the all-blank one, and the reason
     *                 the resolver trims rather than trusting the source.
     */
    @Test
    void resolve_userWithOnlyAFirstName_returnsTheNameWithoutPadding() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID)).thenReturn(userNamed("Admin", "", ""));

        assertEquals("Admin", resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: The user layer returns {@code null} instead of throwing. The production
     *                 {@code UserAPIImpl} throws {@link NoSuchUserException} rather than returning
     *                 null, so this pins a defensive branch reachable through any other
     *                 {@code UserAPI} implementation or decorator.
     * ExpectedResult: {@code "unknown"}, with no NullPointerException.
     */
    @Test
    void resolve_nullUser_fallsBackToUnknown() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID)).thenReturn(null);

        assertEquals(UNKNOWN, resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: The experiment was created by the system user, whose ID is {@code "system"}.
     * ExpectedResult: {@code "System"}, without consulting the user layer at all — the same
     *                 short-circuit {@code BrowserAPIImpl.ownerName} applies, so the two listings
     *                 agree on the label.
     */
    @Test
    void resolve_systemUser_returnsSystemWithoutLookup() throws Exception {
        assertEquals("System", resolver.resolve("system"));
        assertEquals("System", resolver.resolve("SYSTEM"));

        verify(userAPI, never()).loadUserById(anyString());
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: An unset creator ID. {@code AbstractExperiment.createdBy()} is a mandatory
     *                 attribute, so the model cannot actually produce this — the test pins the
     *                 defensive behaviour rather than a reachable path.
     * ExpectedResult: {@code "unknown"}, and the user layer is never consulted, so a blank ID
     *                 cannot cost a lookup or raise.
     */
    @Test
    void resolve_unsetId_returnsUnknownAndSkipsLookup() throws Exception {
        assertEquals(UNKNOWN, resolver.resolve(""));
        assertEquals(UNKNOWN, resolver.resolve(null));

        verify(userAPI, never()).loadUserById(anyString());
        verifyNoMoreInteractions(userAPI);
    }
}

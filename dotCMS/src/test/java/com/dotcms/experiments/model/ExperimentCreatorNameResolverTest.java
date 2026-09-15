package com.dotcms.experiments.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
 * no name set, and an infrastructure failure. All three collapse to the same answer, the raw user
 * ID, and none of them may throw: a decoration failure must never turn a successful experiment
 * read into a failed request (FR-009, FR-010, FR-011).
 *
 * <p>The resolver takes its {@link UserAPI} through the constructor precisely so these branches can
 * be exercised without a database; production code reaches it through {@code APILocator}.
 */
class ExperimentCreatorNameResolverTest {

    private static final String CREATOR_ID = "dotcms.org.1";

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
     *                 blank. {@code User.getFullName()} returns an empty string for that user.
     * ExpectedResult: The raw creator ID, not the empty string — the field is never empty
     *                 (FR-003, FR-010).
     */
    @Test
    void resolve_userWithBlankName_fallsBackToRawId() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID)).thenReturn(userNamed("", "", ""));

        assertEquals(CREATOR_ID, resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: The creator ID points at a user that no longer exists — a deleted user, or an
     *                 orphaned reference left behind by one.
     * ExpectedResult: The raw creator ID, and no exception escapes (FR-009).
     */
    @Test
    void resolve_deletedUser_fallsBackToRawId() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID))
                .thenThrow(new NoSuchUserException("No user matches " + CREATOR_ID));

        assertEquals(CREATOR_ID, resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: The user lookup fails for an infrastructure reason rather than a data one.
     * ExpectedResult: The raw creator ID. This is the branch that keeps a database hiccup from
     *                 turning {@code GET /v1/experiments} into a 500 (FR-011).
     */
    @Test
    void resolve_lookupFailure_fallsBackToRawIdAndDoesNotThrow() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID)).thenThrow(new DotDataException("boom"));

        assertEquals(CREATOR_ID, resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: An unexpected unchecked failure escapes the user layer.
     * ExpectedResult: Still the raw creator ID. The catch is deliberately broad because the caller
     *                 is a serializer: anything thrown here would surface as a failed API response
     *                 for a field that is only decoration.
     */
    @Test
    void resolve_unexpectedRuntimeFailure_fallsBackToRawId() throws Exception {
        when(userAPI.loadUserById(CREATOR_ID)).thenThrow(new IllegalStateException("unexpected"));

        assertEquals(CREATOR_ID, resolver.resolve(CREATOR_ID));
    }

    /**
     * Method to test: {@link ExperimentCreatorNameResolver#resolve(String)}
     * Given Scenario: An unset creator ID. {@code AbstractExperiment.createdBy()} is a mandatory
     *                 attribute, so the model cannot actually produce this — the test pins the
     *                 defensive behaviour rather than a reachable path.
     * ExpectedResult: The input is returned unchanged and the user layer is never consulted, so a
     *                 blank ID cannot cost a lookup or raise.
     */
    @Test
    void resolve_unsetId_returnsInputAndSkipsLookup() throws Exception {
        assertEquals("", resolver.resolve(""));
        assertNotNull(resolver.resolve(""));

        verify(userAPI, org.mockito.Mockito.never()).loadUserById(anyString());
        verifyNoMoreInteractions(userAPI);
    }
}

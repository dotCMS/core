package com.dotmarketing.portlets.folders.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import com.liferay.portal.model.User;
import org.junit.Test;

/**
 * Unit tests for the required-field invariants of {@link FolderSearchParams}.
 *
 * <p>The point of these tests is <em>where</em> the checks are enforced, not merely that they exist. A
 * record's canonical constructor cannot be declared less accessible than the record itself, so for a
 * {@code public record} there is always a public positional entry point and
 * {@link FolderSearchParams#builder()} can only ever be a convenience. A check placed in
 * {@code Builder.build()} guards the callers who happen to use the builder; a check in the canonical
 * constructor guards every construction path, the builder's own included.</p>
 *
 * <p>{@link #test_directConstruction_enforcesRequiredFields()} is therefore the test that matters: it
 * would have passed silently before the checks moved.</p>
 *
 * @author Fabrizio Araya
 */
public class FolderSearchParamsTest {

    private static final String SITE_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    /**
     * Method to test: {@link FolderSearchParams#FolderSearchParams(String, String, boolean, String, User, boolean, int, int, String, String, boolean)}
     * Given scenario: the canonical constructor is called directly, bypassing the builder, with a
     * missing {@code siteId} and then with a missing {@code user}.
     * Expected result: it rejects both. This is the case a check in {@code Builder.build()} cannot
     * cover, and the reason the invariant belongs on the constructor.
     */
    @Test
    public void test_directConstruction_enforcesRequiredFields() {
        final NullPointerException noSite = assertThrows(NullPointerException.class,
                () -> new FolderSearchParams("name", "/", false, null, new User(), false,
                        40, 0, "folder.name", "ASC", false));
        assertEquals("siteId is required", noSite.getMessage());

        final NullPointerException noUser = assertThrows(NullPointerException.class,
                () -> new FolderSearchParams("name", "/", false, SITE_ID, null, false,
                        40, 0, "folder.name", "ASC", false));
        assertEquals("user is required", noUser.getMessage());
    }

    /**
     * Method to test: {@link FolderSearchParams.Builder#build()}
     * Given scenario: the builder is used without setting {@code siteId}.
     * Expected result: it still rejects, now because it delegates to the canonical constructor rather
     * than because it carries its own copy of the check. Same exception type and message as before, so
     * the behaviour callers see is unchanged.
     */
    @Test
    public void test_builderWithoutSiteId_stillRejects() {
        final NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> FolderSearchParams.builder().user(new User()).build());

        assertEquals("siteId is required", thrown.getMessage());
    }

    /**
     * Method to test: {@link FolderSearchParams.Builder#build()}
     * Given scenario: the builder is used without setting {@code user}.
     * Expected result: rejected, with the message unchanged.
     */
    @Test
    public void test_builderWithoutUser_stillRejects() {
        final NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> FolderSearchParams.builder().siteId(SITE_ID).build());

        assertEquals("user is required", thrown.getMessage());
    }

    /**
     * Method to test: {@link FolderSearchParams.Builder#build()}
     * Given scenario: only the two required fields are set.
     * Expected result: the builder's documented defaults survive the move — nothing about them was
     * folded into the constructor.
     */
    @Test
    public void test_builderDefaults_areUnchanged() {
        final User user = new User();
        final FolderSearchParams params = FolderSearchParams.builder()
                .siteId(SITE_ID)
                .user(user)
                .build();

        assertEquals(SITE_ID, params.siteId());
        // assertSame, not assertEquals: User.equals() dereferences a primary key a bare User lacks.
        assertSame(user, params.user());
        assertEquals("/", params.path());
        assertEquals(false, params.recursive());
        assertEquals(false, params.respectFrontendRoles());
        assertEquals(40, params.limit());
        assertEquals(0, params.offset());
        assertEquals("folder.name", params.orderBy());
        assertEquals("ASC", params.orderDirection());
        assertEquals(false, params.includePermissions());
    }

    /**
     * Method to test: {@link FolderSearchParams#FolderSearchParams(String, String, boolean, String, User, boolean, int, int, String, String, boolean)}
     * Given scenario: an optional component is left null.
     * Expected result: accepted. Only {@code siteId} and {@code user} are required, so the move must
     * not have tightened anything else — {@code name} being null is how "no name filter" is expressed.
     */
    @Test
    public void test_optionalComponentsMayBeNull() {
        final FolderSearchParams params = new FolderSearchParams(null, "/", false, SITE_ID,
                new User(), false, 40, 0, "folder.name", "ASC", false);

        assertEquals(null, params.name());
    }
}

package com.dotcms.rest.api.v1.system.role;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the {@code childCount} / {@code userCount} fields on {@link RoleView}
 * and the backing {@link RoleAPI#countUsersByRoleIds(java.util.Collection)} aggregate.
 * See https://github.com/dotCMS/core/issues/37071
 *
 * Created roles and users are tracked and removed once in {@link #cleanUp()} instead of
 * per-test: every role deletion clears the global role cache (and user deletion cascades
 * through the user's own role), which can race the REST auth check of the next test in the
 * class and surface as a spurious 401. Class-level cleanup leaves the suite just as clean
 * without interleaving deletions between REST calls.
 *
 * @author hassandotcms
 */
public class RoleResourceCountsIntegrationTest {

    static HttpServletResponse response;
    static RoleResource resource;
    static RoleAPI roleAPI;

    private static final List<Role> rolesToClean = new ArrayList<>();
    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new RoleResource();
        roleAPI = APILocator.getRoleAPI();
        response = new MockHttpResponse();
    }

    @AfterClass
    public static void cleanUp() {
        usersToClean.forEach(UserDataGen::remove);
        // children were created after their parents, so remove in reverse creation order
        Collections.reverse(rolesToClean);
        rolesToClean.forEach(RoleDataGen::remove);
    }

    private static Role track(final Role role) {
        rolesToClean.add(role);
        return role;
    }

    private static User track(final User user) {
        usersToClean.add(user);
        return user;
    }

    private static HttpServletRequest mockAdminRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        return request;
    }

    @SuppressWarnings("unchecked")
    private static RoleView loadRoleView(final String roleId, final boolean loadChildren)
            throws Exception {
        final Response restResponse = resource.loadRoleByRoleId(
                mockAdminRequest(), response, roleId, loadChildren);
        assertEquals(Response.Status.OK.getStatusCode(), restResponse.getStatus());
        final ResponseEntityRoleDetailView view =
                (ResponseEntityRoleDetailView) restResponse.getEntity();
        return view.getEntity();
    }

    /**
     * Given a role with N direct children, when it is loaded through the endpoint,
     * then childCount equals N regardless of the loadChildrenRoles flag.
     */
    @Test
    public void childCount_matchesDirectChildren() throws Exception {
        final Role parent = track(new RoleDataGen().nextPersisted());
        final Role childA = track(new RoleDataGen().parent(parent.getId()).nextPersisted());
        final Role childB = track(new RoleDataGen().parent(parent.getId()).nextPersisted());
        final Role childC = track(new RoleDataGen().parent(parent.getId()).nextPersisted());

        final RoleView withChildren = loadRoleView(parent.getId(), true);
        assertEquals(3, withChildren.getChildCount());
        assertEquals(3, withChildren.getRoleChildren().size());

        final RoleView withoutChildren = loadRoleView(parent.getId(), false);
        assertEquals("childCount must not depend on children hydration",
                3, withoutChildren.getChildCount());
        assertTrue(withoutChildren.getRoleChildren().isEmpty());

        assertEquals("a leaf role must report zero children",
                0, loadRoleView(childA.getId(), true).getChildCount());
        assertNotNull(childB);
        assertNotNull(childC);
    }

    /**
     * Given a role with M directly granted users, userCount equals M.
     */
    @Test
    public void userCount_matchesDirectGrants() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        final User userA = track(new UserDataGen().roles(role).nextPersisted());
        final User userB = track(new UserDataGen().roles(role).nextPersisted());

        assertNotNull(userA);
        assertNotNull(userB);
        assertEquals(2, loadRoleView(role.getId(), false).getUserCount());
    }

    /**
     * Grants are direct-only in both directions: a grant on the parent does not count
     * for the child, and a grant on the child does not count for the parent.
     */
    @Test
    public void userCount_excludesInheritedGrants() throws Exception {
        final Role parent = track(new RoleDataGen().nextPersisted());
        final Role child = track(new RoleDataGen().parent(parent.getId()).nextPersisted());
        final User parentUser = track(new UserDataGen().roles(parent).nextPersisted());
        assertNotNull(parentUser);

        assertEquals(1, loadRoleView(parent.getId(), false).getUserCount());
        assertEquals("a grant on the parent must not count for the child",
                0, loadRoleView(child.getId(), false).getUserCount());

        final User childUser = track(new UserDataGen().roles(child).nextPersisted());
        assertNotNull(childUser);
        assertEquals("a grant on the child must not count for the parent",
                1, loadRoleView(parent.getId(), false).getUserCount());
        assertEquals(1, loadRoleView(child.getId(), false).getUserCount());
    }

    /**
     * A role with no children and no grants reports 0 for both fields.
     */
    @Test
    public void counts_zeroForEmptyRole() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        final RoleView view = loadRoleView(role.getId(), true);
        assertEquals(0, view.getChildCount());
        assertEquals(0, view.getUserCount());
    }

    /**
     * When children are hydrated, each child view carries its own counts too.
     */
    @Test
    public void childViews_carryTheirOwnCounts() throws Exception {
        final Role parent = track(new RoleDataGen().nextPersisted());
        final Role child = track(new RoleDataGen().parent(parent.getId()).nextPersisted());
        final Role grandChild = track(new RoleDataGen().parent(child.getId()).nextPersisted());
        final User childUser = track(new UserDataGen().roles(child).nextPersisted());
        assertNotNull(grandChild);
        assertNotNull(childUser);

        final RoleView parentView = loadRoleView(parent.getId(), true);
        assertEquals(1, parentView.getRoleChildren().size());
        final RoleView childView = parentView.getRoleChildren().get(0);
        assertEquals(child.getId(), childView.getId());
        assertEquals(1, childView.getChildCount());
        assertEquals(1, childView.getUserCount());
    }

    /**
     * The root-roles listing carries both fields on every view.
     */
    @Test
    public void loadRootRoles_carriesCounts() throws Exception {
        final Role rootRole = track(new RoleDataGen().nextPersisted());
        final Role child = track(new RoleDataGen().parent(rootRole.getId()).nextPersisted());
        final User granted = track(new UserDataGen().roles(rootRole).nextPersisted());
        assertNotNull(child);
        assertNotNull(granted);

        final Response restResponse = resource.loadRootRoles(mockAdminRequest(), response, false);
        assertEquals(Response.Status.OK.getStatusCode(), restResponse.getStatus());
        final ResponseEntityRoleViewListView view =
                (ResponseEntityRoleViewListView) restResponse.getEntity();
        final RoleView rootView = view.getEntity().stream()
                .filter(roleView -> rootRole.getId().equals(roleView.getId()))
                .findFirst().orElse(null);
        assertNotNull("the created root role must be in the listing", rootView);
        assertEquals(1, rootView.getChildCount());
        assertEquals(1, rootView.getUserCount());
    }

    /**
     * The roles-of-a-user listing carries both fields; the user's own user-role
     * reports exactly one direct grant (the creation-time self-grant), and roles
     * returned by loadRolesForUser carry a hydrated childCount.
     */
    @Test
    public void loadUserRoles_carriesCounts() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        final Role child = track(new RoleDataGen().parent(role.getId()).nextPersisted());
        final User user = track(new UserDataGen().roles(role).nextPersisted());
        final Role userRole = roleAPI.getUserRole(user);
        assertNotNull(child);

        final ResponseEntityRoleViewListView view = resource.loadUserRoles(
                mockAdminRequest(), response, user.getUserId());
        final List<RoleView> roleViews = view.getEntity();
        assertFalse(roleViews.isEmpty());

        final RoleView grantedView = roleViews.stream()
                .filter(roleView -> role.getId().equals(roleView.getId()))
                .findFirst().orElse(null);
        assertNotNull("the granted role must be listed", grantedView);
        assertEquals(1, grantedView.getUserCount());
        assertEquals("roles from loadRolesForUser must carry a hydrated childCount",
                1, grantedView.getChildCount());

        final RoleView userRoleView = roleViews.stream()
                .filter(roleView -> userRole.getId().equals(roleView.getId()))
                .findFirst().orElse(null);
        assertNotNull("the user's own user-role must be listed", userRoleView);
        assertEquals("a user-role holds exactly its self-grant", 1, userRoleView.getUserCount());
    }

    /**
     * userCount matches the users the companion GET /{roleid}/users endpoint actually
     * returns: hidden users (the system user, users flagged delete_in_progress) are
     * excluded from the count exactly like they are excluded from the listing, so the
     * tree badge always equals the Users tab total.
     */
    @Test
    public void userCount_matchesVisibleUsersOnly() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        final User visible = track(new UserDataGen().roles(role).nextPersisted());
        final User deleting = track(new UserDataGen().roles(role).nextPersisted());
        roleAPI.addRoleToUser(role, APILocator.systemUser());
        new DotConnect().setSQL("update user_ set delete_in_progress = "
                        + DbConnectionFactory.getDBTrue() + " where userid = ?")
                .addParam(deleting.getUserId()).loadResult();
        try {
            assertEquals("hidden users must not be counted",
                    1, loadRoleView(role.getId(), false).getUserCount());

            final ResponseEntityPaginatedDataView usersView = resource.loadUsersByRoleId(
                    mockAdminRequest(), new MockHttpResponse(), role.getId(),
                    null, 1, 40, null, "ASC");
            assertEquals("the badge must equal the users listing total",
                    (long) loadRoleView(role.getId(), false).getUserCount(),
                    usersView.getPagination().getTotalEntries());
            assertNotNull(visible);
        } finally {
            // reset the flag so the class-level cleanup can remove this user normally
            new DotConnect().setSQL("update user_ set delete_in_progress = "
                            + DbConnectionFactory.getDBFalse() + " where userid = ?")
                    .addParam(deleting.getUserId()).loadResult();
        }
    }

    /**
     * Security: hostile SQL payloads passed as role ids are inert. The IN-list is built
     * from constant "?" placeholders and every id is bound as a PreparedStatement
     * parameter, so metacharacters, stacked statements, tautologies and UNIONs must be
     * treated as data: the query returns nothing for them, throws nothing, and the
     * tables remain intact for subsequent legitimate queries.
     * Covers the Semgrep CUSTOM_INJECTION-2 findings on countUsersByRoleIds.
     */
    @Test
    public void countUsersByRoleIds_hostileIdsAreInert() throws Exception {
        final Role legit = track(new RoleDataGen().nextPersisted());
        final User granted = track(new UserDataGen().roles(legit).nextPersisted());
        assertNotNull(granted);

        final Map<String, Integer> counts = roleAPI.countUsersByRoleIds(List.of(
                "'; drop table users_cms_roles; --",
                "x' OR '1'='1",
                "?) union select userid, 1 from user_ --",
                "1; update user_ set delete_in_progress = true; --",
                legit.getId()));

        assertEquals("hostile ids must match nothing, legit id must still count",
                1, counts.size());
        assertEquals(Integer.valueOf(1), counts.get(legit.getId()));

        final Map<String, Integer> after = roleAPI.countUsersByRoleIds(List.of(legit.getId()));
        assertEquals("tables must be intact after the hostile call",
                Integer.valueOf(1), after.get(legit.getId()));
    }

    /**
     * The aggregate itself: one call resolves counts for many roles; ids without
     * grants are absent from the map; empty input returns an empty map.
     */
    @Test
    public void countUsersByRoleIds_batchesCorrectly() throws Exception {
        final Role grantedTwice = track(new RoleDataGen().nextPersisted());
        final Role grantedOnce = track(new RoleDataGen().nextPersisted());
        final Role neverGranted = track(new RoleDataGen().nextPersisted());
        track(new UserDataGen().roles(grantedTwice).nextPersisted());
        track(new UserDataGen().roles(grantedTwice).nextPersisted());
        track(new UserDataGen().roles(grantedOnce).nextPersisted());

        final Map<String, Integer> counts = roleAPI.countUsersByRoleIds(
                List.of(grantedTwice.getId(), grantedOnce.getId(), neverGranted.getId()));
        assertEquals(Integer.valueOf(2), counts.get(grantedTwice.getId()));
        assertEquals(Integer.valueOf(1), counts.get(grantedOnce.getId()));
        assertFalse("ids with no grants must be absent",
                counts.containsKey(neverGranted.getId()));

        assertTrue(roleAPI.countUsersByRoleIds(List.of()).isEmpty());
    }
}

package com.dotcms.rest.api.v1.system.role;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Base64;
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
 * @author hassandotcms
 */
public class RoleResourceCountsIntegrationTest {

    static HttpServletResponse response;
    static RoleResource resource;
    static RoleAPI roleAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new RoleResource();
        roleAPI = APILocator.getRoleAPI();
        response = new MockHttpResponse();
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
        final Role parent = new RoleDataGen().nextPersisted();
        final Role childA = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final Role childB = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final Role childC = new RoleDataGen().parent(parent.getId()).nextPersisted();

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
        final Role role = new RoleDataGen().nextPersisted();
        final User userA = new UserDataGen().roles(role).nextPersisted();
        final User userB = new UserDataGen().roles(role).nextPersisted();

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
        final Role parent = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final User parentUser = new UserDataGen().roles(parent).nextPersisted();
        assertNotNull(parentUser);

        assertEquals(1, loadRoleView(parent.getId(), false).getUserCount());
        assertEquals("a grant on the parent must not count for the child",
                0, loadRoleView(child.getId(), false).getUserCount());

        final User childUser = new UserDataGen().roles(child).nextPersisted();
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
        final Role role = new RoleDataGen().nextPersisted();
        final RoleView view = loadRoleView(role.getId(), true);
        assertEquals(0, view.getChildCount());
        assertEquals(0, view.getUserCount());
    }

    /**
     * When children are hydrated, each child view carries its own counts too.
     */
    @Test
    public void childViews_carryTheirOwnCounts() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final Role grandChild = new RoleDataGen().parent(child.getId()).nextPersisted();
        final User childUser = new UserDataGen().roles(child).nextPersisted();
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
        final Role rootRole = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(rootRole.getId()).nextPersisted();
        final User granted = new UserDataGen().roles(rootRole).nextPersisted();
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
     * reports exactly one direct grant (the creation-time self-grant).
     */
    @Test
    public void loadUserRoles_carriesCounts() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Role userRole = roleAPI.getUserRole(user);

        final ResponseEntityRoleViewListView view = resource.loadUserRoles(
                mockAdminRequest(), response, user.getUserId());
        final List<RoleView> roleViews = view.getEntity();
        assertFalse(roleViews.isEmpty());

        final RoleView grantedView = roleViews.stream()
                .filter(roleView -> role.getId().equals(roleView.getId()))
                .findFirst().orElse(null);
        assertNotNull("the granted role must be listed", grantedView);
        assertEquals(1, grantedView.getUserCount());

        final RoleView userRoleView = roleViews.stream()
                .filter(roleView -> userRole.getId().equals(roleView.getId()))
                .findFirst().orElse(null);
        assertNotNull("the user's own user-role must be listed", userRoleView);
        assertEquals("a user-role holds exactly its self-grant", 1, userRoleView.getUserCount());
    }

    /**
     * The aggregate itself: one call resolves counts for many roles; ids without
     * grants are absent from the map; empty input returns an empty map.
     */
    @Test
    public void countUsersByRoleIds_batchesCorrectly() throws Exception {
        final Role grantedTwice = new RoleDataGen().nextPersisted();
        final Role grantedOnce = new RoleDataGen().nextPersisted();
        final Role neverGranted = new RoleDataGen().nextPersisted();
        new UserDataGen().roles(grantedTwice).nextPersisted();
        new UserDataGen().roles(grantedTwice).nextPersisted();
        new UserDataGen().roles(grantedOnce).nextPersisted();

        final Map<String, Integer> counts = roleAPI.countUsersByRoleIds(
                List.of(grantedTwice.getId(), grantedOnce.getId(), neverGranted.getId()));
        assertEquals(Integer.valueOf(2), counts.get(grantedTwice.getId()));
        assertEquals(Integer.valueOf(1), counts.get(grantedOnce.getId()));
        assertFalse("ids with no grants must be absent",
                counts.containsKey(neverGranted.getId()));

        assertTrue(roleAPI.countUsersByRoleIds(List.of()).isEmpty());
    }
}

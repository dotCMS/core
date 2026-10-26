package com.dotcms.rest.api.v1.system.role;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowActionDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.datagen.WorkflowStepDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration tests for the privilege that guards
 * {@code GET /v1/roles/{roleid}/rolehierarchyanduserroles}: who may read the users that hold a role.
 *
 * Created roles and users are removed once in {@link #cleanUp()}; every role deletion clears the
 * global role cache, which can race the REST auth check of the next test if done per test.
 */
public class RoleResourceMembershipIntegrationTest {

    private static RoleResource resource;
    private static RoleAPI roleAPI;

    private static final List<Role> rolesToClean = new ArrayList<>();
    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new RoleResource();
        roleAPI = APILocator.getRoleAPI();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        usersToClean.forEach(UserDataGen::remove);
        // children were created after their parents, so remove in reverse creation order
        Collections.reverse(rolesToClean);
        rolesToClean.forEach(RoleDataGen::remove);
        // the deletions above cleared the global role cache; resolve the back-end role again
        // so the next suite class's REST auth check never starts against a cold cache
        APILocator.getRoleAPI().loadBackEndUserRole();
    }

    private static Role track(final Role role) {
        rolesToClean.add(role);
        return role;
    }

    private static User track(final User user) {
        usersToClean.add(user);
        return user;
    }

    private static HttpServletRequest requestFor(final User user) {
        final MockSessionRequest request = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request());
        request.getSession().setAttribute(
                com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        return request.request();
    }

    /**
     * A back-end user with no portlets and no workflow action that assigns to the role cannot
     * read the role's hierarchy and members.
     */
    @Test(expected = ForbiddenException.class)
    public void hierarchy_plainBackendUser_isForbidden() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        final User plain = track(new UserDataGen()
                .roles(roleAPI.loadBackEndUserRole()).nextPersisted());

        resource.loadUsersAndRolesByRoleId(requestFor(plain), new MockHttpResponse(),
                role.getId(), false, null);
    }

    /**
     * A back-end user who may use a workflow action that assigns work to the role must be able
     * to pick an assignee, so the role's members stay visible to them.
     */
    @Test
    public void hierarchy_assignerOfTheRole_isAllowed() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        track(new UserDataGen().roles(role).nextPersisted());
        final Role assignerRole = track(new RoleDataGen().nextPersisted());
        final User assigner = track(new UserDataGen()
                .roles(roleAPI.loadBackEndUserRole(), assignerRole).nextPersisted());

        final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
        final WorkflowStep step = new WorkflowStepDataGen(scheme.getId()).nextPersisted();
        final WorkflowAction action = new WorkflowActionDataGen(scheme.getId(), step.getId())
                .nextAssign(role.getId()).nextPersisted();
        APILocator.getPermissionAPI().save(
                new Permission(action.getId(), assignerRole.getId(), PermissionAPI.PERMISSION_USE),
                action, APILocator.systemUser(), false);

        final Response response = resource.loadUsersAndRolesByRoleId(requestFor(assigner),
                new MockHttpResponse(), role.getId(), false, null);

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }

    /**
     * A back-end user who may read role membership (Roles plus Users portlets) can read the
     * role's hierarchy and members.
     */
    @Test
    public void hierarchy_rolesAndUsersPortlets_isAllowed() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        final Layout layout = new LayoutDataGen().portletIds("roles", "users").nextPersisted();
        final Role portletRole = track(new RoleDataGen().layout(layout).nextPersisted());
        final User delegate = track(new UserDataGen()
                .roles(roleAPI.loadBackEndUserRole(), portletRole).nextPersisted());

        final Response response = resource.loadUsersAndRolesByRoleId(requestFor(delegate),
                new MockHttpResponse(), role.getId(), false, null);

        assertEquals(200, response.getStatus());
    }
}

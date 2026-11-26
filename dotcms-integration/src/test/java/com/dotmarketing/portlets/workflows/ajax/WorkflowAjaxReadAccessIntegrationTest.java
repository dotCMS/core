package com.dotmarketing.portlets.workflows.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowActionDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.datagen.WorkflowStepDataGen;
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
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Checks, against real layouts, roles and permissions, which back-end users can read the role,
 * assignee and step lists served by {@link WfRoleStoreAjax} and {@link WfStepAjax}.
 */
public class WorkflowAjaxReadAccessIntegrationTest extends IntegrationTestBase {

    private static final List<User> users = new ArrayList<>();
    private static final List<Role> roles = new ArrayList<>();
    private static final List<Layout> layouts = new ArrayList<>();

    private static RoleAPI roleAPI;
    private static WorkflowScheme scheme;
    private static WorkflowStep step;
    private static Role assigneeRole;
    private static Role actionRole;
    private static WorkflowAction restrictedAction;
    private static WorkflowAction contentRoleAction;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        roleAPI = APILocator.getRoleAPI();

        assigneeRole = track(newRole().nextPersisted());
        track(new UserDataGen().roles(roleAPI.loadBackEndUserRole(), assigneeRole).nextPersisted());
        actionRole = track(newRole().nextPersisted());

        scheme = new WorkflowDataGen().nextPersisted();
        step = new WorkflowStepDataGen(scheme.getId()).nextPersisted();
        restrictedAction = assignableAction();
        grantUse(restrictedAction, actionRole);
        contentRoleAction = assignableAction();
        grantUse(contentRoleAction, roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY));
    }

    /**
     * Deletes the scheme, users, roles and layouts created by the class; a failure on one does
     * not strand the others.
     */
    @AfterClass
    public static void cleanUp() {
        try {
            WorkflowDataGen.remove(scheme, false);
        } catch (final Exception e) {
            Logger.warn(WorkflowAjaxReadAccessIntegrationTest.class,
                    "Unable to delete test scheme: " + e.getMessage());
        }
        for (final User user : users) {
            try {
                APILocator.getUserAPI().delete(user, TestUserUtils.getAdminUser(),
                        APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(WorkflowAjaxReadAccessIntegrationTest.class,
                        "Unable to delete test user: " + e.getMessage());
            }
        }
        roles.forEach(RoleDataGen::remove);
        for (final Layout layout : layouts) {
            try {
                APILocator.getLayoutAPI()
                        .removeLayout(APILocator.getLayoutAPI().findLayout(layout.getId()));
            } catch (final Exception e) {
                Logger.warn(WorkflowAjaxReadAccessIntegrationTest.class,
                        "Unable to delete test layout: " + e.getMessage());
            }
        }
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#action(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user whose only portlet is the Site Browser
     * Expected: 403 and no role list
     */
    @Test
    public void roleList_withoutRolePickerPortlet_isForbidden() throws Exception {
        final Result result = roleList(userWithPortlet("site-browser"), Map.of());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, result.status);
        assertEquals("", result.body);
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#action(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user whose only portlet hosts a role picker, for each such portlet
     * Expected: the role list is served
     */
    @Test
    public void roleList_withEachRolePickerPortlet_isServed() throws Exception {
        for (final String portletId : List.of("workflow-schemes", "workflow", "configuration",
                "publishing-queue")) {
            final Result result = roleList(userWithPortlet(portletId), Map.of());

            assertEquals(portletId, 0, result.status);
            assertTrue(portletId, result.body.contains("\"items\""));
        }
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#action(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user without a role picker portlet asks for one role by id
     * Expected: 403 and no role name
     */
    @Test
    public void roleLookupById_withoutRolePickerPortlet_isForbidden() throws Exception {
        final Result result = roleList(userWithPortlet("site-browser"),
                Map.of("roleId", assigneeRole.getId()));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, result.status);
        assertFalse(result.body.contains(assigneeRole.getId()));
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#action(HttpServletRequest, HttpServletResponse)}
     * Given: a CMS administrator
     * Expected: the role list is served
     */
    @Test
    public void roleList_cmsAdministrator_isServed() throws Exception {
        final Result result = roleList(TestUserUtils.getAdminUser(), Map.of());

        assertEquals(0, result.status);
        assertTrue(result.body.contains("\"items\""));
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#assignable(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user without USE on an action restricted to another role
     * Expected: 403 and no assignees
     */
    @Test
    public void assignees_actionRestrictedToAnotherRole_isForbidden() throws Exception {
        final Result result = assignees(userWithPortlet("site-browser"), restrictedAction.getId());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, result.status);
        assertFalse(result.body.contains(assigneeRole.getId()));
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#assignable(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user holding the role that may use the action
     * Expected: the action's assignees are served
     */
    @Test
    public void assignees_userWhoCanUseAction_isServed() throws Exception {
        final User user = track(new UserDataGen()
                .roles(roleAPI.loadBackEndUserRole(), actionRole).nextPersisted());

        final Result result = assignees(user, restrictedAction.getId());

        assertEquals(0, result.status);
        assertTrue(result.body.contains(assigneeRole.getId()));
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#assignable(HttpServletRequest, HttpServletResponse)}
     * Given: an action whose "who can use" is "Anyone who can Edit Content", asked for by a
     * back-end user with no special role
     * Expected: the action's assignees are served, since that role is resolved per content
     */
    @Test
    public void assignees_actionOpenToContentRoles_isServed() throws Exception {
        final Result result = assignees(userWithPortlet("site-browser"), contentRoleAction.getId());

        assertEquals(0, result.status);
        assertTrue(result.body.contains(assigneeRole.getId()));
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#assignable(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user whose only portlet is Workflow Schemes, on an action restricted to
     * another role
     * Expected: the action's assignees are served
     */
    @Test
    public void assignees_workflowSchemesUser_isServed() throws Exception {
        final Result result = assignees(userWithPortlet("workflow-schemes"), restrictedAction.getId());

        assertEquals(0, result.status);
        assertTrue(result.body.contains(assigneeRole.getId()));
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#assignable(HttpServletRequest, HttpServletResponse)}
     * Given: a front-end user, on an action open to "Anyone who can Edit Content"
     * Expected: 403 and no assignees
     */
    @Test
    public void assignees_frontEndUser_isForbidden() throws Exception {
        final User frontEndUser = track(new UserDataGen()
                .roles(roleAPI.loadFrontEndUserRole()).nextPersisted());

        final Result result = assignees(frontEndUser, contentRoleAction.getId());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, result.status);
        assertFalse(result.body.contains(assigneeRole.getId()));
    }

    /**
     * Method to test: {@link WfRoleStoreAjax#assignable(HttpServletRequest, HttpServletResponse)}
     * Given: an action id that matches no action
     * Expected: the same 403 as for an action the user may not use
     */
    @Test
    public void assignees_unknownAction_isForbidden() throws Exception {
        final Result result = assignees(userWithPortlet("site-browser"),
                "00000000-0000-0000-0000-000000000000");

        assertEquals(HttpServletResponse.SC_FORBIDDEN, result.status);
    }

    /**
     * Method to test: {@link WfStepAjax#listByScheme(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user whose only portlet is Workflow Tasks
     * Expected: the scheme's steps are served
     */
    @Test
    public void stepList_workflowPortlet_isServed() throws Exception {
        final Result result = stepList(userWithPortlet("workflow"), scheme.getId());

        assertEquals(0, result.status);
        assertTrue(result.body.contains(step.getId()));
    }

    /**
     * Method to test: {@link WfStepAjax#listByScheme(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user whose only portlet is Workflow Schemes, and a CMS administrator
     * Expected: the scheme's steps are served to both
     */
    @Test
    public void stepList_workflowSchemesUserAndAdministrator_areServed() throws Exception {
        for (final User user : List.of(userWithPortlet("workflow-schemes"),
                TestUserUtils.getAdminUser())) {
            final Result result = stepList(user, scheme.getId());

            assertEquals(user.getUserId(), 0, result.status);
            assertTrue(user.getUserId(), result.body.contains(step.getId()));
        }
    }

    /**
     * Method to test: {@link WfStepAjax#listByScheme(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user whose only portlet is Configuration
     * Expected: 403 and no steps
     */
    @Test
    public void stepList_configurationOnly_isForbidden() throws Exception {
        final Result result = stepList(userWithPortlet("configuration"), scheme.getId());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, result.status);
        assertFalse(result.body.contains(step.getId()));
    }

    /**
     * Status sent with {@code sendError} (0 when none) and the body written.
     */
    private static final class Result {
        private int status;
        private String body = "";
    }

    private static Result roleList(final User user, final Map<String, String> params)
            throws Exception {
        final WfRoleStoreAjax roleStore = new WfRoleStoreAjax();
        roleStore.setUser(user);
        return call(params, (request, response) -> roleStore.action(request, response));
    }

    private static Result assignees(final User user, final String actionId) throws Exception {
        final WfRoleStoreAjax roleStore = new WfRoleStoreAjax();
        roleStore.setUser(user);
        return call(Map.of("actionId", actionId),
                (request, response) -> roleStore.assignable(request, response));
    }

    private static Result stepList(final User user, final String schemeId) throws Exception {
        final WfStepAjax stepAjax = new WfStepAjax();
        stepAjax.setUser(user);
        return call(Map.of("schemeId", schemeId),
                (request, response) -> stepAjax.listByScheme(request, response));
    }

    @FunctionalInterface
    private interface AjaxCall {
        void run(HttpServletRequest request, HttpServletResponse response) throws Exception;
    }

    private static Result call(final Map<String, String> params, final AjaxCall ajaxCall)
            throws Exception {
        final Map<String, String> parameters = new HashMap<>(params);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> parameters.get((String) invocation.getArgument(0)));
        final Result result = new Result();
        final StringWriter body = new StringWriter();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(body, true));
        doAnswer(invocation -> {
            result.status = invocation.getArgument(0);
            return null;
        }).when(response).sendError(org.mockito.ArgumentMatchers.anyInt());

        ajaxCall.run(request, response);

        result.body = body.toString();
        return result;
    }

    private static User userWithPortlet(final String portletId) throws Exception {
        final Layout layout = new LayoutDataGen()
                .name("wf-ajax-" + portletId + "-" + System.nanoTime())
                .portletIds(portletId)
                .nextPersisted();
        layouts.add(layout);
        final Role role = track(newRole().layout(layout).nextPersisted());
        return track(new UserDataGen().roles(roleAPI.loadBackEndUserRole(), role).nextPersisted());
    }

    private static RoleDataGen newRole() {
        final long suffix = System.nanoTime();
        return new RoleDataGen().name("wf-ajax-role-" + suffix).key("wf-ajax-role-" + suffix);
    }

    private static WorkflowAction assignableAction() {
        return new WorkflowActionDataGen(scheme.getId(), step.getId())
                .assignable(true)
                .nextAssign(assigneeRole.getId())
                .nextPersisted();
    }

    private static void grantUse(final WorkflowAction action, final Role role) throws Exception {
        APILocator.getPermissionAPI().save(
                new Permission(action.getId(), role.getId(), PermissionAPI.PERMISSION_USE),
                action, APILocator.systemUser(), false);
    }

    private static User track(final User user) {
        users.add(user);
        return user;
    }

    private static Role track(final Role role) {
        roles.add(role);
        return role;
    }
}

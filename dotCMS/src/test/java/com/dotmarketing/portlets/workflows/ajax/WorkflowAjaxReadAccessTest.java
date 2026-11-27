package com.dotmarketing.portlets.workflows.ajax;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies which back-end users may read the role, step and assignee lists served to the legacy
 * workflow, publishing and configuration screens.
 */
class WorkflowAjaxReadAccessTest {

    private LayoutAPI layoutAPI;
    private PermissionAPI permissionAPI;
    private RoleAPI roleAPI;
    private User user;
    private WorkflowAction action;
    private Role otherContentRole;
    private Role contentRole;
    private WorkflowAjaxReadAccess readAccess;

    @BeforeEach
    void setUp() throws Exception {
        layoutAPI = mock(LayoutAPI.class);
        permissionAPI = mock(PermissionAPI.class);
        roleAPI = mock(RoleAPI.class);
        user = mock(User.class);
        when(user.isBackendUser()).thenReturn(true);
        action = mock(WorkflowAction.class);
        otherContentRole = mock(Role.class);
        contentRole = mock(Role.class);
        when(roleAPI.findWorkflowSpecialRoles()).thenReturn(List.of(otherContentRole, contentRole));
        readAccess = new WorkflowAjaxReadAccess(layoutAPI, permissionAPI, roleAPI);
    }

    /**
     * Gives the user access to the given portlet only.
     */
    private void grantOnly(final String portletId) throws DotDataException {
        when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), eq(user)))
                .thenAnswer(invocation -> portletId.equals(invocation.getArgument(0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"workflow-schemes", "workflow", "configuration", "publishing-queue"})
    void rolesAreReadableWithAnyRolePickerPortlet(final String portletId) throws Exception {
        grantOnly(portletId);

        assertTrue(readAccess.canReadRoles(user));
    }

    @Test
    void rolesRequireOneOfTheRolePickerPortlets() throws Exception {
        grantOnly("site-browser");

        assertFalse(readAccess.canReadRoles(user));
    }

    @Test
    void rolesRequireABackEndUser() throws Exception {
        grantOnly("workflow-schemes");
        when(user.isBackendUser()).thenReturn(false);

        assertFalse(readAccess.canReadRoles(user));
    }

    @Test
    void missingUserIsRefusedByEveryCheck() throws Exception {
        when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), any())).thenReturn(true);

        assertFalse(readAccess.canReadRoles(null));
        assertFalse(readAccess.canReadSteps(null));
        assertFalse(readAccess.canListAssignees(null, action));
    }

    @Test
    void rolesAreNotReadableWhenLayoutLookupFails() throws Exception {
        when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), eq(user)))
                .thenThrow(new DotDataException("lookup failed"));

        assertFalse(readAccess.canReadRoles(user));
    }

    @ParameterizedTest
    @ValueSource(strings = {"workflow", "workflow-schemes"})
    void stepsAreReadableWithWorkflowPortlets(final String portletId) throws Exception {
        grantOnly(portletId);

        assertTrue(readAccess.canReadSteps(user));
    }

    @Test
    void stepsRequireAWorkflowPortlet() throws Exception {
        grantOnly("configuration");

        assertFalse(readAccess.canReadSteps(user));
    }

    @Test
    void stepsAreNotReadableWhenLayoutLookupFails() throws Exception {
        when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), eq(user)))
                .thenThrow(new DotDataException("lookup failed"));

        assertFalse(readAccess.canReadSteps(user));
    }

    @Test
    void assigneesOfMissingActionAreNotListed() throws Exception {
        grantOnly("workflow-schemes");

        assertFalse(readAccess.canListAssignees(user, null));
    }

    @Test
    void assigneesAreListedForUserWhoCanUseAction() throws Exception {
        grantOnly("site-browser");
        when(permissionAPI.doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user, false))
                .thenReturn(true);

        assertTrue(readAccess.canListAssignees(user, action));
    }

    @Test
    void assigneesAreListedForActionOpenToContentRoles() throws Exception {
        grantOnly("site-browser");
        when(permissionAPI.doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE, contentRole))
                .thenReturn(true);

        assertTrue(readAccess.canListAssignees(user, action));
    }

    @Test
    void assigneesRequireABackEndUser() throws Exception {
        grantOnly("workflow-schemes");
        when(user.isBackendUser()).thenReturn(false);
        when(permissionAPI.doesUserHavePermission(any(WorkflowAction.class), anyInt(), any(User.class),
                anyBoolean())).thenReturn(true);
        when(permissionAPI.doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE, contentRole))
                .thenReturn(true);

        assertFalse(readAccess.canListAssignees(user, action));
    }

    @Test
    void assigneesAreListedForWorkflowSchemesUser() throws Exception {
        // no USE on the action and no content role on it: only the portlet grants access
        grantOnly("workflow-schemes");

        assertTrue(readAccess.canListAssignees(user, action));
    }

    @Test
    void assigneesRequireAccessToTheAction() throws Exception {
        grantOnly("workflow");

        assertFalse(readAccess.canListAssignees(user, action));
    }

    @Test
    void assigneesAreNotListedWhenSpecialRoleLookupFails() throws Exception {
        grantOnly("site-browser");
        when(roleAPI.findWorkflowSpecialRoles()).thenThrow(new DotDataException("lookup failed"));

        assertFalse(readAccess.canListAssignees(user, action));
    }

    @Test
    void assigneesAreNotListedWhenPermissionLookupFails() throws Exception {
        grantOnly("site-browser");
        when(permissionAPI.doesUserHavePermission(any(WorkflowAction.class), anyInt(), any(User.class),
                anyBoolean())).thenThrow(new DotDataException("lookup failed"));

        assertFalse(readAccess.canListAssignees(user, action));
    }
}

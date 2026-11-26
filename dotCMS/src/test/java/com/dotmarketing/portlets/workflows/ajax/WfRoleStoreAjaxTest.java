package com.dotmarketing.portlets.workflows.ajax;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.liferay.portal.model.User;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Verifies that the role store answers 403 and writes nothing when the caller may not read it.
 */
class WfRoleStoreAjaxTest {

    private MockedStatic<APILocator> apiLocator;
    private RoleAPI roleAPI;
    private WorkflowAPI workflowAPI;
    private WorkflowAjaxReadAccess readAccess;
    private User user;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private WfRoleStoreAjax roleStore;

    @BeforeEach
    void setUp() throws Exception {
        roleAPI = mock(RoleAPI.class);
        workflowAPI = mock(WorkflowAPI.class);
        apiLocator = org.mockito.Mockito.mockStatic(APILocator.class);
        apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
        apiLocator.when(APILocator::getWorkflowAPI).thenReturn(workflowAPI);
        apiLocator.when(APILocator::getUserAPI).thenReturn(mock(UserAPI.class));

        readAccess = mock(WorkflowAjaxReadAccess.class);
        user = mock(User.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        roleStore = new WfRoleStoreAjax(readAccess);
        roleStore.setUser(user);
    }

    @AfterEach
    void tearDown() {
        apiLocator.close();
    }

    @Test
    void roleListAnswersForbiddenWithoutAccess() throws Exception {
        when(readAccess.canReadRoles(user)).thenReturn(false);

        roleStore.action(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).getWriter();
        verifyNoInteractions(roleAPI);
    }

    @Test
    void roleLookupByIdAnswersForbiddenWithoutAccess() throws Exception {
        when(readAccess.canReadRoles(user)).thenReturn(false);
        when(request.getParameter("roleId")).thenReturn("some-role-id");

        roleStore.action(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).getWriter();
        verifyNoInteractions(roleAPI);
    }

    @Test
    void assigneesAnswerForbiddenWithoutAccess() throws Exception {
        final WorkflowAction action = mock(WorkflowAction.class);
        when(request.getParameter("actionId")).thenReturn("action-id");
        when(workflowAPI.findAction("action-id", user)).thenReturn(action);
        when(readAccess.canListAssignees(user, action)).thenReturn(false);

        roleStore.assignable(request, response);

        verify(readAccess).canListAssignees(user, action);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).getWriter();
        verifyNoInteractions(roleAPI);
    }

    @Test
    void assigneesOfUnknownActionAnswerLikeDenied() throws Exception {
        when(request.getParameter("actionId")).thenReturn("unknown-id");
        when(workflowAPI.findAction(anyString(), any(User.class))).thenReturn(null);
        when(readAccess.canListAssignees(user, null)).thenReturn(false);

        roleStore.assignable(request, response);

        verify(readAccess).canListAssignees(user, null);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).getWriter();
        verifyNoInteractions(roleAPI);
    }

    @Test
    void assigneesAnswerForbiddenWhenActionLookupFails() throws Exception {
        when(request.getParameter("actionId")).thenReturn("action-id");
        when(workflowAPI.findAction("action-id", user)).thenThrow(new DotDataException("lookup failed"));

        roleStore.assignable(request, response);

        verify(readAccess).canListAssignees(user, null);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).getWriter();
    }

    @Test
    void assigneesWithoutActionIdAnswerForbidden() throws Exception {
        roleStore.assignable(request, response);

        verify(readAccess).canListAssignees(user, null);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).getWriter();
        verifyNoInteractions(workflowAPI);
    }
}

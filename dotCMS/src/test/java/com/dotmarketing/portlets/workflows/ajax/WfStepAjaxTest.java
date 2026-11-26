package com.dotmarketing.portlets.workflows.ajax;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.liferay.portal.model.User;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Verifies that the step list answers 403 and writes nothing when the caller may not read it.
 */
class WfStepAjaxTest {

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<WebAPILocator> webApiLocator;
    private MockedStatic<WorkflowHelper> workflowHelper;
    private WorkflowAPI workflowAPI;
    private WorkflowAjaxReadAccess readAccess;
    private User user;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private WfStepAjax stepAjax;

    @BeforeEach
    void setUp() throws Exception {
        workflowAPI = mock(WorkflowAPI.class);
        apiLocator = Mockito.mockStatic(APILocator.class);
        apiLocator.when(APILocator::getWorkflowAPI).thenReturn(workflowAPI);
        webApiLocator = Mockito.mockStatic(WebAPILocator.class);
        webApiLocator.when(WebAPILocator::getUserWebAPI).thenReturn(mock(UserWebAPI.class));
        // keeps the WorkflowHelper singleton from being built with this test's mocks
        workflowHelper = Mockito.mockStatic(WorkflowHelper.class);
        workflowHelper.when(WorkflowHelper::getInstance).thenReturn(mock(WorkflowHelper.class));

        readAccess = mock(WorkflowAjaxReadAccess.class);
        user = mock(User.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        stepAjax = new WfStepAjax(readAccess);
        stepAjax.setUser(user);
    }

    @AfterEach
    void tearDown() {
        workflowHelper.close();
        webApiLocator.close();
        apiLocator.close();
    }

    @Test
    void stepListAnswersForbiddenWithoutAccess() throws Exception {
        when(readAccess.canReadSteps(user)).thenReturn(false);
        when(request.getParameter("schemeId")).thenReturn("scheme-id");

        stepAjax.listByScheme(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).getWriter();
        verifyNoInteractions(workflowAPI);
    }

    @Test
    void stepListWithoutSchemeWritesNothing() throws Exception {
        when(readAccess.canReadSteps(user)).thenReturn(true);

        stepAjax.listByScheme(request, response);

        verify(response, never()).sendError(anyInt());
        verify(response, never()).getWriter();
    }
}

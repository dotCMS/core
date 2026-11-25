package com.dotmarketing.portlets.cmsmaintenance.ajax;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit test for the access rule {@link LogConsoleAjaxAction} applies to its commands: the same one
 * as {@link IndexAjaxAction}, a CMS administrator who also has access to the Maintenance portlet.
 *
 * @author hassandotcms
 */
class LogConsoleAjaxActionTest {

    /**
     * Given: a back-end user with the Maintenance portlet who is not a CMS administrator
     * When: the command that changes which logs are enabled is requested
     * Then: the request is answered with 401 and the command does not run
     */
    @Test
    void service_rejectsPortletHolderWhoIsNotAdmin() throws Exception {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("back-end-user");
        when(user.isAdmin()).thenReturn(false);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        when(layoutAPI.doesUserHaveAccessToPortlet("maintenance", user)).thenReturn(true);

        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(null);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final Map<String, String> params = new HashMap<>();
        params.put("cmd", "enabledDisabledLogs");
        final LogConsoleAjaxAction action = mock(LogConsoleAjaxAction.class, CALLS_REAL_METHODS);
        when(action.getURIParams()).thenReturn(params);
        when(action.getUser()).thenReturn(user);
        doNothing().when(action).setUser(any(HttpServletRequest.class));
        doNothing().when(action).enabledDisabledLogs(any(), any());

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class);
                MockedStatic<SecurityLogger> securityLogger = mockStatic(SecurityLogger.class)) {
            apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);

            action.service(request, response);
        }

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(action, never()).enabledDisabledLogs(any(), any());
    }
}

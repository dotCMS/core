package com.dotmarketing.portlets.cmsmaintenance.ajax;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the access rule of {@link IndexAjaxAction#service}: a command runs only for a CMS
 * administrator who also has access to the Maintenance portlet. The action is a partial mock whose
 * user and URI parameters are stubbed; the layout API and the loggers are mocked.
 *
 * @author hassandotcms
 */
class IndexAjaxActionTest {

    private static final String MAINTENANCE_PORTLET = "maintenance";
    private static final String COMMAND = "deleteIndex";

    private final LayoutAPI layoutAPI = mock(LayoutAPI.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<Logger> logger;
    private MockedStatic<SecurityLogger> securityLogger;

    @BeforeEach
    void setUp() {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(null);
        when(request.getSession()).thenReturn(session);

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
        logger = mockStatic(Logger.class);
        securityLogger = mockStatic(SecurityLogger.class);
    }

    @AfterEach
    void tearDown() {
        securityLogger.close();
        logger.close();
        apiLocator.close();
    }

    /**
     * Builds the action with the command and the logged-in user stubbed, and the command itself
     * replaced by a no-op so only the access rule in {@code service} runs.
     *
     * @param admin              whether the user is a CMS administrator
     * @param maintenancePortlet whether the user has access to the Maintenance portlet
     * @return the prepared action
     */
    private IndexAjaxAction givenActionFor(final boolean admin, final boolean maintenancePortlet)
            throws Exception {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(admin ? "admin-user" : "back-end-user");
        when(user.isAdmin()).thenReturn(admin);
        when(layoutAPI.doesUserHaveAccessToPortlet(MAINTENANCE_PORTLET, user))
                .thenReturn(maintenancePortlet);

        final Map<String, String> params = new HashMap<>();
        params.put("cmd", COMMAND);

        final IndexAjaxAction action = mock(IndexAjaxAction.class, CALLS_REAL_METHODS);
        when(action.getURIParams()).thenReturn(params);
        when(action.getUser()).thenReturn(user);
        doNothing().when(action).setUser(any(HttpServletRequest.class));
        doNothing().when(action).deleteIndex(any(), any());
        return action;
    }

    /**
     * Given: a back-end user with the Maintenance portlet who is not a CMS administrator
     * When: a command is requested
     * Then: the request is answered with 401 and the command does not run
     */
    @Test
    void service_rejectsPortletHolderWhoIsNotAdmin() throws Exception {
        final IndexAjaxAction action = givenActionFor(false, true);

        action.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(action, never()).deleteIndex(any(), any());
        securityLogger.verify(() -> SecurityLogger.logInfo(eq(IndexAjaxAction.class),
                contains("back-end-user")));
    }

    /**
     * Given: no user resolved on the action, and a session user with the Maintenance portlet who
     * is not a CMS administrator
     * When: a command is requested
     * Then: the request is answered with 401 and the command does not run
     */
    @Test
    void service_rejectsSessionUserWhoIsNotAdmin() throws Exception {
        final IndexAjaxAction action = givenActionFor(false, true);
        final User sessionUser = action.getUser();
        when(action.getUser()).thenReturn(null);
        when(request.getSession().getAttribute(WebKeys.CMS_USER)).thenReturn(sessionUser);

        action.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(action, never()).deleteIndex(any(), any());
    }

    /**
     * Given: a CMS administrator whose portlets cannot be loaded
     * When: a command is requested
     * Then: the request is answered with 401 and the command does not run
     */
    @Test
    void service_rejectsWhenAccessCannotBeChecked() throws Exception {
        final IndexAjaxAction action = givenActionFor(true, true);
        when(layoutAPI.doesUserHaveAccessToPortlet(eq(MAINTENANCE_PORTLET), any()))
                .thenThrow(new DotDataException("layouts unavailable"));

        action.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(action, never()).deleteIndex(any(), any());
    }

    /**
     * Given: a CMS administrator with the Maintenance portlet
     * When: a command is requested
     * Then: the command runs and no error is sent
     */
    @Test
    void service_runsCommandForAdminWithPortlet() throws Exception {
        final IndexAjaxAction action = givenActionFor(true, true);

        action.service(request, response);

        verify(action).deleteIndex(request, response);
        verify(response, never()).sendError(anyInt());
    }
}

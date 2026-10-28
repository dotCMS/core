package com.dotcms.cmsmaintenance.ajax;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.listeners.SessionMonitor;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.util.Map;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Unit tests for the access rules of {@link UserSessionAjax}: every session tool requires a CMS Administrator who
 * holds the Maintenance portlet. The logged-in user is resolved from a mocked DWR request, and the tracked sessions
 * come from a mocked {@link SessionMonitor}.
 */
class UserSessionAjaxTest {

    private static final String MAINTENANCE_PORTLET = "maintenance";

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpSession callingSession = mock(HttpSession.class);
    private final HttpSession otherSession = mock(HttpSession.class);
    private final LayoutAPI layoutAPI = mock(LayoutAPI.class);

    private MockedStatic<WebContextFactory> webContextFactory;
    private MockedStatic<PortalUtil> portalUtil;
    private MockedStatic<APILocator> apiLocator;
    private MockedConstruction<SessionMonitor> sessionMonitor;

    @BeforeEach
    void setUp() {
        when(callingSession.getId()).thenReturn("calling-session");
        when(otherSession.getId()).thenReturn("other-session");

        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);
        when(webContext.getSession()).thenReturn(callingSession);
        webContextFactory = mockStatic(WebContextFactory.class);
        webContextFactory.when(WebContextFactory::get).thenReturn(webContext);
        portalUtil = mockStatic(PortalUtil.class);
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
    }

    @AfterEach
    void tearDown() {
        if (null != sessionMonitor) {
            sessionMonitor.close();
        }
        apiLocator.close();
        portalUtil.close();
        webContextFactory.close();
    }

    /**
     * Logs in a back-end user who holds the Maintenance portlet.
     *
     * @param admin whether the user is a CMS Administrator
     */
    private void givenMaintenanceUser(final boolean admin) throws Exception {
        givenLoggedInUser(admin, true);
    }

    /**
     * Logs in a back-end user.
     *
     * @param admin              whether the user is a CMS Administrator
     * @param maintenancePortlet whether the user has access to the Maintenance portlet
     */
    private void givenLoggedInUser(final boolean admin, final boolean maintenancePortlet) throws Exception {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(admin ? "admin-user" : "maintenance-user");
        when(user.isAdmin()).thenReturn(admin);
        portalUtil.when(() -> PortalUtil.getUser(request)).thenReturn(user);
        when(layoutAPI.doesUserHaveAccessToPortlet(MAINTENANCE_PORTLET, user)).thenReturn(maintenancePortlet);
    }

    /**
     * Makes the session monitor report the given sessions.
     *
     * @param sessions the tracked sessions, by id
     */
    private void givenTrackedSessions(final Map<String, HttpSession> sessions) {
        sessionMonitor = mockConstruction(SessionMonitor.class,
                (monitor, context) -> when(monitor.getUserSessions()).thenReturn(sessions));
    }

    /** One call per public session tool of {@link UserSessionAjax}. */
    static Stream<Arguments> everyTool() {
        return Stream.of(
                Arguments.of("getSessionList", (ToolCall) UserSessionAjax::getSessionList),
                Arguments.of("invalidateAllSessions", (ToolCall) UserSessionAjax::invalidateAllSessions),
                Arguments.of("invalidateSession", (ToolCall) tool -> tool.invalidateSession("token")));
    }

    /**
     * Method to test: every public session tool of {@link UserSessionAjax}.
     * Given scenario: the caller holds the Maintenance portlet but is not a CMS Administrator, and another session is
     * tracked.
     * Expected result: a {@link DotRuntimeException}, and the other session is not invalidated.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("everyTool")
    void everyTool_callerWithoutAdministratorRole_isRejected(final String toolName, final ToolCall call)
            throws Exception {
        givenMaintenanceUser(false);
        givenTrackedSessions(Map.of("other-session", otherSession));

        assertThrows(DotRuntimeException.class, () -> call.run(new UserSessionAjax()),
                toolName + " must require a CMS Administrator");

        verify(otherSession, never()).invalidate();
    }

    /**
     * Method to test: {@link UserSessionAjax#invalidateAllSessions()}.
     * Given scenario: the caller is a CMS Administrator who holds the Maintenance portlet, and another session is
     * tracked.
     * Expected result: the other session is invalidated.
     */
    @Test
    void invalidateAllSessions_cmsAdministrator_invalidatesOtherSessions() throws Exception {
        givenMaintenanceUser(true);
        givenTrackedSessions(Map.of("other-session", otherSession));

        new UserSessionAjax().invalidateAllSessions();

        verify(otherSession).invalidate();
    }

    /**
     * Method to test: {@link UserSessionAjax#validateUser()}.
     * Given scenario: the caller is a CMS Administrator without access to the Maintenance portlet.
     * Expected result: a {@link DotRuntimeException}.
     */
    @Test
    void validateUser_administratorWithoutMaintenancePortlet_isRejected() throws Exception {
        givenLoggedInUser(true, false);

        assertThrows(DotRuntimeException.class, () -> new UserSessionAjax().validateUser());
    }

    /**
     * Method to test: {@link UserSessionAjax#validateUser()}.
     * Given scenario: nobody is logged in.
     * Expected result: a {@link DotRuntimeException}.
     */
    @Test
    void validateUser_withoutLoggedInUser_isRejected() {
        portalUtil.when(() -> PortalUtil.getUser(request)).thenReturn(null);

        assertThrows(DotRuntimeException.class, () -> new UserSessionAjax().validateUser());
    }

    /** A call to one public session tool of {@link UserSessionAjax}. */
    @FunctionalInterface
    interface ToolCall {

        /**
         * Invokes the tool.
         *
         * @param tool the tool under test
         * @throws Exception whatever the tool throws
         */
        void run(UserSessionAjax tool) throws Exception;
    }
}

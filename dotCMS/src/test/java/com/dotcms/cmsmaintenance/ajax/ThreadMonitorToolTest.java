package com.dotcms.cmsmaintenance.ajax;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

/**
 * Unit tests for the access rules of {@link ThreadMonitorTool}: every tool requires a CMS Administrator who holds the
 * Maintenance portlet. The logged-in user is resolved from a mocked DWR request.
 */
class ThreadMonitorToolTest {

    private static final String MAINTENANCE_PORTLET = "maintenance";

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final LayoutAPI layoutAPI = mock(LayoutAPI.class);

    private MockedStatic<WebContextFactory> webContextFactory;
    private MockedStatic<PortalUtil> portalUtil;
    private MockedStatic<APILocator> apiLocator;

    @BeforeEach
    void setUp() {
        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);
        webContextFactory = mockStatic(WebContextFactory.class);
        webContextFactory.when(WebContextFactory::get).thenReturn(webContext);
        portalUtil = mockStatic(PortalUtil.class);
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
    }

    @AfterEach
    void tearDown() {
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

    /** One call per public tool of {@link ThreadMonitorTool}. */
    static Stream<Arguments> everyTool() {
        return Stream.of(
                Arguments.of("getThreads", (ToolCall) ThreadMonitorTool::getThreads),
                Arguments.of("getThreads hiding system threads", (ToolCall) tool -> tool.getThreads(true)),
                Arguments.of("getSysProps", (ToolCall) ThreadMonitorTool::getSysProps));
    }

    /**
     * Method to test: every public tool of {@link ThreadMonitorTool}.
     * Given scenario: the caller holds the Maintenance portlet but is not a CMS Administrator.
     * Expected result: a {@link DotRuntimeException}, and nothing is returned.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("everyTool")
    void everyTool_callerWithoutAdministratorRole_isRejected(final String toolName, final ToolCall call)
            throws Exception {
        givenMaintenanceUser(false);

        assertThrows(DotRuntimeException.class, () -> call.run(new ThreadMonitorTool()),
                toolName + " must require a CMS Administrator");
    }

    /**
     * Method to test: {@link ThreadMonitorTool#getSysProps()}.
     * Given scenario: the caller is a CMS Administrator who holds the Maintenance portlet.
     * Expected result: the current thread count is returned.
     */
    @Test
    void getSysProps_cmsAdministrator_returnsThreadCounts() throws Exception {
        givenMaintenanceUser(true);

        final Map<String, String> properties = new ThreadMonitorTool().getSysProps();

        assertTrue(properties.containsKey("Thread Count - Current "));
    }

    /**
     * Method to test: {@link ThreadMonitorTool#validateUser()}.
     * Given scenario: the caller is a CMS Administrator without access to the Maintenance portlet.
     * Expected result: a {@link DotRuntimeException}.
     */
    @Test
    void validateUser_administratorWithoutMaintenancePortlet_isRejected() throws Exception {
        givenLoggedInUser(true, false);

        assertThrows(DotRuntimeException.class, () -> new ThreadMonitorTool().validateUser());
    }

    /**
     * Method to test: {@link ThreadMonitorTool#validateUser()}.
     * Given scenario: nobody is logged in.
     * Expected result: a {@link DotRuntimeException}.
     */
    @Test
    void validateUser_withoutLoggedInUser_isRejected() {
        portalUtil.when(() -> PortalUtil.getUser(request)).thenReturn(null);

        assertThrows(DotRuntimeException.class, () -> new ThreadMonitorTool().validateUser());
    }

    /** A call to one public tool of {@link ThreadMonitorTool}. */
    @FunctionalInterface
    interface ToolCall {

        /**
         * Invokes the tool.
         *
         * @param tool the tool under test
         * @return what the tool returns
         * @throws Exception whatever the tool throws
         */
        Object run(ThreadMonitorTool tool) throws Exception;
    }
}

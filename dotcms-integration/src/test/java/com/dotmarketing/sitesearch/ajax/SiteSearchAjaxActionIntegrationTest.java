package com.dotmarketing.sitesearch.ajax;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.RoleAPI;
import com.liferay.portal.model.User;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link SiteSearchAjaxAction#service(HttpServletRequest, HttpServletResponse)}
 * using a back-end user that only holds the Site Search portlet. The endpoint serves the commands
 * it declares, and answers any other command with a 403. Only commands that change
 * nothing are used as the "other endpoint" examples, so the shared test database keeps its data.
 */
public class SiteSearchAjaxActionIntegrationTest {

    private static final String SITE_SEARCH_PORTLET = "site-search";
    private static final String ENDPOINT =
            "/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/";

    private static Layout siteSearchLayout;
    private static User siteSearchUser;
    private static User userWithoutPortlet;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        // getJobProgress needs an enterprise license: its task list is null below the STANDARD level
        LicenseTestUtil.getLicense();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        siteSearchLayout = new LayoutDataGen()
                .name("site-search-" + System.nanoTime())
                .portletIds(SITE_SEARCH_PORTLET)
                .nextPersisted();
        siteSearchUser = new UserDataGen()
                .emailAddress("site-search-user" + System.nanoTime() + "@dotcms.com")
                .roles(roleAPI.loadBackEndUserRole())
                .nextPersisted();
        roleAPI.addLayoutToRole(siteSearchLayout, roleAPI.getUserRole(siteSearchUser));
        userWithoutPortlet = new UserDataGen()
                .emailAddress("no-portlet-user" + System.nanoTime() + "@dotcms.com")
                .roles(roleAPI.loadBackEndUserRole())
                .nextPersisted();

        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
        assertFalse(siteSearchUser.isAdmin());
        assertTrue(layoutAPI.doesUserHaveAccessToPortlet(SITE_SEARCH_PORTLET, siteSearchUser));
        assertFalse(layoutAPI.doesUserHaveAccessToPortlet("maintenance", siteSearchUser));
        assertFalse(layoutAPI.doesUserHaveAccessToPortlet(SITE_SEARCH_PORTLET, userWithoutPortlet));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        for (final User user : new User[]{siteSearchUser, userWithoutPortlet}) {
            if (null != user) {
                APILocator.getUserAPI().delete(user, TestUserUtils.getAdminUser(),
                        APILocator.systemUser(), false);
            }
        }
        if (null != siteSearchLayout) {
            final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
            layoutAPI.removeLayout(layoutAPI.findLayout(siteSearchLayout.getId()));
        }
    }

    /**
     * Method to test: {@link SiteSearchAjaxAction#service(HttpServletRequest, HttpServletResponse)}
     * Given: a Site Search user requesting commands that are not served by this endpoint
     * Expected: 403 for each of them
     */
    @Test
    public void service_rejectsCommandsOutsideTheSupportedSet() throws Exception {
        for (final String cmd : new String[]{"getActiveIndex", "getReindexThreadStatus",
                "activateIndex", "deactivateIndex", "closeIndex", "openIndex", "clearIndex",
                "deleteIndex", "createIndex", "updateReplicas", "scheduleJobNow", "getIndexName", "getIndexStatus",
                "getNotActiveIndexNames", "indexList", "service", "doGet"}) {
            final HttpServletResponse response = call(cmd);
            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * Method to test: {@link SiteSearchAjaxAction#service(HttpServletRequest, HttpServletResponse)}
     * Given: a Site Search user requesting a command served by this endpoint
     * Expected: the command runs, so its JSON output is written and no error is reported
     */
    @Test
    public void service_runsSupportedCommands() throws Exception {
        final StringWriter body = new StringWriter();
        final HttpServletResponse response = call("getJobProgress", siteSearchUser, body);

        verify(response, never()).sendError(anyInt());
        assertTrue("expected the job progress list, got: " + body, body.toString().trim().startsWith("["));
    }

    /**
     * Method to test: {@link SiteSearchAjaxAction#service(HttpServletRequest, HttpServletResponse)}
     * Given: a back-end user that does not hold the Site Search portlet
     * Expected: the command does not run, so nothing is written to the response
     */
    @Test
    public void service_doesNotRunCommandsForUsersWithoutThePortlet() throws Exception {
        final StringWriter body = new StringWriter();
        call("getJobProgress", userWithoutPortlet, body);

        assertTrue("expected no output, got: " + body, body.toString().isEmpty());
    }

    private static HttpServletResponse call(final String cmd) throws Exception {
        return call(cmd, siteSearchUser, new StringWriter());
    }

    private static HttpServletResponse call(final String cmd, final User user, final StringWriter body)
            throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(ENDPOINT + cmd);
        when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(body, true));

        final SiteSearchAjaxAction action = new SiteSearchAjaxAction();
        action.setUser(user);
        action.setURIParams(request);
        action.service(request, response);
        return response;
    }
}

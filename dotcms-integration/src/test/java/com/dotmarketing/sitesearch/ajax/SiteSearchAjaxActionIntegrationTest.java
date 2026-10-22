package com.dotmarketing.sitesearch.ajax;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
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

        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
        assertFalse(siteSearchUser.isAdmin());
        assertTrue(layoutAPI.doesUserHaveAccessToPortlet(SITE_SEARCH_PORTLET, siteSearchUser));
        assertFalse(layoutAPI.doesUserHaveAccessToPortlet("maintenance", siteSearchUser));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (null != siteSearchUser) {
            APILocator.getUserAPI().delete(siteSearchUser, TestUserUtils.getAdminUser(),
                    APILocator.systemUser(), false);
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
                "deleteIndex", "createIndex", "updateReplicas", "service", "doGet"}) {
            final HttpServletResponse response = call(cmd);
            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * Method to test: {@link SiteSearchAjaxAction#service(HttpServletRequest, HttpServletResponse)}
     * Given: a Site Search user requesting a command served by this endpoint
     * Expected: the command runs and the request is not turned away
     */
    @Test
    public void service_runsSupportedCommands() throws Exception {
        final HttpServletResponse response = call("indexList");

        verify(response, never()).sendError(anyInt());
    }

    private static HttpServletResponse call(final String cmd) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(ENDPOINT + cmd);
        when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        final SiteSearchAjaxAction action = new SiteSearchAjaxAction();
        action.setUser(siteSearchUser);
        action.setURIParams(request);
        action.service(request, response);
        return response;
    }
}

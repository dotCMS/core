package com.dotcms.rest.api.v1.site;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.mapAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.util.PaginationUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * {@link SiteResource} test
 * @author jsanca
 */
public class SiteResourceTest extends UnitTestBase {

    private static final int page = 1;
    private static final int count = 20;

    /**
     * Queries the list of sites associated to a user based on the value of the
     * "filter" parameter being an actual filter or an empty value.
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testNullAndEmptyFilter() throws JSONException, DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final HostAPI hostAPI     = mock(HostAPI.class);
        final HostVariableAPI hostVariableAPI = mock(HostVariableAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final WebResource webResource       = mock(WebResource.class);
        final ServletContext context = mock(ServletContext.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final PaginatedArrayList<Host> hosts = getSites();
        final PaginationUtil paginationUtil = mock(PaginationUtil.class);

        final Response responseExpected = Response.ok(new ResponseEntityView<>(hosts)).build();

        Config.CONTEXT = context;
        try {
            when(initDataObject.getUser()).thenReturn(user);
            when(webResource.init((WebResource.InitBuilder)any())).thenReturn(initDataObject);
            when(initDataObject.getUser()).thenReturn(user);
            when(paginationUtil.getPage(request, user, "filter",1, count,
                    Map.of("archive", false, "live", false, "system", false))).thenReturn(responseExpected);
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session);
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
            SiteResource siteResource =
                    new SiteResource(webResource, new SiteHelper(hostAPI, hostVariableAPI),
                            paginationUtil);

            final Response response = siteResource
                    .sites(request, httpServletResponse, "filter", false, false, false, page, count);

            RestUtilTest.verifySuccessResponse(response);

            assertEquals(((ResponseEntityView) response.getEntity()).getEntity(), hosts);
        } finally {
            Config.CONTEXT = null;
        }
    }


    @Test
    public void testSwitchNullEmptyAndInvalidFilter() throws JSONException, DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final HostAPI hostAPI     = mock(HostAPI.class);
        final HostVariableAPI hostVariableAPI = mock(HostVariableAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final WebResource webResource       = mock(WebResource.class);
        final ServletContext context = mock(ServletContext.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final List<Host> hosts = getSites();
        final PaginationUtil paginationUtil = mock(PaginationUtil.class);

        Config.CONTEXT = context;
        try {
            when(initDataObject.getUser()).thenReturn(user);
            // final InitDataObject initData = this.webResource.init(null, request, response, true, null); // should logged in
            when(webResource.init((WebResource.InitBuilder)notNull())).thenReturn(initDataObject);

            when(hostAPI.findAll(user, true)).thenReturn(hosts);
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session);
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
            SiteResource siteResource =
                    new SiteResource(webResource, new SiteHelper(hostAPI, hostVariableAPI),
                            paginationUtil);

            Response response1 = siteResource.switchSite(request, httpServletResponse);
            System.out.println(response1);
            System.out.println(response1.getEntity());

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 500);

            response1 = siteResource.switchSite(request, httpServletResponse, StringUtils.EMPTY);
            System.out.println(response1);
            System.out.println(response1.getEntity());

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 404);

            response1 = siteResource
                    .switchSite(request, httpServletResponse, "48190c8c-not-found-8d1a-0cd5db894797");
            System.out.println(response1);
            System.out.println(response1.getEntity());

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 404);

            response1 = siteResource.switchSite(request, httpServletResponse,
                    "48190c8c-42c4-46af-8d1a-0cd5db894797"); // system, should be not allowed to switch
            System.out.println(response1);
            System.out.println(response1.getEntity());

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 404);
        } finally {
            Config.CONTEXT = null;
        }
    }


    @Test
    public void testSwitchExistingHost() throws JSONException, DotSecurityException, DotDataException {
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final HostAPI hostAPI     = mock(HostAPI.class);
        final HostVariableAPI hostVariableAPI = mock(HostVariableAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final WebResource webResource       = mock(WebResource.class);
        final ServletContext context = mock(ServletContext.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        
        final User user = new User();
        final Host host = getSite().get(0);
        final PaginationUtil paginationUtil = mock(PaginationUtil.class);

        Config.CONTEXT = context;
        try {
            Map<String, Object> sessionAttributes = new HashMap<>(Map.of(WebKeys.CONTENTLET_LAST_SEARCH, "mock mock mock mock"));

            when(initDataObject.getUser()).thenReturn(user);
            when(webResource.init((WebResource.InitBuilder)any())).thenReturn(initDataObject);
            when(hostAPI.find("48190c8c-42c4-46af-8d1a-0cd5db894798", user, Boolean.TRUE)).thenReturn(host);
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session);
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
            doAnswer(new Answer<Void>() {

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {

                    Object [] args = invocation.getArguments();
                    sessionAttributes.put((String) args[0], args[1]);
                    return null;
                }
            }).when(session).setAttribute(
                    anyString(),
                    any()
            );

            doAnswer(new Answer<Void>() {

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {

                    Object [] args = invocation.getArguments();
                    sessionAttributes.remove((String) args[0]);
                    return null;
                }
            }).when(session).removeAttribute(
                    anyString()
            );

            SiteResource siteResource =
                    new SiteResource(webResource, new SiteHelper(hostAPI, hostVariableAPI),
                            paginationUtil);

            Response response1 = siteResource
                    .switchSite(request, httpServletResponse, "48190c8c-42c4-46af-8d1a-0cd5db894798");
            System.out.println(response1);
            System.out.println(response1.getEntity());
            System.out.println(sessionAttributes);

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 200);
            assertTrue(sessionAttributes.size() == 1 );
            assertTrue(!sessionAttributes.containsKey(WebKeys.CONTENTLET_LAST_SEARCH));
            assertTrue(sessionAttributes.containsKey(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID));
        } finally {
            Config.CONTEXT = null;
        }
    }

    /**
     * Verifies the list of sites that a user has access to. Such a list is used
     * to load the items in the Site Selector component.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testCurrentSites() throws DotSecurityException, DotDataException {
        final HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final HttpSession session = request.getSession();
        RestUtilTest.initMockContext();
        try {
            final User user = new User();
            final PaginatedArrayList<Host> siteList = getSites();
            final Host currentSite = siteList.get(0);
            final String currentSiteId = currentSite.getIdentifier();
            final WebResource webResource = RestUtilTest.getMockWebResource(user, request);
            final PaginationUtil paginationUtil = mock(PaginationUtil.class);

            final HostAPI hostAPI = mock(HostAPI.class);
            final HostVariableAPI hostVariableAPI = mock(HostVariableAPI.class);
            when(hostAPI.find(currentSiteId, user, false)).thenReturn(currentSite);

            final UserAPI userAPI = mock(UserAPI.class);
            when(userAPI.loadUserById(Mockito.anyString())).thenReturn(user);
            when(session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID))
                    .thenReturn(currentSite.getIdentifier());

            final InitDataObject initDataObject = mock(InitDataObject.class);
            when(webResource.init((WebResource.InitBuilder) any())).thenReturn(
                    initDataObject);
            when(initDataObject.getUser()).thenReturn(user);

            final SiteResource siteResource =
                    new SiteResource(webResource, new SiteHelper(hostAPI, hostVariableAPI),
                            paginationUtil);
            final Response response = siteResource.currentSite(request, httpServletResponse);

            RestUtilTest.verifySuccessResponse(response);
            Object entity = ((ResponseEntityView) response.getEntity()).getEntity();
            assertEquals(currentSite, entity);
        } finally {
            RestUtilTest.cleanupContext();
        }
    }

    /**
     * Returns a list of 2 mocked Sites for testing purposes.
     *
     * @return
     */
    private PaginatedArrayList<Host> getSite() throws DotDataException {
        Contentlet contentlet = new Contentlet(mapAll(
                Map.of(
                        "hostName", "system.dotcms.com",
                        "googleMap", "TEST_GOOGLE_MAP_KEY",
                        "modDate", Integer.parseInt("125466"),
                        "aliases", "",
                        "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                        "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                        "type", "host",
                        "title", "system.dotcms.com",
                        "inode", "54ac9a4e-3d63-4b9a-882f-27c7ba29618f",
                        "hostname", "system.dotcms.com"),
                Map.of(
                        "__DOTNAME__", "system.dotcms.com",
                        "addThis", "TEST_ADD_THIS_KEY",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894798",
                        "runDashboard", false,
                        "languageId", 1
                ),
                Map.of(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "TEST_GOOGLE_ANALYTICS_KEY",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                        "isSystemHost", true,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                )));

        contentlet = Mockito.spy(contentlet);
        Mockito.doNothing().when(contentlet).setTags();
        final List<Host> temp = list(new Host(contentlet) {
                                   @Override
                                   public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                                       return false;
                                   }
                               }
        );
        PaginatedArrayList<Host> hosts = new PaginatedArrayList<>();
        hosts.addAll(temp);
        hosts.setTotalResults(1);
        return hosts;
    }
    /**
     * Returns a list of 2 mocked Sites for testing purposes.
     *
     * @return
     */
    private PaginatedArrayList<Host> getSites() {
        final List<Contentlet> contentlets = list(new Contentlet(mapAll(
                Map.of(
                        "hostName", "demo.dotcms.com",
                        "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utmu0",
                        "modDate", Integer.parseInt("125466"),
                        "aliases", "",
                        "keywords", "CMS, Web Content Management, Open Source, Java, J2EE",
                        "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                        "type", "host",
                        "title", "demo.dotcms.com",
                        "inode", "54ac9a4e-3d63-4b9a-882f-27c7ba29618f",
                        "hostname", "demo.dotcms.com"),
                Map.of(
                        "__DOTNAME__", "demo.dotcms.com",
                        "addThis", "TEST_ADD_THIS_KEY",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380c",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894797",
                        "runDashboard", false,
                        "languageId", 1

                ),
                Map.of(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "TEST_GOOGLE_ANALYTICS_KEY",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894797",
                        "isSystemHost", false,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                )
        )), new Contentlet(mapAll(
                Map.of(
                        "hostName", "system.dotcms.com",
                        "googleMap", "TEST_GOOGLE_MAP_KEY",
                        "modDate", Integer.parseInt("125466"),
                        "aliases", "",
                        "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                        "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                        "type", "host",
                        "title", "system.dotcms.com",
                        "inode", "54ac9a4e-3d63-4b9a-882f-27c7ba29618f",
                        "hostname", "system.dotcms.com"),
                Map.of(
                        "__DOTNAME__", "system.dotcms.com",
                        "addThis", "TEST_ADD_THIS_KEY",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894798",
                        "runDashboard", false,
                        "languageId", 1
                ),
                Map.of(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "TEST_GOOGLE_ANALYTICS_KEY",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                        "isSystemHost", true,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                ))));

        final List<Host> temp = new ArrayList<>();
        contentlets.forEach(contentlet -> {
            contentlet = Mockito.spy(contentlet);
            try {
                Mockito.doNothing().when(contentlet).setTags();
            } catch (DotDataException e) {
                throw new RuntimeException(e);
            }
            temp.add(new Host(contentlet) {
                @Override
                public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                    return false;
                }
            });

        });

        PaginatedArrayList<Host> hosts = new PaginatedArrayList<>();
        hosts.addAll(temp);
        hosts.setTotalResults(2);
        return hosts;
    }

    /**
     * Returns a list of 3 mocked Sites for testing purposes.
     *
     * @return
     */
    private PaginatedArrayList<Host> getTwoSites() {
        List<Host> temp = list(new Host(new Contentlet(mapAll(
                Map.of(
                        "hostName", "demo.awesome.dotcms.com",
                        "googleMap", "TEST_GOOGLE_MAP_KEY",
                        "modDate", Integer.parseInt("125466"),
                        "aliases", "",
                        "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                        "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                        "type", "host",
                        "title", "system.dotcms.com",
                        "inode", "54ac9a4e-3d63-4b9a-882f-27c7dba29618f",
                        "hostname", "system.dotcms.com"),
                Map.of(
                        "__DOTNAME__", "demo.awesome.dotcms.com",
                        "addThis", "TEST_ADD_THIS_KEY",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894798",
                        "runDashboard", false,
                        "languageId", 1
                ),
                Map.of(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "TEST_GOOGLE_ANALYTICS_KEY",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                        "isSystemHost", false,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                )))) {
                                   @Override
                                   public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                                       return false;
                                   }
                               }, new Host(new Contentlet(mapAll(
                Map.of(
                        "hostName", "demo.dotcms.com",
                        "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utmu0",
                        "modDate", Integer.parseInt("125466"),
                        "aliases", "",
                        "keywords", "CMS, Web Content Management, Open Source, Java, J2EE",
                        "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                        "type", "host",
                        "title", "demo.dotcms.com",
                        "inode", "54ac9a4e-3d63-4b9a-882f-27c7ba29618f",
                        "hostname", "demo.dotcms.com"),
                Map.of(
                        "__DOTNAME__", "demo.dotcms.com",
                        "addThis", "TEST_ADD_THIS_KEY",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380c",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894796",
                        "runDashboard", false,
                        "languageId", 1

                ),
                Map.of(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "TEST_GOOGLE_ANALYTICS_KEY",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894797",
                        "isSystemHost", false,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                )
                               ))) {
                                   @Override
                                   public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                                       return false;
                                   }
                               }

        );

        PaginatedArrayList<Host> hosts = new PaginatedArrayList<>();
        hosts.addAll(temp);
        hosts.setTotalResults(2);
        return hosts;
    }

    /**
     * Returns a list of 3 mocked Sites for testing purposes.
     *
     * @return
     */
    private PaginatedArrayList<Host> getNoSites() {
        PaginatedArrayList<Host> hosts = new PaginatedArrayList<>();
        hosts.setTotalResults(0);
        return hosts;
    }
}

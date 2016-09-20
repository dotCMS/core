package com.dotcms.rest.api.v1.site;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link SiteBrowserResource} test
 * @author jsanca
 */
public class SiteBrowserResourceTest extends BaseMessageResources {

    @Test
    public void testNullAndEmptyFilter() throws JSONException, DotSecurityException, DotDataException {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final HostAPI hostAPI     = mock(HostAPI.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final WebResource webResource       = mock(WebResource.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final List<Host> hosts = list(new Host(new Contentlet(mapAll(
                map(
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
                map(
                        "__DOTNAME__", "demo.dotcms.com",
                            "addThis", "ra-4e02119211875e7b",
                            "disabledWYSIWYG", new Object[]{},
                             "host", "SYSTEM_HOST",
                            "lastReview", 14503,
                            "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380c",
                            "owner", "dotcms.org.1",
                            "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894797",
                            "runDashboard", false,
                            "languageId", 1

                    ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
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
        }, new Host(new Contentlet(mapAll(
                map(
                "hostName", "system.dotcms.com",
                "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utru0",
                "modDate", Integer.parseInt("125466"),
                "aliases", "",
                "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                "type", "host",
                "title", "system.dotcms.com",
                "inode", "54ac9a4e-3d63-4b9a-882f-27c7ba29618f",
                "hostname", "system.dotcms.com"),
                map(
                        "__DOTNAME__", "system.dotcms.com",
                        "addThis", "ra-4e02119211875e7b",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "lastReview", 14503,
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894798",
                        "runDashboard", false,
                        "languageId", 1
                        ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                        "isSystemHost", true,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                )))) {
                          @Override
                          public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                              return false;
                          }
        }
        );

        Config.CONTEXT = context;
        Config.CONTEXT = context;


        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(hostAPI.findAll(user, true)).thenReturn(hosts);
        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
        SiteBrowserResource siteBrowserResource =
                new SiteBrowserResource(webResource, new SiteBrowserHelper( hostAPI ), layoutAPI, I18NUtil.INSTANCE);


        Response response1 = siteBrowserResource.sites(request, null, false);
        System.out.println(response1);
        System.out.println(response1.getEntity());

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getEntity() instanceof Map);
        assertNotNull(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result").equals("demo.dotcms.com"));
        assertTrue(Map.class.cast(List.class.cast(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result")).get(0))
                .get("hostName").equals("demo.dotcms.com"));


        response1 = siteBrowserResource.sites(request, StringUtils.EMPTY, false);
        System.out.println(response1);
        System.out.println(response1.getEntity());

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getEntity() instanceof Map);
        assertNotNull(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result").equals("demo.dotcms.com"));
        assertTrue(Map.class.cast(List.class.cast(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result")).get(0))
                .get("hostName").equals("demo.dotcms.com"));

        response1 = siteBrowserResource.sites(request, "*", false);
        System.out.println(response1);
        System.out.println(response1.getEntity());

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getEntity() instanceof Map);
        assertNotNull(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result").equals("demo.dotcms.com"));
        assertTrue(Map.class.cast(List.class.cast(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result")).get(0))
                .get("hostName").equals("demo.dotcms.com"));

    }

    @Test
    public void testPreffixFilter() throws JSONException, DotSecurityException, DotDataException {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final HostAPI hostAPI     = mock(HostAPI.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final WebResource webResource       = mock(WebResource.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final List<Host> hosts = list(new Host(new Contentlet(mapAll(
                map(
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
                map(
                        "__DOTNAME__", "demo.dotcms.com",
                        "addThis", "ra-4e02119211875e7b",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "lastReview", 14503,
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380c",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894797",
                        "runDashboard", false,
                        "languageId", 1

                ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
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
                  }, new Host(new Contentlet(mapAll(
                map(
                        "hostName", "system.dotcms.com",
                        "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utru0",
                        "modDate", Integer.parseInt("125466"),
                        "aliases", "",
                        "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                        "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                        "type", "host",
                        "title", "system.dotcms.com",
                        "inode", "54ac9a4e-3d63-4b9a-882f-27c7ba29618f",
                        "hostname", "system.dotcms.com"),
                map(
                        "__DOTNAME__", "system.dotcms.com",
                        "addThis", "ra-4e02119211875e7b",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "lastReview", 14503,
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894798",
                        "runDashboard", false,
                        "languageId", 1
                ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                        "isSystemHost", true,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                )))) {
                                          @Override
                                          public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                                              return false;
                                          }
                                      }
                , new Host(new Contentlet(mapAll(
                        map(
                                "hostName", "demo.awesome.dotcms.com",
                                "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utru0",
                                "modDate", Integer.parseInt("125466"),
                                "aliases", "",
                                "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                                "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                                "type", "host",
                                "title", "system.dotcms.com",
                                "inode", "54ac9a4e-3d63-4b9a-882f-27c7dba29618f",
                                "hostname", "system.dotcms.com"),
                        map(
                                "__DOTNAME__", "demo.awesome.dotcms.com",
                                "addThis", "ra-4e02119211875e7b",
                                "disabledWYSIWYG", new Object[]{},
                                "host", "SYSTEM_HOST",
                                "lastReview", 14503,
                                "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                                "owner", "dotcms.org.1",
                                "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894798",
                                "runDashboard", false,
                                "languageId", 1
                        ),
                        map(
                                "isDefault", true,
                                "folder", "SYSTEM_FOLDER",
                                "googleAnalytics", "UA-9877660-3",
                                "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                                "isSystemHost", false,
                                "sortOrder", 0,
                                "modUser", "dotcms.org.1"
                        )))) {
                    @Override
                    public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                        return false;
                    }
                }
        );

        Config.CONTEXT = context;
        Config.CONTEXT = context;


        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(hostAPI.findAll(user, true)).thenReturn(hosts);
        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
        SiteBrowserResource siteBrowserResource =
                new SiteBrowserResource(webResource, new SiteBrowserHelper( hostAPI ), layoutAPI, I18NUtil.INSTANCE);


        Response response1 = siteBrowserResource.sites(request, "demo", false);
        System.out.println(response1);
        System.out.println(response1.getEntity());

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getEntity() instanceof Map);
        assertNotNull(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result").equals("demo.dotcms.com"));
        assertTrue(List.class.cast(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result")).size() == 2);
        assertTrue(Map.class.cast(List.class.cast(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result")).get(0))
                .get("hostName").equals("demo.awesome.dotcms.com"));
        assertTrue(Map.class.cast(List.class.cast(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result")).get(1))
                .get("hostName").equals("demo.dotcms.com"));


        response1 = siteBrowserResource.sites(request, "nothing", false);
        System.out.println(response1);
        System.out.println(response1.getEntity());

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getEntity() instanceof Map);
        assertNotNull(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result").equals("demo.dotcms.com"));
        assertTrue(List.class.cast(Map.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("result")).size() == 0);

    }

    @Test
    public void testSwitchNullEmptyAndInvalidFilter() throws JSONException, DotSecurityException, DotDataException {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final HostAPI hostAPI     = mock(HostAPI.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final WebResource webResource       = mock(WebResource.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final List<Host> hosts = list(new Host(new Contentlet(mapAll(
                map(
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
                map(
                        "__DOTNAME__", "demo.dotcms.com",
                        "addThis", "ra-4e02119211875e7b",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "lastReview", 14503,
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380c",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894796",
                        "runDashboard", false,
                        "languageId", 1

                ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
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
                                      }, new Host(new Contentlet(mapAll(
                map(
                        "hostName", "system.dotcms.com",
                        "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utru0",
                        "modDate", Integer.parseInt("125466"),
                        "aliases", "",
                        "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                        "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                        "type", "host",
                        "title", "system.dotcms.com",
                        "inode", "54ac9a4e-3d63-4b9a-882f-27c7ba29618f",
                        "hostname", "system.dotcms.com"),
                map(
                        "__DOTNAME__", "system.dotcms.com",
                        "addThis", "ra-4e02119211875e7b",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "lastReview", 14503,
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894797",
                        "runDashboard", false,
                        "languageId", 1
                ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                        "isSystemHost", true,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                )))) {
                                          @Override
                                          public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                                              return false;
                                          }
                                      }
                , new Host(new Contentlet(mapAll(
                        map(
                                "hostName", "demo.awesome.dotcms.com",
                                "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utru0",
                                "modDate", Integer.parseInt("125466"),
                                "aliases", "",
                                "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                                "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                                "type", "host",
                                "title", "system.dotcms.com",
                                "inode", "54ac9a4e-3d63-4b9a-882f-27c7dba29618f",
                                "hostname", "system.dotcms.com"),
                        map(
                                "__DOTNAME__", "demo.awesome.dotcms.com",
                                "addThis", "ra-4e02119211875e7b",
                                "disabledWYSIWYG", new Object[]{},
                                "host", "SYSTEM_HOST",
                                "lastReview", 14503,
                                "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                                "owner", "dotcms.org.1",
                                "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894798",
                                "runDashboard", false,
                                "languageId", 1
                        ),
                        map(
                                "isDefault", true,
                                "folder", "SYSTEM_FOLDER",
                                "googleAnalytics", "UA-9877660-3",
                                "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                                "isSystemHost", false,
                                "sortOrder", 0,
                                "modUser", "dotcms.org.1"
                        )))) {
                    @Override
                    public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                        return false;
                    }
                }
        );

        Config.CONTEXT = context;


        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(hostAPI.findAll(user, true)).thenReturn(hosts);
        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
        SiteBrowserResource siteBrowserResource =
                new SiteBrowserResource(webResource, new SiteBrowserHelper( hostAPI ), layoutAPI, I18NUtil.INSTANCE);


        Response response1 = siteBrowserResource.switchSite(request, null);
        System.out.println(response1);
        System.out.println(response1.getEntity());

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 404);

        response1 = siteBrowserResource.switchSite(request, StringUtils.EMPTY);
        System.out.println(response1);
        System.out.println(response1.getEntity());

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 404);

        response1 = siteBrowserResource.switchSite(request, "48190c8c-not-found-8d1a-0cd5db894797");
        System.out.println(response1);
        System.out.println(response1.getEntity());

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 404);

        response1 = siteBrowserResource.switchSite(request, "48190c8c-42c4-46af-8d1a-0cd5db894797"); // system, should be not allowed to switch
        System.out.println(response1);
        System.out.println(response1.getEntity());

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 404);

    }

    @Test
    public void testSwitchExistingHost() throws JSONException, DotSecurityException, DotDataException {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final HostAPI hostAPI     = mock(HostAPI.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final WebResource webResource       = mock(WebResource.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final List<Host> hosts = list(new Host(new Contentlet(mapAll(
                map(
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
                map(
                        "__DOTNAME__", "demo.dotcms.com",
                        "addThis", "ra-4e02119211875e7b",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "lastReview", 14503,
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380c",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894796",
                        "runDashboard", false,
                        "languageId", 1

                ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
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
                                      }, new Host(new Contentlet(mapAll(
                map(
                        "hostName", "system.dotcms.com",
                        "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utru0",
                        "modDate", Integer.parseInt("125466"),
                        "aliases", "",
                        "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                        "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                        "type", "host",
                        "title", "system.dotcms.com",
                        "inode", "54ac9a4e-3d63-4b9a-882f-27c7ba29618f",
                        "hostname", "system.dotcms.com"),
                map(
                        "__DOTNAME__", "system.dotcms.com",
                        "addThis", "ra-4e02119211875e7b",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "lastReview", 14503,
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894797",
                        "runDashboard", false,
                        "languageId", 1
                ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                        "isSystemHost", true,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                )))) {
                                          @Override
                                          public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                                              return false;
                                          }
                                      }
                , new Host(new Contentlet(mapAll(
                        map(
                                "hostName", "demo.awesome.dotcms.com",
                                "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utru0",
                                "modDate", Integer.parseInt("125466"),
                                "aliases", "",
                                "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                                "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                                "type", "host",
                                "title", "system.dotcms.com",
                                "inode", "54ac9a4e-3d63-4b9a-882f-27c7dba29618f",
                                "hostname", "system.dotcms.com"),
                        map(
                                "__DOTNAME__", "demo.awesome.dotcms.com",
                                "addThis", "ra-4e02119211875e7b",
                                "disabledWYSIWYG", new Object[]{},
                                "host", "SYSTEM_HOST",
                                "lastReview", 14503,
                                "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                                "owner", "dotcms.org.1",
                                "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894798",
                                "runDashboard", false,
                                "languageId", 1
                        ),
                        map(
                                "isDefault", true,
                                "folder", "SYSTEM_FOLDER",
                                "googleAnalytics", "UA-9877660-3",
                                "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                                "isSystemHost", false,
                                "sortOrder", 0,
                                "modUser", "dotcms.org.1"
                        )))) {
                    @Override
                    public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                        return false;
                    }
                }
        );

        Config.CONTEXT = context;
        Map<String, Object> sessionAttributes = map(WebKeys.CONTENTLET_LAST_SEARCH, "mock mock mock mock");


        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(hostAPI.findAll(user, true)).thenReturn(hosts);
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
                anyObject()
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

        SiteBrowserResource siteBrowserResource =
                new SiteBrowserResource(webResource, new SiteBrowserHelper( hostAPI ), layoutAPI, I18NUtil.INSTANCE);


        Response response1 = siteBrowserResource.switchSite(request, "48190c8c-42c4-46af-8d1a-0cd5db894798");
        System.out.println(response1);
        System.out.println(response1.getEntity());
        System.out.println(sessionAttributes);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertTrue(sessionAttributes.size() == 1 );
        assertTrue(!sessionAttributes.containsKey(WebKeys.CONTENTLET_LAST_SEARCH));
        assertTrue(sessionAttributes.containsKey(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID));


    }

    @Test
    public void testCurrentSites() throws DotSecurityException, DotDataException {
        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        RestUtilTest.initMockContext();
        LayoutAPI layoutAPI = mock(LayoutAPI.class);
        User user = new User();
        WebResource webResource = RestUtilTest.getMockWebResource( user, request );

        List<Host> hosts = getHosts();

        HostAPI hostAPI = mock(HostAPI.class);
        when( hostAPI.findAll(user, Boolean.TRUE) ).thenReturn( hosts );

        HttpSession session = request.getSession();
        String currentSite = hosts.get(0).getIdentifier();
        when( session.getAttribute( WebKeys.CMS_SELECTED_HOST_ID ) )
                .thenReturn( currentSite );

        SiteBrowserResource siteBrowserResource =
                new SiteBrowserResource(webResource, new SiteBrowserHelper( hostAPI ), layoutAPI, I18NUtil.INSTANCE);

        Response response = siteBrowserResource.currentSite(request);

        RestUtilTest.verifySuccessResponse(response);
        Map<String, Object> entity = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals( currentSite, entity.get("currentSite") );

        List<Map<String, String>> sites = (List<Map<String, String>>) entity.get("sites");
        assertEquals(1, sites.size());
        assertEquals(hosts.get(0).getMap(), sites.get(0));
    }

    private List<Host> getHosts() {
        return list(new Host(new Contentlet(mapAll(
                map(
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
                map(
                        "__DOTNAME__", "demo.dotcms.com",
                        "addThis", "ra-4e02119211875e7b",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "lastReview", 14503,
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380c",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894797",
                        "runDashboard", false,
                        "languageId", 1

                ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
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
             }, new Host(new Contentlet(mapAll(
                map(
                        "hostName", "system.dotcms.com",
                        "googleMap", "AIzaSyDXvD7JA5Q8S5VgfviI8nDinAq9x5Utru0",
                        "modDate", Integer.parseInt("125466"),
                        "aliases", "",
                        "keywords", "CMS, System Web Content Management, Open Source, Java, J2EE",
                        "description", "dotCMS starter site was designed to demonstrate what you can do with dotCMS.",
                        "type", "host",
                        "title", "system.dotcms.com",
                        "inode", "54ac9a4e-3d63-4b9a-882f-27c7ba29618f",
                        "hostname", "system.dotcms.com"),
                map(
                        "__DOTNAME__", "system.dotcms.com",
                        "addThis", "ra-4e02119211875e7b",
                        "disabledWYSIWYG", new Object[]{},
                        "host", "SYSTEM_HOST",
                        "lastReview", 14503,
                        "stInode", "855a2d72-f2f3-4169-8b04-ac5157c4380d",
                        "owner", "dotcms.org.1",
                        "identifier", "48190c8c-42c4-46af-8d1a-0cd5db894798",
                        "runDashboard", false,
                        "languageId", 1
                ),
                map(
                        "isDefault", true,
                        "folder", "SYSTEM_FOLDER",
                        "googleAnalytics", "UA-9877660-3",
                        "tagStorage", "48190c8c-42c4-46af-8d1a-0cd5db894799",
                        "isSystemHost", true,
                        "sortOrder", 0,
                        "modUser", "dotcms.org.1"
                )))) {
                 @Override
                 public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
                     return false;
                 }
             }
        );
    }
}

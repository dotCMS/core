package com.dotcms.rest.api.v1.theme;

import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.theme.business.ThemeAPI;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test of {@link ThemeResource}
 */
public class ThemeResourceTest {

    private  static final String FOLDER_1 = "folder_1";
    private  static final String FOLDER_2 = "folder_2";

    private  static final String HOST_1 = "host_1";
    private  static final String HOST_2 = "host_2";

    /**
     * Test of {@link ThemeResource#findThemes(HttpServletRequest, String)}
     *
     * Given: A host_id as query parameter
     * Should: Should create the follow lucene query: +parentpath:/application/themes/* +title:template.vtl host:[host_id]
     */
    @Test
    public void testFindThemesWithHostId() throws DotDataException, DotSecurityException, IOException {
        final String hostId = "1";

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = mock(User.class);
        final User systemUser = mock(User.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final ThemeAPI themeAPI = mock(ThemeAPI.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        final FolderAPI folderAPI = mock(FolderAPI.class);

        final Contentlet content1 = mock(Contentlet.class);
        final Contentlet content2 = mock(Contentlet.class);
        final Host host_1 = mock(Host.class);
        final Folder folder_1 = mock(Folder.class);
        final Host host_2 = mock(Host.class);
        final Folder folder_2 = mock(Folder.class);

        final List<Contentlet> contentlets = list(content1, content2);

        when(content1.getFolder()).thenReturn(FOLDER_1);
        when(content1.getHost()).thenReturn(HOST_1);
        when(content2.getFolder()).thenReturn(FOLDER_2);
        when(content2.getHost()).thenReturn(HOST_2);

        when(folder_1.getName()).thenReturn(FOLDER_1);
        when(folder_2.getName()).thenReturn(FOLDER_2);

        when(host_1.getMap()).thenReturn(ImmutableMap.<String, Object> builder()
            .put("property_1", "value_1")
            .put("property_2", "value_2")
            .build()
        );
        when(host_2.getMap()).thenReturn(ImmutableMap.<String, Object> builder()
                .put("property_1", "value_3")
                .put("property_2", "value_4")
                .build()
        );

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(themeAPI.findAll(user, hostId)).thenReturn(contentlets);

        when(hostAPI.find(HOST_1, systemUser, false)).thenReturn(host_1);
        when(folderAPI.find(FOLDER_1, systemUser, false)).thenReturn(folder_1);
        when(hostAPI.find(HOST_2, systemUser, false)).thenReturn(host_2);
        when(folderAPI.find(FOLDER_2, systemUser, false)).thenReturn(folder_2);

        final ThemeResource themeResource = new ThemeResource(themeAPI, hostAPI, folderAPI, userAPI, webResource);
        final Response response = themeResource.findThemes(request, hostId);

        List responseEntity = new ObjectMapper().readValue(response.getEntity().toString().getBytes(), List.class);

        assertEquals(2, responseEntity.size());
        assertEquals("folder_1", ((Map) responseEntity.get(0)).get("name"));
        assertEquals("value_1", ((Map) ((Map) responseEntity.get(0)).get("host")).get("property_1"));
        assertEquals("value_2", ((Map) ((Map) responseEntity.get(0)).get("host")).get("property_2"));
        assertEquals("folder_2", ((Map) responseEntity.get(1)).get("name"));
        assertEquals("value_3", ((Map) ((Map) responseEntity.get(1)).get("host")).get("property_1"));
        assertEquals("value_4", ((Map) ((Map) responseEntity.get(1)).get("host")).get("property_2"));
    }

    /**
     * Test of {@link ThemeResource#findThemes(HttpServletRequest, String)}
     *
     * Given: null host_id query param
     * Should: Should create the follow lucene query: +parentpath:/application/themes/* +title:template.vtl host:[current_host]
     */
    @Test
    public void testFindThemesDefaultHostId() throws DotDataException, DotSecurityException, IOException {
        final String hostId = "1";

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = mock(User.class);
        final User systemUser = mock(User.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final ThemeAPI themeAPI = mock(ThemeAPI.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        final FolderAPI folderAPI = mock(FolderAPI.class);

        final Contentlet content1 = mock(Contentlet.class);
        final Contentlet content2 = mock(Contentlet.class);
        final Host host_1 = mock(Host.class);
        final Folder folder_1 = mock(Folder.class);
        final Host host_2 = mock(Host.class);
        final Folder folder_2 = mock(Folder.class);

        final List<Contentlet> contentlets = list(content1, content2);

        when(content1.getFolder()).thenReturn(FOLDER_1);
        when(content1.getHost()).thenReturn(HOST_1);
        when(content2.getFolder()).thenReturn(FOLDER_2);
        when(content2.getHost()).thenReturn(HOST_2);

        when(folder_1.getName()).thenReturn(FOLDER_1);
        when(folder_2.getName()).thenReturn(FOLDER_2);

        when(host_1.getMap()).thenReturn(ImmutableMap.<String, Object> builder()
                .put("property_1", "value_1")
                .put("property_2", "value_2")
                .build()
        );
        when(host_2.getMap()).thenReturn(ImmutableMap.<String, Object> builder()
                .put("property_1", "value_3")
                .put("property_2", "value_4")
                .build()
        );

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(themeAPI.findAll(user, hostId )).thenReturn(contentlets);

        when(hostAPI.find(HOST_1, systemUser, false)).thenReturn(host_1);
        when(folderAPI.find(FOLDER_1, systemUser, false)).thenReturn(folder_1);
        when(hostAPI.find(HOST_2, systemUser, false)).thenReturn(host_2);
        when(folderAPI.find(FOLDER_2, systemUser, false)).thenReturn(folder_2);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(hostId);

        final ThemeResource themeResource = new ThemeResource(themeAPI, hostAPI, folderAPI, userAPI, webResource);
        final Response response = themeResource.findThemes(request, null);

        final List responseEntity = new ObjectMapper().readValue(response.getEntity().toString().getBytes(), List.class);

        assertEquals(2, responseEntity.size());
        assertEquals("folder_1", ((Map) responseEntity.get(0)).get("name"));
        assertEquals("value_1", ((Map) ((Map) responseEntity.get(0)).get("host")).get("property_1"));
        assertEquals("value_2", ((Map) ((Map) responseEntity.get(0)).get("host")).get("property_2"));
        assertEquals("folder_2", ((Map) responseEntity.get(1)).get("name"));
        assertEquals("value_3", ((Map) ((Map) responseEntity.get(1)).get("host")).get("property_1"));
        assertEquals("value_4", ((Map) ((Map) responseEntity.get(1)).get("host")).get("property_2"));
    }

    /**
     * Test of {@link ThemeResource#findThemes(HttpServletRequest, String)}
     *
     * Given: a user without permission
     * Should: throw a {@link com.dotcms.rest.exception.ForbiddenException}
     */
    @Test
    public void testFindThemesThrowForbidenException() throws DotDataException {
        final String hostId = "1";

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = mock(User.class);
        final User systemUser = mock(User.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final ThemeAPI themeAPI = mock(ThemeAPI.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        final FolderAPI folderAPI = mock(FolderAPI.class);


        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(userAPI.getSystemUser()).thenReturn(systemUser);

        try {
            when(themeAPI.findAll(user, hostId))
                    .thenThrow(new DotSecurityException(""));
        } catch (DotSecurityException e) {
            assertTrue(false);
        }

        final ThemeResource themeResource = new ThemeResource(themeAPI, hostAPI, folderAPI, userAPI, webResource);

        try {
            final Response response = themeResource.findThemes(request, hostId);
            assertTrue(false);
        }catch (ForbiddenException e) {
            assertTrue(true);
        }
    }
}

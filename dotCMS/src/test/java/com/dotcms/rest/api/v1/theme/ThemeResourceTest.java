package com.dotcms.rest.api.v1.theme;

import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginationException;
import com.dotcms.util.pagination.Paginator;
import com.dotcms.util.pagination.ThemePaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.PaginatedArrayList;

import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * test of {@link ThemeResource}
 */
public class ThemeResourceTest {

    private  static final String FOLDER_1 = "folder_1";
    private  static final String FOLDER_2 = "folder_2";

    private  static final String HOST_1 = "host_1";
    private  static final String HOST_2 = "host_2";

    private final WebResource webResource = mock(WebResource.class);
    private final InitDataObject initDataObject = mock(InitDataObject.class);
    private final User user = mock(User.class);
    private final User systemUser = mock(User.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final UserAPI userAPI = mock(UserAPI.class);
    private final ThemePaginator mockThemePaginator = mock(ThemePaginator.class);
    private final HostAPI hostAPI = mock(HostAPI.class);
    private final FolderAPI folderAPI = mock(FolderAPI.class);

    private final Contentlet content1 = mock(Contentlet.class);
    private final Contentlet content2 = mock(Contentlet.class);
    private final Host host_1 = mock(Host.class);
    private final Folder folder_1 = mock(Folder.class);
    private final Host host_2 = mock(Host.class);
    private final Folder folder_2 = mock(Folder.class);

    private  PaginatedArrayList<Contentlet> contentlets;

    private List expected = list();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void init() throws Throwable{

        contentlets = new PaginatedArrayList<>();
        contentlets.addAll(list(content1, content2));
        contentlets.setTotalResults(4);

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


        when(hostAPI.find(HOST_1, systemUser, false)).thenReturn(host_1);
        when(folderAPI.find(FOLDER_1, systemUser, false)).thenReturn(folder_1);
        when(hostAPI.find(HOST_2, systemUser, false)).thenReturn(host_2);
        when(folderAPI.find(FOLDER_2, systemUser, false)).thenReturn(folder_2);

        when(request.getRequestURL()).thenReturn(new StringBuffer("themes"));
    }
    /**
     * Test of {@link ThemeResource#findThemes(HttpServletRequest, String, int, int, String)}
     *
     * Given: A host_id as query parameter
     * Should: Should create the follow lucene query: +parentpath:/application/themes/* +title:template.vtl host:[host_id]
     */
    @Test
    public void testFindThemesWithHostId() throws Throwable {
        final String hostId = "1";

        Map<String, Object> params = map(
                ThemePaginator.HOST_ID_PARAMETER_NAME, hostId,
                Paginator.DEFAULT_FILTER_PARAM_NAME, null,
                Paginator.ORDER_BY_PARAM_NAME, null,
                Paginator.ORDER_DIRECTION_PARAM_NAME, OrderDirection.ASC
        );
        when(mockThemePaginator.getItems(user, 3, 0, params)).thenReturn(contentlets);

        final ThemeResource themeResource = new ThemeResource(mockThemePaginator, hostAPI, folderAPI, userAPI, webResource);
        final Response response = themeResource.findThemes(request, hostId, 1, 3, "ASC");

        checkSuccessResponse(response);
    }

    /**
     * Test of {@link ThemeResource#findThemes(HttpServletRequest, String, int, int, String)}
     *
     * Given: null host_id query param
     * Should: Should create the follow lucene query: +parentpath:/application/themes/* +title:template.vtl host:[current_host]
     */
    @Test
    public void testFindThemesDefaultHostId() throws Throwable  {
        final String hostId = "1";

        final HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(hostId);

        Map<String, Object> params = map(
                ThemePaginator.HOST_ID_PARAMETER_NAME, hostId,
                Paginator.DEFAULT_FILTER_PARAM_NAME, null,
                Paginator.ORDER_BY_PARAM_NAME, null,
                Paginator.ORDER_DIRECTION_PARAM_NAME, OrderDirection.ASC
        );
        when(mockThemePaginator.getItems(user, 3, 0, params)).thenReturn(contentlets);

        final ThemeResource themeResource = new ThemeResource(mockThemePaginator, hostAPI, folderAPI, userAPI, webResource);
        final Response response = themeResource.findThemes(request, null, 1, 3, "ASC");

        checkSuccessResponse(response);
    }

    /**
     * Test of {@link ThemeResource#findThemes(HttpServletRequest, String, int, int, String)}
     *
     * Given: a user without permission
     * Should: throw a {@link com.dotcms.rest.exception.ForbiddenException}
     */
    @Test
    public void testFindThemesThrowForbidenException() throws Throwable  {
        final String hostId = "1";
        final Exception exception = new PaginationException(new DotSecurityException(""));

        Map<String, Object> params = map(
                ThemePaginator.HOST_ID_PARAMETER_NAME, hostId,
                Paginator.DEFAULT_FILTER_PARAM_NAME, null,
                Paginator.ORDER_BY_PARAM_NAME, null,
                Paginator.ORDER_DIRECTION_PARAM_NAME, OrderDirection.ASC
        );
        when(mockThemePaginator.getItems(user, 3, 0, params)).thenThrow(exception);

        final ThemeResource themeResource = new ThemeResource(mockThemePaginator, hostAPI, folderAPI, userAPI, webResource);

        try {
            themeResource.findThemes(request, hostId, 1, 3, "ASC");
            assertTrue(false);
        } catch(DotSecurityException e){
            assertEquals(exception.getCause(), e);
        }
    }

    protected void checkSuccessResponse(Response response) throws IOException {
        String responseString = response.getEntity().toString();
        JsonNode jsonNode = objectMapper.readTree(responseString);

        List<JsonNode> responseList = CollectionsUtils.asList(jsonNode.get("entity").elements());
        assertEquals(2, responseList.size());
        assertEquals("folder_1", responseList.get(0).get("name").asText());
        assertEquals("value_1", responseList.get(0).get("host").get("property_1").asText());
        assertEquals("value_2", responseList.get(0).get("host").get("property_2").asText());
        assertEquals("folder_2", responseList.get(1).get("name").asText());
        assertEquals("value_3", responseList.get(1).get("host").get("property_1").asText());
        assertEquals("value_4", responseList.get(1).get("host").get("property_2").asText());
    }
}

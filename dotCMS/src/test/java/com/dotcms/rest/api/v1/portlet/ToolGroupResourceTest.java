package com.dotcms.rest.api.v1.portlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the admin gates on {@link ToolGroupResource} layout assignment
 * and removal endpoints.
 */
public class ToolGroupResourceTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private WebResource webResource;
    private User loggedInUser;
    private ToolGroupResource resource;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        webResource = mock(WebResource.class);
        loggedInUser = mock(User.class);

        final InitDataObject initDataObject = mock(InitDataObject.class);
        when(initDataObject.getUser()).thenReturn(loggedInUser);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);

        resource = new ToolGroupResource(webResource);
    }

    @Test
    public void deleteToolGroupFromUser_rejectsNonAdmin() {
        when(loggedInUser.isAdmin()).thenReturn(false);

        assertThrows(DotSecurityException.class,
                () -> resource.deleteToolGroupFromUser(request, response, "someLayout", null));
    }

    @Test
    public void addToolGroupToUser_rejectsNonAdmin() {
        when(loggedInUser.isAdmin()).thenReturn(false);

        assertThrows(DotSecurityException.class,
                () -> resource.addToolGroupToUser(request, response, "someLayout", null));
    }

    @Test
    public void addToolGroupToUser_allowsGettingStartedForNonAdmin() throws Exception {
        when(loggedInUser.isAdmin()).thenReturn(false);
        when(loggedInUser.getUserId()).thenReturn("non-admin-user");

        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final Layout gettingStarted = mock(Layout.class);
        when(layoutAPI.findGettingStartedLayout()).thenReturn(gettingStarted);

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);

            final Response result =
                    resource.addToolGroupToUser(request, response, "GettingStarted", null);

            assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
            verify(layoutAPI).addLayoutForUser(gettingStarted, loggedInUser);
        }
    }

    @Test
    public void addToolGroupToUser_allowsAdmin() throws Exception {
        when(loggedInUser.isAdmin()).thenReturn(true);
        when(loggedInUser.getUserId()).thenReturn("admin-user");

        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final Layout layout = mock(Layout.class);
        when(layoutAPI.findLayout("someLayout")).thenReturn(layout);

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);

            final Response result =
                    resource.addToolGroupToUser(request, response, "someLayout", null);

            assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
            verify(layoutAPI).addLayoutForUser(layout, loggedInUser);
        }
    }

    @Test
    public void deleteToolGroupFromUser_allowsAdmin() throws Exception {
        when(loggedInUser.isAdmin()).thenReturn(true);
        when(loggedInUser.getUserId()).thenReturn("admin-user");
        final Role userRole = mock(Role.class);
        when(loggedInUser.getUserRole()).thenReturn(userRole);

        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        final Layout layout = mock(Layout.class);
        when(layoutAPI.findLayout("someLayout")).thenReturn(layout);

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            final Response result =
                    resource.deleteToolGroupFromUser(request, response, "someLayout", null);

            assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
            verify(roleAPI).removeLayoutFromRole(layout, userRole);
        }
    }

    @Test
    public void deleteToolGroupFromUser_nonAdminCausesNoSideEffects() throws Exception {
        when(loggedInUser.isAdmin()).thenReturn(false);

        final RoleAPI roleAPI = mock(RoleAPI.class);
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            assertThrows(DotSecurityException.class,
                    () -> resource.deleteToolGroupFromUser(request, response, "someLayout", null));

            verify(roleAPI, never()).removeLayoutFromRole(any(), any());
        }
    }
}

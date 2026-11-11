package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Full-list tool updates preserve retired entries without accepting invalid new additions. */
public class LayoutResourceToolsTest {

    private LayoutAPI layoutAPI;
    private PortletAPI portletAPI;
    private RoleAPI roleAPI;
    private Role adminRole;
    private User user;
    private Layout layout;
    private LayoutResource resource;

    /** Distinguishes metadata-only lookup from loading the target's persisted tool membership. */
    @Before
    public void setUp() throws Exception {
        layoutAPI = mock(LayoutAPI.class);
        portletAPI = mock(PortletAPI.class);
        roleAPI = mock(RoleAPI.class);
        adminRole = mock(Role.class);
        user = mock(User.class);
        when(user.getUserId()).thenReturn("admin-user");
        when(roleAPI.loadCMSAdminRole()).thenReturn(adminRole);
        when(roleAPI.doesUserHaveRole(user, adminRole)).thenReturn(true);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);
        layout = new Layout();
        layout.setId("target");
        layout.setName("System");
        layout.setDescription("settings");
        layout.setPortletIds(List.of("templates", "dynamic-plugins", "plugins"));
        final Layout metadata = new Layout();
        metadata.setId(layout.getId());
        metadata.setName(layout.getName());
        when(layoutAPI.findLayout("target")).thenReturn(metadata);
        when(layoutAPI.loadLayout("target")).thenReturn(layout);
        when(layoutAPI.findAllLayouts()).thenReturn(List.of(layout));
        doAnswer(call -> {
            final Layout target = call.getArgument(0);
            final List<String> ids = call.getArgument(1);
            target.setPortletIds(List.copyOf(ids));
            layout.setPortletIds(List.copyOf(ids));
            return null;
        }).when(layoutAPI).setPortletIdsToLayout(any(Layout.class), anyList());
        resource = new LayoutResource(webResource, layoutAPI, portletAPI, roleAPI);
    }

    /** Calls the real resource while keeping its security-log sink out of the unit environment. */
    private Response setTools(final List<String> ids) throws Exception {
        try (MockedStatic<SecurityLogger> ignored = mockStatic(SecurityLogger.class)) {
            return resource.setTools(mock(HttpServletRequest.class), mock(HttpServletResponse.class),
                    layout.getId(), new SectionToolsForm.Builder().portletIds(ids).build());
        }
    }

    /** Reordering loads tool membership omitted by findLayout and retains the legacy entry. */
    @Test
    public void reorder_retainsUnregisteredLegacyTool() throws Exception {
        final List<String> order = List.of("plugins", "dynamic-plugins", "templates");
        final Response response = setTools(order);
        assertEquals(200, response.getStatus());
        verify(layoutAPI).loadLayout("target");
        verify(layoutAPI).setPortletIdsToLayout(layout, order);
        assertEquals(order, ((ResponseEntitySectionListView) response.getEntity()).getEntity().get(0).portletIds());
    }

    /** An orphan can coexist with valid new additions without blocking their placement. */
    @Test
    public void addRegisteredTool_retainsLegacyEntry() throws Exception {
        when(portletAPI.findPortlet("users")).thenReturn(
                new Portlet("users", "com.liferay.portlet.JSPPortlet", Map.of()));
        when(portletAPI.canAddPortletToLayout("users")).thenReturn(true);
        final List<String> ids = List.of("plugins", "dynamic-plugins", "users");
        assertEquals(200, setTools(ids).getStatus());
        verify(layoutAPI).setPortletIdsToLayout(layout, ids);
    }

    /** Another section's legacy membership must not authorize adding an unregistered id here. */
    @Test
    public void addLegacyToolFromDifferentSection_rejectedWithoutWrite() throws Exception {
        layout.setPortletIds(List.of("plugins", "templates"));
        final Layout other = new Layout();
        other.setId("other");
        other.setPortletIds(List.of("dynamic-plugins"));
        when(layoutAPI.findAllLayouts()).thenReturn(List.of(layout, other));
        assertThrows(BadRequestException.class, () -> setTools(List.of("plugins", "dynamic-plugins")));
        verify(layoutAPI, never()).setPortletIdsToLayout(any(Layout.class), anyList());
        assertEquals(List.of("plugins", "templates"), layout.getPortletIds());
    }

    /** New non-placeable tools remain forbidden even when the request preserves a legacy id. */
    @Test
    public void addNonPlaceableTool_rejectedWithoutWrite() throws Exception {
        when(portletAPI.findPortlet("my-account")).thenReturn(
                new Portlet("my-account", "com.liferay.portlet.JSPPortlet", Map.of()));
        assertThrows(BadRequestException.class,
                () -> setTools(List.of("dynamic-plugins", "my-account")));
        verify(layoutAPI, never()).setPortletIdsToLayout(any(Layout.class), anyList());
    }

    /** Existing legacy ids are still subject to duplicate validation. */
    @Test
    public void duplicateLegacyTool_rejectedWithoutWrite() throws Exception {
        assertThrows(BadRequestException.class,
                () -> setTools(List.of("dynamic-plugins", "dynamic-plugins")));
        verify(layoutAPI, never()).setPortletIdsToLayout(any(Layout.class), anyList());
    }

    /** Removal is explicit; no migration or cleanup is needed just to reorder the remaining tools. */
    @Test
    public void removeLegacyTool_succeeds() throws Exception {
        final List<String> ids = List.of("plugins", "templates");
        assertEquals(200, setTools(ids).getStatus());
        verify(layoutAPI).setPortletIdsToLayout(layout, ids);
    }

    /** The Getting Started section must still retain at least one tool. */
    @Test
    public void gettingStarted_cannotBeEmptied() throws Exception {
        layout.setId(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
        when(layoutAPI.findLayout(layout.getId())).thenReturn(layout);
        assertThrows(BadRequestException.class, () -> setTools(List.of()));
        verify(layoutAPI, never()).setPortletIdsToLayout(any(Layout.class), anyList());
    }

    /** Retaining a legacy id does not weaken the administrator gate on editing sections. */
    @Test
    public void nonAdministrator_cannotReorder() throws Exception {
        when(roleAPI.doesUserHaveRole(user, adminRole)).thenReturn(false);
        assertThrows(DotSecurityException.class,
                () -> setTools(List.of("plugins", "dynamic-plugins", "templates")));
        verify(layoutAPI, never()).findLayout("target");
        verify(layoutAPI, never()).loadLayout("target");
        verify(layoutAPI, never()).setPortletIdsToLayout(any(Layout.class), anyList());
    }
}

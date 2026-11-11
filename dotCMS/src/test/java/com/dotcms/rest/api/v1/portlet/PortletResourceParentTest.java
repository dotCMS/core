package com.dotcms.rest.api.v1.portlet;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.portal.DotPortlet;
import com.dotmarketing.business.portal.PortletAPI;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Custom-tool writes retain an editable navigation parent in the persisted portlet definition. */
public class PortletResourceParentTest {

    private PortletAPI portletAPI;
    private PortletResource resource;
    private HttpServletRequest request;
    private User user;

    /** Supplies a base content tool with a parent so new tools must explicitly override it. */
    @Before
    public void setUp() throws Exception {
        portletAPI = mock(PortletAPI.class);
        request = mock(HttpServletRequest.class);
        user = new User();
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);
        when(portletAPI.portletIdPrefixCleaner(anyString())).thenAnswer(call -> call.getArgument(0));
        when(portletAPI.findPortlet("content")).thenReturn(new Portlet("content",
                "com.liferay.portlet.StrutsPortlet", Map.of("view-action", "/content", "parent", "Base")));
        when(portletAPI.savePortlet(any(Portlet.class), eq(user))).thenAnswer(call -> call.getArgument(0));
        resource = new PortletResource(webResource, portletAPI);
    }

    /** Builds a complete tool definition, leaving the optional parent to each scenario. */
    private CustomPortletForm.Builder form() {
        return CustomPortletForm.builder().withPortletId("c_custom").withPortletName("Custom tool")
                .withBaseTypes("CONTENT").withContentTypes("").withDataViewMode("list");
    }

    /** The submitted parent is trimmed, stored and retained through portlet XML serialization. */
    @Test
    public void create_storesParentInDefinition() throws Exception {
        try (MockedStatic<APILocator> locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getPortletAPI).thenReturn(portletAPI);
            final Response response = resource.saveNew(request, form().withParent(" R&D <team> ").build());
            assertEquals(200, response.getStatus());
            final ArgumentCaptor<Portlet> saved = ArgumentCaptor.forClass(Portlet.class);
            verify(portletAPI).savePortlet(saved.capture(), eq(user));
            final DotPortlet roundTrip = DotPortlet.builder().fromXml(DotPortlet.from(saved.getValue()).toXml());
            assertEquals("R&D <team>", roundTrip.initParams().get("parent"));
            assertEquals("/content", roundTrip.initParams().get("view-action"));
        }
    }

    /** An omitted parent creates an ungrouped tool rather than inheriting the base tool's parent. */
    @Test
    public void create_omittedParentIsEmpty() throws Exception {
        try (MockedStatic<APILocator> locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getPortletAPI).thenReturn(portletAPI);
            assertEquals(200, resource.saveNew(request, form().build()).getStatus());
            final ArgumentCaptor<Portlet> saved = ArgumentCaptor.forClass(Portlet.class);
            verify(portletAPI).savePortlet(saved.capture(), eq(user));
            assertEquals("", saved.getValue().getInitParams().get("parent"));
        }
    }

    /** Existing tools can be regrouped, explicitly ungrouped, or updated by an older client. */
    @Test
    public void update_setsClearsOrPreservesParent() throws Exception {
        for (final String parent : new String[] {" New-parent ", "", "   ", null}) {
            setUp();
            final Portlet existing = new Portlet("c_custom", "com.liferay.portlet.StrutsPortlet",
                    Map.of("parent", "Old-parent"));
            when(portletAPI.findPortlet("c_custom")).thenReturn(existing);
            when(portletAPI.isCustomContentPortlet(existing)).thenReturn(true);
            try (MockedStatic<APILocator> locator = mockStatic(APILocator.class)) {
                locator.when(APILocator::getPortletAPI).thenReturn(portletAPI);
                assertEquals(200, resource.updatePortlet(request, form().withParent(parent).build()).getStatus());
                final ArgumentCaptor<Portlet> saved = ArgumentCaptor.forClass(Portlet.class);
                verify(portletAPI).savePortlet(saved.capture(), eq(user));
                assertEquals(parent == null ? "Old-parent" : parent.trim(),
                        saved.getValue().getInitParams().get("parent"));
                assertEquals("c_custom", saved.getValue().getPortletId());
            }
        }
    }

    /** JSON binding and form copies distinguish an omitted parent from an explicit empty value. */
    @Test
    public void form_bindsAndCopiesParent() throws Exception {
        final var mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        final CustomPortletForm submitted = mapper.readValue("{\"parent\":\" Identity-Access \"}", CustomPortletForm.class);
        assertEquals("Identity-Access", submitted.parent);
        assertEquals("Identity-Access", CustomPortletForm.from(submitted).build().parent);
        assertEquals("", mapper.readValue("{\"parent\":\"   \"}", CustomPortletForm.class).parent);
        assertNull(mapper.readValue("{}", CustomPortletForm.class).parent);
    }
}

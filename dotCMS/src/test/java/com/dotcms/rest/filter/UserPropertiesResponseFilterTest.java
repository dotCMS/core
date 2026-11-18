package com.dotcms.rest.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.filter.ContentFieldFilter;
import com.dotcms.rest.api.v1.vtl.VTLResource;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the responses {@link UserPropertiesResponseFilter} leaves to others: the entity already
 * filtered where it was built, and the responses of scripted endpoints.
 *
 * @author hassandotcms
 */
class UserPropertiesResponseFilterTest {

    private static final List<String> SETTINGS = List.of(
            ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY,
            ContentFieldFilter.FIELDS_TO_MASK_PROPERTY,
            ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY);

    private final Map<String, String> originalSettings = new LinkedHashMap<>();

    /** Turns the user properties setting on with empty lists, remembering the previous configuration. */
    @BeforeEach
    void configure() {
        SETTINGS.forEach(name -> originalSettings.put(name, Config.getStringProperty(name, null)));
        Config.setProperty(ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY, "true");
        Config.setProperty(ContentFieldFilter.FIELDS_TO_MASK_PROPERTY, null);
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, null);
    }

    /** Puts back the configuration changed by a test. */
    @AfterEach
    void restoreSettings() {
        originalSettings.forEach(Config::setProperty);
        originalSettings.clear();
    }

    /**
     * Given an entity marked as already filtered, when the response holds that exact entity, then it is left
     * as it is; when the response holds an equal entity that was not marked, then it is filtered.
     */
    @Test
    void skipsOnlyTheEntityMarkedAsFiltered() throws IOException {
        final Map<String, Object> marked = page();
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        UserPropertiesResponseFilter.markFiltered(servletRequest, marked);
        final ArgumentCaptor<String> attribute = ArgumentCaptor.forClass(String.class);
        verify(servletRequest).setAttribute(attribute.capture(), eq(marked));
        final ContainerRequestContext request = request();
        when(request.getProperty(attribute.getValue())).thenReturn(marked);
        final UserPropertiesResponseFilter filter = new UserPropertiesResponseFilter();

        final ContainerResponseContext sameEntity = response(marked);
        filter.filter(request, sameEntity);
        verify(sameEntity, never()).setEntity(any());

        final ContainerResponseContext equalEntity = response(page());
        filter.filter(request, equalEntity);
        final ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(equalEntity).setEntity(sent.capture());
        assertFalse(sent.getValue().toString().contains("dotcms.org.1"));
    }

    /**
     * Given a JSON response written by a scripted endpoint, when it is filtered for an anonymous caller, then
     * it is left as the site wrote it.
     */
    @Test
    void leavesScriptedEndpointResponsesAsWritten() throws Exception {
        final ResourceInfo resource = mock(ResourceInfo.class);
        when(resource.getResourceClass()).thenAnswer(invocation -> VTLResource.class);
        final UserPropertiesResponseFilter filter = new UserPropertiesResponseFilter();
        FieldUtils.writeField(filter, "resourceInfo", resource, true);
        final ContainerResponseContext response = response(Map.of("ticket", Map.of("owner", "Support team")));

        filter.filter(request(), response);

        verify(response, never()).setEntity(any());
        verify(response, never()).setStatus(anyInt());
    }

    /** Returns a response object that is not a contentlet and holds a user property. */
    private static Map<String, Object> page() {
        final Map<String, Object> page = new LinkedHashMap<>();
        page.put("title", "Home");
        page.put("owner", "dotcms.org.1");
        return page;
    }

    /** Returns a request made by an anonymous caller. */
    private static ContainerRequestContext request() {
        final User anonymous = mock(User.class);
        when(anonymous.isAnonymousUser()).thenReturn(true);
        final ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getProperty(WebKeys.USER)).thenReturn(anonymous);
        return request;
    }

    /** Returns a successful JSON response with the given entity. */
    private static ContainerResponseContext response(final Object entity) {
        final ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntity()).thenReturn(entity);
        when(response.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(response.getStatus()).thenReturn(200);
        return response;
    }
}

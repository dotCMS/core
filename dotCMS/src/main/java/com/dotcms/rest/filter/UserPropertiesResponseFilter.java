package com.dotcms.rest.filter;

import com.dotcms.content.filter.ContentFieldFilter;
import com.dotcms.content.filter.PropertyTreeFilter;
import com.dotcms.rendering.js.JsResource;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.vtl.VTLResource;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.Provider;

/**
 * Removes user properties from the JSON responses sent to callers who are neither CMS administrators nor
 * back-end users. The caller is the user stored under {@link com.liferay.portal.util.WebKeys#USER} on the
 * request; a request without one is treated as a non-back-end caller. Non-JSON responses, streamed
 * entities and the responses of scripted Velocity and JavaScript endpoints are never read nor changed.
 * <p>
 * It runs after the other response filters (its priority is lower than
 * {@link Priorities#HEADER_DECORATOR}), so the entity it reads is the one about to be written.
 *
 * @author hassandotcms
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class UserPropertiesResponseFilter implements ContainerResponseFilter {

    /** Request attribute holding the one response entity already filtered for the caller where it was built. */
    private static final String FILTERED_ENTITY_ATTRIBUTE = UserPropertiesResponseFilter.class.getName() + ".filteredEntity";

    @Context
    private ResourceInfo resourceInfo;

    /**
     * Records that a response entity was already filtered for the caller where it was built, so this filter
     * does not read and rebuild it again. Only that exact object is skipped: an entity that wraps or replaces
     * it is filtered as usual.
     *
     * @param request the current request
     * @param entity the filtered response entity
     * @param <T> the entity type
     * @return the same entity
     */
    public static <T> T markFiltered(final HttpServletRequest request, final T entity) {
        request.setAttribute(FILTERED_ENTITY_ATTRIBUTE, entity);
        return entity;
    }

    /**
     * Filters the entity of one response for the caller of the request.
     *
     * @param requestContext the request, which holds the caller
     * @param responseContext the response whose entity may be replaced
     * @throws IOException never thrown; a response that cannot be read is answered with an error instead
     */
    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext) throws IOException {
        if (!responseContext.hasEntity()) {
            return;
        }
        final Object entity = responseContext.getEntity();
        if (null == entity || entity == requestContext.getProperty(FILTERED_ENTITY_ATTRIBUTE)
                || isScriptedEndpoint() || isStream(entity)
                || !isJson(responseContext.getMediaType(), entity)) {
            return;
        }
        final ContentFieldFilter filter = ContentFieldFilter.forUser(caller(requestContext));
        if (!filter.isActive()) {
            return;
        }
        try {
            final ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            final String json = entity instanceof String ? (String) entity : mapper.writeValueAsString(entity);
            if (!mayContain(json, filter.restrictedProperties())) {
                return;
            }
            final Object filtered = PropertyTreeFilter.filter(
                    JsonUtil.getObjectFromJson(json, Object.class), filter, PropertyTreeFilter.storedContentTypes());
            responseContext.setEntity(entity instanceof String ? mapper.writeValueAsString(filtered) : filtered);
        } catch (final Exception e) {
            Logger.warn(this, String.format("The response for '%s' could not be prepared for the caller; "
                    + "an error is returned instead: %s", requestContext.getUriInfo() == null ? null
                    : requestContext.getUriInfo().getPath(), e.getMessage()));
            responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            responseContext.setEntity(null);
        }
    }

    /**
     * Returns the caller of a request: the user stored under {@link WebKeys#USER} when the request was
     * authenticated or resolved as anonymous.
     *
     * @param requestContext the request
     * @return the {@link User}, or {@code null} when none was resolved
     */
    private static User caller(final ContainerRequestContext requestContext) {
        final Object user = requestContext.getProperty(WebKeys.USER);
        return user instanceof User ? (User) user : null;
    }

    /**
     * Tells whether the request was handled by a scripted Velocity or JavaScript endpoint. What those
     * endpoints return is written by the site, so it is sent as the site wrote it.
     *
     * @return {@code true} for a response of {@link VTLResource} or {@link JsResource}
     */
    private boolean isScriptedEndpoint() {
        final Class<?> resourceClass = null == resourceInfo ? null : resourceInfo.getResourceClass();
        return VTLResource.class.equals(resourceClass) || JsResource.class.equals(resourceClass);
    }

    /**
     * Tells whether an entity is written as a stream, which this filter never reads.
     *
     * @param entity the response entity
     * @return {@code true} for streamed, binary and file entities
     */
    private static boolean isStream(final Object entity) {
        return entity instanceof StreamingOutput || entity instanceof InputStream || entity instanceof byte[]
                || entity instanceof File || entity instanceof Reader;
    }

    /**
     * Tells whether a response is JSON. A JSON media type always is. Text without a media type is JSON when
     * it starts like JSON. A response entity view, map or collection is written as JSON under any media type
     * but XML, since resources that declare several media types still return them.
     *
     * @param mediaType the response media type; may be {@code null}
     * @param entity the response entity
     * @return {@code true} for a JSON response
     */
    private static boolean isJson(final MediaType mediaType, final Object entity) {
        if (null != mediaType && (MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType)
                || (null != mediaType.getSubtype() && mediaType.getSubtype().endsWith("+json")))) {
            return true;
        }
        if (entity instanceof String) {
            final String text = ((String) entity).trim();
            return null == mediaType && (text.startsWith("{") || text.startsWith("["));
        }
        return (entity instanceof ResponseEntityView || entity instanceof Map || entity instanceof Collection)
                && !isXml(mediaType);
    }

    /**
     * Tells whether a media type is XML.
     *
     * @param mediaType the response media type; may be {@code null}
     * @return {@code true} for {@code application/xml}, {@code text/xml} and {@code +xml} types
     */
    private static boolean isXml(final MediaType mediaType) {
        return null != mediaType && null != mediaType.getSubtype()
                && ("xml".equalsIgnoreCase(mediaType.getSubtype()) || mediaType.getSubtype().endsWith("+xml"));
    }

    /**
     * Tells whether a JSON text may hold one of the given properties as a key. Text with {@code \}{@code u}
     * character codes is always read, since a key could be written with them.
     *
     * @param json the JSON text
     * @param properties lower-cased property names
     * @return {@code false} only when the text cannot contain any of them
     */
    private static boolean mayContain(final String json, final Set<String> properties) {
        if (json.contains("\\u")) {
            return true;
        }
        final String lowerCased = json.toLowerCase(Locale.ROOT);
        return properties.stream().anyMatch(property -> lowerCased.contains('"' + property + '"'));
    }

}

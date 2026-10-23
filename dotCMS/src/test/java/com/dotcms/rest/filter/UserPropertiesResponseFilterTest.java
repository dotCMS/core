package com.dotcms.rest.filter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.content.filter.ContentFieldFilter;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the REST response filter that removes user properties from JSON responses sent to
 * callers who are neither CMS administrators nor back-end users.
 *
 * @author hassandotcms
 */
class UserPropertiesResponseFilterTest {

    private static final List<String> SETTINGS = List.of(
            ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY,
            ContentFieldFilter.FIELDS_TO_MASK_PROPERTY,
            ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY);

    private static final MediaType JAVASCRIPT = MediaType.valueOf("application/javascript");

    private final Map<String, String> originalSettings = new LinkedHashMap<>();
    /** No content type is stored in a unit test, so every field type is unknown. */
    private final UserPropertiesResponseFilter filter =
            new UserPropertiesResponseFilter(() -> (contentType, fieldVariable) -> null);

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
     * Given a back-end caller and an entity with modUser, when the response is filtered, then the entity
     * is the same instance.
     */
    @Test
    void leavesResponsesForBackendUsersUnchanged() throws IOException {
        final ContainerResponseContext response = response(
                new ResponseEntityView<>(contentWithUserProperties()), MediaType.APPLICATION_JSON_TYPE);

        filter.filter(request(backendUser()), response);

        verify(response, never()).setEntity(any());
        verify(response, never()).setStatus(anyInt());
    }

    /**
     * Given a request without a resolved user, when the response is filtered, then the user properties are
     * removed.
     */
    @Test
    void filtersWhenCallerIsUnknown() throws IOException {
        final ContainerResponseContext response = response(
                new ResponseEntityView<>(contentWithUserProperties()), MediaType.APPLICATION_JSON_TYPE);

        filter.filter(request(null), response);

        final JsonNode sent = sentEntity(response);
        assertFalse(sent.at("/entity").has("modUser"));
        assertFalse(sent.at("/entity").has("owner"));
        assertEquals("Hello", sent.at("/entity/title").asText());
    }

    /**
     * Given an HTML or JavaScript response, when it is filtered, then it is unchanged.
     */
    @Test
    void leavesNonJsonResponsesUnchanged() throws IOException {
        final ContainerResponseContext html = response("<p data-owner=\"x\">modUser</p>", MediaType.TEXT_HTML_TYPE);
        filter.filter(request(anonymous()), html);
        verify(html, never()).setEntity(any());
        verify(html, never()).setStatus(anyInt());

        final ContainerResponseContext script = response("cb({\"modUser\":\"dotcms.org.1\"})", JAVASCRIPT);
        filter.filter(request(anonymous()), script);
        verify(script, never()).setEntity(any());
        verify(script, never()).setStatus(anyInt());
    }

    /**
     * Given a streamed, input-stream or byte-array entity, when it is filtered, then it is unchanged and
     * not consumed.
     */
    @Test
    void leavesStreamedResponsesUnchanged() throws IOException {
        final AtomicBoolean written = new AtomicBoolean(false);
        final StreamingOutput streaming = output -> written.set(true);
        final ContainerResponseContext streamed = response(streaming, MediaType.APPLICATION_JSON_TYPE);
        filter.filter(request(anonymous()), streamed);
        assertFalse(written.get(), "a streamed entity must not be written by the filter");
        verify(streamed, never()).setEntity(any());

        final InputStream input = mock(InputStream.class);
        final ContainerResponseContext stream = response(input, MediaType.APPLICATION_JSON_TYPE);
        filter.filter(request(anonymous()), stream);
        verifyNoInteractions(input);
        verify(stream, never()).setEntity(any());

        final byte[] bytes = "{\"modUser\":\"dotcms.org.1\"}".getBytes(StandardCharsets.UTF_8);
        final byte[] copy = bytes.clone();
        final ContainerResponseContext binary = response(bytes, MediaType.APPLICATION_JSON_TYPE);
        filter.filter(request(anonymous()), binary);
        assertArrayEquals(copy, bytes);
        verify(binary, never()).setEntity(any());
        verify(binary, never()).setStatus(anyInt());
    }

    /**
     * Given a JSON entity that contains no user property name, when it is filtered, then the same entity
     * instance is kept.
     */
    @Test
    void leavesResponsesWithoutUserPropertiesUnchanged() throws IOException {
        final ContainerResponseContext response = response(
                new ResponseEntityView<>(Map.of("identifier", "abc-1", "title", "Hello")),
                MediaType.APPLICATION_JSON_TYPE);

        filter.filter(request(anonymous()), response);

        verify(response, never()).setEntity(any());
        verify(response, never()).setStatus(anyInt());
    }

    /**
     * Given a response entity view of a map with modUser and owner, when it is filtered, then the entity is
     * replaced by one without them.
     */
    @Test
    void removesUserPropertiesFromJsonEntities() throws IOException {
        final ContainerResponseContext response = response(
                new ResponseEntityView<>(contentWithUserProperties()), MediaType.APPLICATION_JSON_TYPE);

        filter.filter(request(anonymous()), response);

        final JsonNode sent = sentEntity(response);
        assertFalse(sent.at("/entity").has("modUser"));
        assertFalse(sent.at("/entity").has("owner"));
        assertEquals("abc-1", sent.at("/entity/identifier").asText());
        assertEquals("Hello", sent.at("/entity/title").asText());
    }

    /**
     * Given a response entity view declared with a non-JSON media type, as resources producing several
     * media types do, when it is filtered, then the user properties are removed; an XML entity is unchanged.
     */
    @Test
    void filtersJsonEntitiesDeclaredWithAnotherMediaType() throws IOException {
        final ContainerResponseContext response = response(
                new ResponseEntityView<>(contentWithUserProperties()), MediaType.valueOf("application/html"));

        filter.filter(request(anonymous()), response);

        final JsonNode sent = sentEntity(response);
        assertFalse(sent.at("/entity").has("modUser"));
        assertEquals("Hello", sent.at("/entity/title").asText());

        final ContainerResponseContext xml = response(contentWithUserProperties(), MediaType.APPLICATION_XML_TYPE);
        filter.filter(request(anonymous()), xml);
        verify(xml, never()).setEntity(any());
        verify(xml, never()).setStatus(anyInt());
    }

    /**
     * Given a text entity holding JSON with modUserName, when it is filtered, then the JSON text no longer
     * has it.
     */
    @Test
    void removesUserPropertiesFromJsonText() throws IOException {
        final ContainerResponseContext response = response(
                "{\"entity\":{\"identifier\":\"abc-1\",\"modUserName\":\"Admin User\",\"title\":\"Hello\"}}",
                MediaType.APPLICATION_JSON_TYPE);

        filter.filter(request(anonymous()), response);

        final JsonNode sent = sentEntity(response);
        assertFalse(sent.at("/entity").has("modUserName"));
        assertEquals("Hello", sent.at("/entity/title").asText());
    }

    /**
     * Given an entity whose serializer writes prebuilt JSON containing owner, when it is filtered, then
     * owner is removed.
     */
    @Test
    void removesUserPropertiesWrittenAsRawJson() throws IOException {
        final ContainerResponseContext response = response(new RawJsonEntity(), MediaType.APPLICATION_JSON_TYPE);

        filter.filter(request(anonymous()), response);

        final JsonNode sent = sentEntity(response);
        assertFalse(sent.at("/page").has("owner"));
        assertEquals("Home", sent.at("/page/title").asText());
    }

    /**
     * Given an entity that cannot be serialized, when it is filtered for a non-back-end caller, then the
     * response is an error and the original entity is not sent.
     */
    @Test
    void failsClosedWhenTheResponseCannotBeRead() throws IOException {
        final UnreadableEntity original = new UnreadableEntity();
        final ContainerResponseContext response = response(original, MediaType.APPLICATION_JSON_TYPE);

        filter.filter(request(anonymous()), response);

        verify(response).setStatus(500);
        final ArgumentCaptor<Object> entity = ArgumentCaptor.forClass(Object.class);
        verify(response, atLeastOnce()).setEntity(entity.capture());
        assertNull(entity.getValue(), "the original entity must not be sent");
    }

    // ---------------------------------------------------------------------------------------------

    /** An entity written by a serializer that emits prebuilt JSON text, as page and container views do. */
    @JsonSerialize(using = RawJsonSerializer.class)
    static final class RawJsonEntity {
    }

    /** Writes {@link RawJsonEntity} with {@link JsonGenerator#writeRawValue(String)}. */
    static final class RawJsonSerializer extends JsonSerializer<RawJsonEntity> {

        /** Writes the entity as a page object built from prebuilt JSON text. */
        @Override
        public void serialize(final RawJsonEntity value, final JsonGenerator generator,
                              final SerializerProvider serializers) throws IOException {
            generator.writeStartObject();
            generator.writeFieldName("page");
            generator.writeRawValue("{\"identifier\":\"page-1\",\"owner\":\"dotcms.org.1\",\"title\":\"Home\"}");
            generator.writeEndObject();
        }
    }

    /** An entity whose only property cannot be read. */
    static final class UnreadableEntity {

        /**
         * Always fails, so the entity cannot be serialized.
         *
         * @return never returns
         */
        public String getModUser() {
            throw new IllegalStateException("not readable");
        }
    }

    /** Returns, as a JSON tree, the entity the filter put in place of the original one. */
    private static JsonNode sentEntity(final ContainerResponseContext response) throws IOException {
        final ArgumentCaptor<Object> entity = ArgumentCaptor.forClass(Object.class);
        verify(response, atLeastOnce()).setEntity(entity.capture());
        verify(response, never()).setStatus(500);
        final Object sent = entity.getValue();
        final String json = sent instanceof String
                ? (String) sent
                : DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValueAsString(sent);
        assertTrue(json.startsWith("{"), "the replacement entity must still be JSON");
        return DotObjectMapperProvider.getInstance().getDefaultObjectMapper().readTree(json);
    }

    /** Returns a request whose caller is the given user; {@code null} means no resolved user. */
    private static ContainerRequestContext request(final User caller) {
        final ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getProperty(WebKeys.USER)).thenReturn(caller);
        return request;
    }

    /** Returns a successful response with the given entity and media type. */
    private static ContainerResponseContext response(final Object entity, final MediaType mediaType) {
        final ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(response.getEntity()).thenReturn(entity);
        when(response.hasEntity()).thenReturn(null != entity);
        when(response.getEntityClass()).thenAnswer(invocation -> null == entity ? null : entity.getClass());
        when(response.getMediaType()).thenReturn(mediaType);
        when(response.getStatus()).thenReturn(200);
        return response;
    }

    /** Returns the anonymous user, without any role lookup. */
    private static User anonymous() {
        final User anonymous = mock(User.class);
        when(anonymous.isAnonymousUser()).thenReturn(true);
        return anonymous;
    }

    /** Returns a back-end user, without any role lookup. */
    private static User backendUser() {
        final User backend = mock(User.class);
        when(backend.isBackendUser()).thenReturn(true);
        return backend;
    }

    /** Returns a content map with modUser and owner. */
    private static Map<String, Object> contentWithUserProperties() {
        final Map<String, Object> content = new LinkedHashMap<>();
        content.put("identifier", "abc-1");
        content.put("inode", "inode-1");
        content.put("contentType", "Blog");
        content.put("title", "Hello");
        content.put("modUser", "dotcms.org.1");
        content.put("owner", "dotcms.org.1");
        return content;
    }
}

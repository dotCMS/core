package com.dotcms.rest.api.v1.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ContentTypeResource#updateContentTypeMetadata}.
 *
 * <p>Covers the basic merge/remove contract as well as the
 * {@code normalizeStyleEditorSchemaToString} path, which coerces a
 * {@code DOT_STYLE_EDITOR_SCHEMA} value received as a JSON object into the JSON string
 * representation that downstream page-rendering code expects.
 */
public class ContentTypeResourceUpdateMetadataTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static ContentTypeResource resource;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new ContentTypeResource();
    }

    // -------------------------------------------------------------------------
    // Basic merge contract
    // -------------------------------------------------------------------------

    /**
     * Given: a content type with no metadata
     * When:  PATCH is called with a new key/value
     * Then:  the key is present in the saved metadata
     */
    @Test
    public void givenNewKey_whenPatch_thenKeyIsAdded() throws Exception {
        final ContentType ct = new ContentTypeDataGen().nextPersisted();
        try {
            final Response response = resource.updateContentTypeMetadata(
                    getHttpRequest(), new EmptyHttpResponse(), ct.id(),
                    Map.of("MY_CUSTOM_KEY", "hello"));

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("hello", getMetadata(response).get("MY_CUSTOM_KEY"));
        } finally {
            ContentTypeDataGen.remove(ct);
        }
    }

    /**
     * Given: a content type with an existing metadata key
     * When:  PATCH is called with the same key and a different value
     * Then:  the key is overwritten with the new value
     */
    @Test
    public void givenExistingKey_whenPatch_thenKeyIsOverwritten() throws Exception {
        final ContentType ct = persistWithMetadata(Map.of("MY_KEY", "original"));
        try {
            final Response response = resource.updateContentTypeMetadata(
                    getHttpRequest(), new EmptyHttpResponse(), ct.id(),
                    Map.of("MY_KEY", "updated"));

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("updated", getMetadata(response).get("MY_KEY"));
        } finally {
            ContentTypeDataGen.remove(ct);
        }
    }

    /**
     * Given: a content type with an existing metadata key
     * When:  PATCH is called with that key set to {@code null}
     * Then:  the key is removed from the saved metadata
     */
    @Test
    public void givenExistingKey_whenPatchWithNullValue_thenKeyIsRemoved() throws Exception {
        final ContentType ct = persistWithMetadata(Map.of("MY_KEY", "value"));
        try {
            final Map<String, Object> patch = new HashMap<>();
            patch.put("MY_KEY", null);

            final Response response = resource.updateContentTypeMetadata(
                    getHttpRequest(), new EmptyHttpResponse(), ct.id(), patch);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertFalse("Key should have been removed", getMetadata(response).containsKey("MY_KEY"));
        } finally {
            ContentTypeDataGen.remove(ct);
        }
    }

    /**
     * Given: a content type with an existing metadata key
     * When:  PATCH is called with a different, unrelated key
     * Then:  the original key is preserved alongside the new one
     */
    @Test
    public void givenExistingMetadata_whenPatchAddsNewKey_thenUnrelatedKeysArePreserved() throws Exception {
        final ContentType ct = persistWithMetadata(Map.of("EXISTING_KEY", "preserved"));
        try {
            final Response response = resource.updateContentTypeMetadata(
                    getHttpRequest(), new EmptyHttpResponse(), ct.id(),
                    Map.of("NEW_KEY", "new_value"));

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            final Map<String, Object> saved = getMetadata(response);
            assertEquals("preserved", saved.get("EXISTING_KEY"));
            assertEquals("new_value", saved.get("NEW_KEY"));
        } finally {
            ContentTypeDataGen.remove(ct);
        }
    }

    /**
     * Given: a content type with metadata
     * When:  PATCH is called with an empty map
     * Then:  the response is 200 and the existing metadata is returned unchanged
     */
    @Test
    public void givenEmptyPatch_whenPatch_thenContentTypeIsReturnedUnchanged() throws Exception {
        final ContentType ct = persistWithMetadata(Map.of("EXISTING", "value"));
        try {
            final Response response = resource.updateContentTypeMetadata(
                    getHttpRequest(), new EmptyHttpResponse(), ct.id(), Map.of());

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("value", getMetadata(response).get("EXISTING"));
        } finally {
            ContentTypeDataGen.remove(ct);
        }
    }

    /**
     * Given: an ID that does not correspond to any content type
     * When:  PATCH is called
     * Then:  the response is 404 Not Found
     */
    @Test
    public void givenUnknownId_whenPatch_thenReturns404() {
        final Response response = resource.updateContentTypeMetadata(
                getHttpRequest(), new EmptyHttpResponse(),
                UUID.randomUUID().toString(), Map.of("KEY", "value"));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    // -------------------------------------------------------------------------
    // normalizeStyleEditorSchemaToString paths
    // -------------------------------------------------------------------------

    /**
     * Given: {@code DOT_STYLE_EDITOR_SCHEMA} is sent as a JSON object (i.e. a {@link Map}),
     *        which is what Jackson produces when the client does not stringify the value
     * When:  PATCH is called
     * Then:  the stored value is a JSON string (not a Map), and the string round-trips back
     *        to the original object correctly
     */
    @Test
    public void givenStyleEditorSchemaAsJsonObject_whenPatch_thenSchemaIsStoredAsString()
            throws Exception {
        final Map<String, Object> schemaObject = Map.of(
                "contentType", "myType",
                "sections", List.of(Map.of("title", "Layout", "fields", List.of()))
        );

        final ContentType ct = new ContentTypeDataGen().nextPersisted();
        try {
            final Map<String, Object> patch = new HashMap<>();
            patch.put("DOT_STYLE_EDITOR_SCHEMA", schemaObject);

            final Response response = resource.updateContentTypeMetadata(
                    getHttpRequest(), new EmptyHttpResponse(), ct.id(), patch);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Object storedValue = getMetadata(response).get("DOT_STYLE_EDITOR_SCHEMA");
            assertNotNull("DOT_STYLE_EDITOR_SCHEMA should be present", storedValue);
            assertTrue("DOT_STYLE_EDITOR_SCHEMA should have been normalized to a String",
                    storedValue instanceof String);

            final Map<?, ?> parsedBack = MAPPER.readValue((String) storedValue, Map.class);
            assertEquals("myType", parsedBack.get("contentType"));
        } finally {
            ContentTypeDataGen.remove(ct);
        }
    }

    /**
     * Given: {@code DOT_STYLE_EDITOR_SCHEMA} is sent already as a JSON string
     * When:  PATCH is called
     * Then:  the stored value equals the original string unchanged
     */
    @Test
    public void givenStyleEditorSchemaAsString_whenPatch_thenSchemaPassesThroughUnchanged()
            throws Exception {
        final String schemaStr = "{\"contentType\":\"myType\",\"sections\":[]}";
        final ContentType ct = new ContentTypeDataGen().nextPersisted();
        try {
            final Response response = resource.updateContentTypeMetadata(
                    getHttpRequest(), new EmptyHttpResponse(), ct.id(),
                    Map.of("DOT_STYLE_EDITOR_SCHEMA", schemaStr));

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals(schemaStr, getMetadata(response).get("DOT_STYLE_EDITOR_SCHEMA"));
        } finally {
            ContentTypeDataGen.remove(ct);
        }
    }

    /**
     * Given: a content type that already has {@code DOT_STYLE_EDITOR_SCHEMA} in its metadata
     * When:  PATCH is called with that key set to {@code null}
     * Then:  the key is removed from the saved metadata
     */
    @Test
    public void givenStyleEditorSchemaPresent_whenPatchWithNullValue_thenKeyIsRemoved()
            throws Exception {
        final ContentType ct = persistWithMetadata(
                Map.of("DOT_STYLE_EDITOR_SCHEMA", "{\"contentType\":\"myType\"}"));
        try {
            final Map<String, Object> patch = new HashMap<>();
            patch.put("DOT_STYLE_EDITOR_SCHEMA", null);

            final Response response = resource.updateContentTypeMetadata(
                    getHttpRequest(), new EmptyHttpResponse(), ct.id(), patch);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertNull("DOT_STYLE_EDITOR_SCHEMA should have been removed",
                    getMetadata(response).get("DOT_STYLE_EDITOR_SCHEMA"));
        } finally {
            ContentTypeDataGen.remove(ct);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates and persists a content type with the given metadata map.
     */
    private static ContentType persistWithMetadata(final Map<String, Object> metadata)
            throws Exception {
        final ContentType base = new ContentTypeDataGen().nextPersisted();
        final ContentType withMeta = ContentTypeBuilder.builder(base).metadata(metadata).build();
        return APILocator.getContentTypeAPI(APILocator.getUserAPI().getSystemUser(), true)
                .save(withMeta);
    }

    /**
     * Extracts the {@code metadata} map from a {@link ResponseEntityContentTypeDetailView}
     * response body.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMetadata(final Response response) {
        final Map<String, Object> entity =
                (Map<String, Object>) ((ResponseEntityView<?>) response.getEntity()).getEntity();
        return (Map<String, Object>) entity.get("metadata");
    }

    private static HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request()
                        ).request()
                ).request()
        );
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(
                        "admin@dotcms.com:admin".getBytes()));
        return request;
    }
}
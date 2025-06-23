package com.dotcms.graphql.datafetcher.page;

import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.datafetcher.ContentMapDataFetcher;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import graphql.schema.DataFetchingEnvironment;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Integration tests for {@link ContentMapDataFetcher}.
 * <p>
 * These tests verify that:
 * - Raw fields (ending in "_raw") are preserved as strings
 * - Corresponding base fields are replaced with parsed JSON if they exist
 * - No new base fields are added unless explicitly present in the contentlet
 */
public class ContentMapDataFetcherTest {

    private static UserAPI userAPI;
    private static User user;
    private static Language defaultLanguage;

    /**
     * Initializes the test environment including default user and language.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        userAPI = APILocator.getUserAPI();
        user = userAPI.getSystemUser();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
    }

    /**
     * Given a contentlet with both `blockEditor_raw` and `blockEditor`,
     * When fetched through ContentMapDataFetcher,
     * Then the base field is replaced with the parsed JSON object
     */
    @Test
    @SuppressWarnings("unchecked") // safe cast after instanceof check
    public void testMapIncludesRawAndParsedBlockEditor() throws Exception {
        Contentlet contentlet = TestDataUtils.getNewsContent(true, defaultLanguage.getId(), getNewsLikeContentType().id());
        String rawJson = "{\"type\":\"doc\",\"attrs\":{\"charCount\":4}}";
        contentlet.setStringProperty("blockEditor_raw", rawJson);
        contentlet.setStringProperty("blockEditor", "some string"); // base field pre-exists

        var environment = mock(DataFetchingEnvironment.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        DotGraphQLContext context = mock(DotGraphQLContext.class);
        when(context.getHttpServletRequest()).thenReturn(request);
        when(context.getHttpServletResponse()).thenReturn(response);
        when(context.getUser()).thenReturn(user);

        when(environment.getContext()).thenReturn(context);
        when(environment.getArgument("key")).thenReturn(null);
        when(environment.getArgument("depth")).thenReturn(0);
        when(environment.getArgument("render")).thenReturn(false);
        when(environment.getSource()).thenReturn(contentlet);

        var fetcher = new ContentMapDataFetcher();
        Object result = fetcher.get(environment);

        assertNotNull(result);
        assertTrue("Expected result to be a Map", result instanceof Map);

        Map<String, Object> map = (Map<String, Object>) result;

        assertTrue(map.containsKey("blockEditor_raw"));
        assertTrue(map.get("blockEditor_raw") instanceof String);
        assertEquals(rawJson, map.get("blockEditor_raw"));

        assertTrue(map.containsKey("blockEditor"));
        assertTrue(map.get("blockEditor") instanceof Map);

        Map<String, Object> parsed = (Map<String, Object>) map.get("blockEditor");
        assertEquals("doc", parsed.get("type"));
        assertTrue(parsed.containsKey("attrs"));
    }

    /**
     * Given a contentlet with only a `customField_raw`,
     * When fetched through ContentMapDataFetcher,
     * Then the base field `customField` should NOT be present in the result
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testRawFieldDoesNotAddMissingBaseField() throws Exception {
        Contentlet contentlet = TestDataUtils.getNewsContent(true, defaultLanguage.getId(), getNewsLikeContentType().id());
        String rawJson = "{\"foo\": \"bar\"}";
        contentlet.setStringProperty("customField_raw", rawJson); // base field not set

        var environment = mock(DataFetchingEnvironment.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        DotGraphQLContext context = mock(DotGraphQLContext.class);
        when(context.getHttpServletRequest()).thenReturn(request);
        when(context.getHttpServletResponse()).thenReturn(response);
        when(context.getUser()).thenReturn(user);

        when(environment.getContext()).thenReturn(context);
        when(environment.getArgument("key")).thenReturn(null);
        when(environment.getArgument("depth")).thenReturn(0);
        when(environment.getArgument("render")).thenReturn(false);
        when(environment.getSource()).thenReturn(contentlet);

        var fetcher = new ContentMapDataFetcher();
        Object result = fetcher.get(environment);

        assertNotNull(result);
        assertTrue(result instanceof Map);

        Map<String, Object> map = (Map<String, Object>) result;

        assertTrue(map.containsKey("customField_raw"));
        assertEquals(rawJson, map.get("customField_raw"));
        assertFalse("Parsed base field should not be added", map.containsKey("customField"));
    }

    /**
     * Verifies that when no variant is set in the request,
     * the contentlet is hydrated using the default variant implicitly.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void returnsMapWhenVariantAttributeIsNull() throws Exception {
        // Mock request with no variant attribute
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(VariantAPI.VARIANT_KEY)).thenReturn(null);

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Mock GraphQL context
        DotGraphQLContext context = mock(DotGraphQLContext.class);
        when(context.getHttpServletRequest()).thenReturn(request);
        when(context.getHttpServletResponse()).thenReturn(response);
        when(context.getUser()).thenReturn(user);

        // Mock GraphQL environment
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        when(environment.getContext()).thenReturn(context);
        when(environment.getArgument("key")).thenReturn(null);
        when(environment.getArgument("depth")).thenReturn(0);
        when(environment.getArgument("render")).thenReturn(false);

        // Valid contentlet with default variant data
        Contentlet contentlet = TestDataUtils.getNewsContent(true, defaultLanguage.getId(), getNewsLikeContentType().id());
        when(environment.getSource()).thenReturn(contentlet);

        // Execute data fetcher
        ContentMapDataFetcher fetcher = new ContentMapDataFetcher();
        Object result = fetcher.get(environment);

        // Validate response
        assertNotNull(result);
        assertTrue(result instanceof Map);

        Map<String, Object> map = (Map<String, Object>) result;
        assertFalse("Hydrated map should not be empty", map.isEmpty());
    }

    /**
     * Verifies that when the DEFAULT variant is explicitly set in the request,
     * the contentlet is hydrated successfully and no fallback occurs.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void returnsMapWhenVariantIsDefault() throws Exception {
        // Mock request explicitly setting the DEFAULT variant
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(VariantAPI.VARIANT_KEY)).thenReturn(VariantAPI.DEFAULT_VARIANT.name());

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Mock context and environment
        DotGraphQLContext context = mock(DotGraphQLContext.class);
        when(context.getHttpServletRequest()).thenReturn(request);
        when(context.getHttpServletResponse()).thenReturn(response);
        when(context.getUser()).thenReturn(user);

        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        when(environment.getContext()).thenReturn(context);
        when(environment.getArgument("key")).thenReturn(null);
        when(environment.getArgument("depth")).thenReturn(0);
        when(environment.getArgument("render")).thenReturn(false);

        // Valid contentlet with data in DEFAULT variant
        Contentlet contentlet = TestDataUtils.getNewsContent(true, defaultLanguage.getId(), getNewsLikeContentType().id());
        when(environment.getSource()).thenReturn(contentlet);

        // Execute data fetcher
        ContentMapDataFetcher fetcher = new ContentMapDataFetcher();
        Object result = fetcher.get(environment);

        // Validate
        assertNotNull(result);
        assertTrue(result instanceof Map);

        Map<String, Object> map = (Map<String, Object>) result;
        assertFalse("Hydrated map should not be empty", map.isEmpty());
    }

    /**
     * Verifies that when a non-existent variant is set in the request,
     * the fetcher falls back to the DEFAULT variant and still returns content.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void fallsBackToDefaultWhenVariantFails() throws Exception {
        // Mock request with a non-existent variant
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(VariantAPI.VARIANT_KEY)).thenReturn("non-existent-variant");

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Mock context and environment
        DotGraphQLContext context = mock(DotGraphQLContext.class);
        when(context.getHttpServletRequest()).thenReturn(request);
        when(context.getHttpServletResponse()).thenReturn(response);
        when(context.getUser()).thenReturn(user);

        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        when(environment.getContext()).thenReturn(context);
        when(environment.getArgument("key")).thenReturn(null);
        when(environment.getArgument("depth")).thenReturn(0);
        when(environment.getArgument("render")).thenReturn(false);

        // Valid contentlet that only exists under DEFAULT variant
        Contentlet contentlet = TestDataUtils.getNewsContent(true, defaultLanguage.getId(), getNewsLikeContentType().id());
        contentlet.setVariantId("non-existent-variant");
        when(environment.getSource()).thenReturn(contentlet);

        // Execute data fetcher
        ContentMapDataFetcher fetcher = new ContentMapDataFetcher();
        Object result = fetcher.get(environment);

        // Validate
        assertNotNull(result);
        assertTrue(result instanceof Map);

        Map<String, Object> map = (Map<String, Object>) result;
        assertFalse("Hydrated map should not be empty even after fallback", map.isEmpty());
    }

}

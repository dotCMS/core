package com.dotcms.graphql.datafetcher.page;

import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static org.junit.Assert.*;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.datafetcher.ContentMapDataFetcher;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import graphql.schema.DataFetchingEnvironment;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

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

        var environment = Mockito.mock(DataFetchingEnvironment.class);
        Mockito.when(environment.getContext()).thenReturn(
                DotGraphQLContext.createServletContext().with(user).build()
        );
        Mockito.when(environment.getArgument("key")).thenReturn(null);
        Mockito.when(environment.getArgument("depth")).thenReturn(0);
        Mockito.when(environment.getArgument("render")).thenReturn(false);
        Mockito.when(environment.getSource()).thenReturn(contentlet);

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

        var environment = Mockito.mock(DataFetchingEnvironment.class);
        Mockito.when(environment.getContext()).thenReturn(
                DotGraphQLContext.createServletContext().with(user).build()
        );
        Mockito.when(environment.getArgument("key")).thenReturn(null);
        Mockito.when(environment.getArgument("depth")).thenReturn(0);
        Mockito.when(environment.getArgument("render")).thenReturn(false);
        Mockito.when(environment.getSource()).thenReturn(contentlet);

        var fetcher = new ContentMapDataFetcher();
        Object result = fetcher.get(environment);

        assertNotNull(result);
        assertTrue(result instanceof Map);

        Map<String, Object> map = (Map<String, Object>) result;

        assertTrue(map.containsKey("customField_raw"));
        assertEquals(rawJson, map.get("customField_raw"));
        assertFalse("Parsed base field should not be added", map.containsKey("customField"));
    }
}

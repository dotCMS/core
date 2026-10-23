package com.dotcms.content.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotmarketing.util.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the user properties setting of {@link ContentFieldFilter} and for its checks on
 * JSON search bodies. The content property lists themselves are covered by
 * {@code ContentFieldFilterTest}.
 *
 * @author hassandotcms
 */
class ContentFieldFilterUserPropertiesTest {

    private static final List<String> SETTINGS = List.of(
            ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY,
            ContentFieldFilter.FIELDS_TO_MASK_PROPERTY,
            ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, String> originalSettings = new LinkedHashMap<>();

    /** Remembers the settings this class changes, so every test starts and ends from the same configuration. */
    @BeforeEach
    void rememberSettings() {
        SETTINGS.forEach(name -> originalSettings.put(name, Config.getStringProperty(name, null)));
        Config.setProperty(ContentFieldFilter.FIELDS_TO_MASK_PROPERTY, null);
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, null);
    }

    /** Puts back the settings changed by a test. */
    @AfterEach
    void restoreSettings() {
        originalSettings.forEach(Config::setProperty);
        originalSettings.clear();
    }

    // ---------------------------------------------------------------------------------------------
    // User properties setting (CONTENT_API_HIDE_USER_PROPERTIES)
    // ---------------------------------------------------------------------------------------------

    /**
     * Given the user properties setting is on and both lists are empty, when the filter for an anonymous
     * caller is applied to a content map, then modUser, owner and modUserName are removed.
     */
    @Test
    void userPropertiesSettingHidesUserProperties() {
        settingIs(true);

        final ContentFieldFilter filter = ContentFieldFilter.forUser(anonymous());
        assertTrue(filter.isActive(), "the setting alone must make the filter active");

        final Map<String, Object> content = filter.apply(content());
        assertFalse(content.containsKey("modUser"));
        assertFalse(content.containsKey("owner"));
        assertFalse(content.containsKey("modUserName"));
        assertEquals("Hello", content.get("title"));
        assertEquals("abc-123", content.get("identifier"));
    }

    /**
     * Given the setting is off and both lists are empty, when the filter for an anonymous caller is built,
     * then it changes nothing.
     */
    @Test
    void settingOffAndNoListsChangesNothing() {
        settingIs(false);

        final ContentFieldFilter filter = ContentFieldFilter.forUser(anonymous());
        assertFalse(filter.isActive());
        assertEquals(content(), filter.apply(content()));
        assertTrue(filter.allowsSearch("+owner:user-0*", "modUser desc"));
    }

    /**
     * Given the setting is on, when the filter for a back-end user is built, then it changes nothing.
     */
    @Test
    void settingOnLeavesBackendUsersUnfiltered() {
        settingIs(true);

        final ContentFieldFilter filter = ContentFieldFilter.forUser(backendUser());
        assertFalse(filter.isActive());
        assertEquals(content(), filter.apply(content()));
        assertTrue(filter.allowsSearch("+owner:user-0*", "modUser desc"));
    }

    /**
     * Given the setting is on and modUser and title are listed to mask, when a content map is filtered,
     * then modUser is removed and title is masked.
     */
    @Test
    void userPropertiesAreRemovedEvenWhenListedToMask() {
        settingIs(true);
        Config.setProperty(ContentFieldFilter.FIELDS_TO_MASK_PROPERTY, "modUser,title");

        final Map<String, Object> content = ContentFieldFilter.forUser(anonymous()).apply(content());
        assertFalse(content.containsKey("modUser"), "a user property is removed, not masked");
        assertEquals(ContentFieldFilter.mask("Hello"), content.get("title"));
    }

    /**
     * Given the setting is on and tags is listed to hide, when a content map is filtered, then tags and the
     * user properties are removed.
     */
    @Test
    void settingAddsToConfiguredHideList() {
        settingIs(true);
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, "tags");

        final Map<String, Object> content = ContentFieldFilter.forUser(anonymous()).apply(content());
        assertFalse(content.containsKey("tags"));
        for (final String userProperty : List.of("owner", "ownerUserName", "modUser", "modUserName",
                "publishUser", "publishUserName", "lockedBy", "lockedByName", "wfCreatedBy", "wfAssign")) {
            assertFalse(content.containsKey(userProperty), userProperty + " must be removed");
        }
        assertEquals("Hello", content.get("title"));
    }

    /**
     * Given the setting has no value in the environment or the system table, when the filter for an
     * anonymous caller is built, then the value shipped in dotmarketing-config.properties (on) applies.
     */
    @Test
    void settingDefaultsToTheShippedConfigurationValue() throws IOException {
        final Properties shipped = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("dotmarketing-config.properties")) {
            assertNotNull(in, "dotmarketing-config.properties must be on the classpath");
            shipped.load(in);
        }
        assertEquals("true", shipped.getProperty(ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY),
                "the setting must ship turned on");

        final ContentFieldFilter filter = ContentFieldFilter.forUser(anonymous());
        assertTrue(filter.isActive(), "with no override the shipped value applies");
        assertFalse(filter.apply(content()).containsKey("modUser"));
    }

    /**
     * Given the setting is on, when an anonymous caller queries a user property with a wildcard, then the
     * search is not allowed.
     */
    @Test
    void searchOnUserPropertiesIsRefusedWhenSettingOn() {
        settingIs(true);

        final ContentFieldFilter filter = ContentFieldFilter.forUser(anonymous());
        assertFalse(filter.allowsSearch("+owner:user-0*", null));
        assertFalse(filter.allowsSearch("+contentType:Blog", "modUser desc"));
        assertTrue(filter.allowsSearch("+contentType:Blog +title:owner", "title asc"));
    }

    /**
     * Given the setting is on, when an anonymous caller queries a workflow user field, then the search is
     * not allowed.
     */
    @Test
    void searchOnWorkflowUserFieldsIsRefused() {
        settingIs(true);

        final ContentFieldFilter filter = ContentFieldFilter.forUser(anonymous());
        assertFalse(filter.allowsSearch("+wfcreatedby:x*", null));
        assertFalse(filter.allowsSearch("+wfAssign:x*", null));
    }

    // ---------------------------------------------------------------------------------------------
    // JSON search bodies (/api/es/search, /api/es/raw)
    // ---------------------------------------------------------------------------------------------

    /** Given an active filter, when a body aggregates on contenttype only, then it is allowed. */
    @Test
    void jsonSearchAllowsUnrelatedQueries() {
        final ContentFieldFilter filter = activeFilter();
        assertTrue(filter.allowsJsonSearch(
                "{\"query\":{\"match_all\":{}},\"aggs\":{\"t\":{\"terms\":{\"field\":\"contenttype\"}}}}"));
        assertTrue(filter.allowsJsonSearch(
                "{\"query\":{\"bool\":{\"must\":[{\"term\":{\"title\":\"hello\"}}]}},\"sort\":[{\"moddate\":\"desc\"}]}"));
    }

    /** Given a null or empty body, or an inactive filter, when the body is checked, then it is allowed. */
    @Test
    void jsonSearchAllowsEmptyBodyAndInactiveFilter() {
        final ContentFieldFilter active = activeFilter();
        assertTrue(active.allowsJsonSearch(null));
        assertTrue(active.allowsJsonSearch(""));

        settingIs(false);
        final ContentFieldFilter inactive = ContentFieldFilter.forUser(anonymous());
        assertFalse(inactive.isActive());
        assertTrue(inactive.allowsJsonSearch("{\"aggs\":{\"u\":{\"terms\":{\"field\":\"moduser_dotraw\"}}}}"));
    }

    /** Given an active filter, when a body aggregates on a restricted index field, then it is not allowed. */
    @Test
    void jsonSearchRefusesAggregationOnRestrictedField() {
        assertFalse(activeFilter().allowsJsonSearch(
                "{\"size\":0,\"aggs\":{\"u\":{\"terms\":{\"field\":\"moduser_dotraw\",\"size\":100}}}}"));
    }

    /** Given an active filter, when a body sorts on a restricted field, then it is not allowed. */
    @Test
    void jsonSearchRefusesSortOnRestrictedField() {
        final ContentFieldFilter filter = activeFilter();
        assertFalse(filter.allowsJsonSearch("{\"query\":{\"match_all\":{}},\"sort\":[{\"owner\":\"asc\"}]}"));
        assertFalse(filter.allowsJsonSearch("{\"query\":{\"match_all\":{}},\"sort\":\"owner\"}"));
    }

    /** Given an active filter, when a query clause is keyed by a restricted field name, then it is not allowed. */
    @Test
    void jsonSearchRefusesQueryClauseKeyedByRestrictedField() {
        assertFalse(activeFilter().allowsJsonSearch("{\"query\":{\"prefix\":{\"owner\":\"user-0\"}}}"));
    }

    /** Given an active filter, when a query_string text names a restricted field, then it is not allowed. */
    @Test
    void jsonSearchRefusesRestrictedFieldInQueryString() {
        final ContentFieldFilter filter = activeFilter();
        assertFalse(filter.allowsJsonSearch("{\"query\":{\"query_string\":{\"query\":\"+modUser:x*\"}}}"));
        assertFalse(filter.allowsJsonSearch(
                "{\"query\":{\"simple_query_string\":{\"query\":\"x*\",\"fields\":[\"title\",\"owner\"]}}}"));
        assertFalse(filter.allowsJsonSearch(
                "{\"query\":{\"query_string\":{\"query\":\"x*\",\"default_field\":\"moduser\"}}}"));
    }

    /** Given an active filter, when a body contains a script, then it is not allowed. */
    @Test
    void jsonSearchRefusesScripts() {
        final ContentFieldFilter filter = activeFilter();
        assertFalse(filter.allowsJsonSearch(
                "{\"query\":{\"script_score\":{\"query\":{\"match_all\":{}},\"script\":{\"source\":\"1\"}}}}"));
        assertFalse(filter.allowsJsonSearch(
                "{\"query\":{\"match_all\":{}},\"script_fields\":{\"x\":{\"script\":{\"source\":\"1\"}}}}"));
    }

    /** Given an active filter, when a body defines runtime mappings, then it is not allowed. */
    @Test
    void jsonSearchRefusesRuntimeMappings() {
        assertFalse(activeFilter().allowsJsonSearch(
                "{\"runtime_mappings\":{\"x\":{\"type\":\"keyword\"}},\"query\":{\"match_all\":{}}}"));
    }

    /** Given an active filter, when a restricted field is written in mixed case, then it is still not allowed. */
    @Test
    void jsonSearchIgnoresCase() {
        final ContentFieldFilter filter = activeFilter();
        assertFalse(filter.allowsJsonSearch("{\"aggs\":{\"u\":{\"terms\":{\"field\":\"ModUser_DotRaw\"}}}}"));
        assertFalse(filter.allowsJsonSearch("{\"query\":{\"prefix\":{\"OWNER\":\"user-0\"}}}"));
    }

    /**
     * Given an active filter, when a body uses a field whose name only starts like a restricted one, or a
     * value equal to a restricted name, then it is allowed.
     */
    @Test
    void jsonSearchDoesNotMatchSimilarNamesOrValues() {
        final ContentFieldFilter filter = activeFilter();
        assertTrue(filter.allowsJsonSearch("{\"aggs\":{\"u\":{\"terms\":{\"field\":\"ownername\"}}}}"));
        assertTrue(filter.allowsJsonSearch("{\"query\":{\"term\":{\"title\":\"owner\"}}}"));
    }

    /**
     * Given an active filter, when a restricted field appears inside composite sources or multi_terms
     * arrays, then it is not allowed.
     */
    @Test
    void jsonSearchRefusesRestrictedFieldInsideArrays() {
        final ContentFieldFilter filter = activeFilter();
        assertFalse(filter.allowsJsonSearch("{\"aggs\":{\"c\":{\"composite\":{\"sources\":["
                + "{\"t\":{\"terms\":{\"field\":\"contenttype\"}}},{\"x\":{\"terms\":{\"field\":\"owner\"}}}]}}}}"));
        assertFalse(filter.allowsJsonSearch("{\"aggs\":{\"m\":{\"multi_terms\":{\"terms\":["
                + "{\"field\":\"contenttype\"},{\"field\":\"moduser\"}]}}}}"));
    }

    /**
     * Given a body with a top_hits aggregation without _source, when it is restricted, then its _source is
     * identifier and inode and the rest of the body is unchanged.
     */
    @Test
    void topHitsSourceIsLimitedToIdentifierAndInode() throws IOException {
        final String body = "{\"size\":0,\"query\":{\"term\":{\"contenttype\":\"blog\"}},"
                + "\"aggs\":{\"t\":{\"top_hits\":{\"size\":3}},"
                + "\"by\":{\"terms\":{\"field\":\"contenttype\"},\"aggs\":{\"n\":{\"top_hits\":{\"_source\":[\"title\"]}}}}}}";

        final JsonNode restricted = MAPPER.readTree(activeFilter().restrictTopHits(body));

        assertEquals(MAPPER.readTree("[\"identifier\",\"inode\"]"), restricted.at("/aggs/t/top_hits/_source"));
        assertEquals(3, restricted.at("/aggs/t/top_hits/size").asInt());
        assertEquals(MAPPER.readTree("[\"identifier\",\"inode\"]"),
                restricted.at("/aggs/by/aggs/n/top_hits/_source"));
        assertEquals(MAPPER.readTree("{\"term\":{\"contenttype\":\"blog\"}}"), restricted.get("query"));
        assertEquals("contenttype", restricted.at("/aggs/by/terms/field").asText());
        assertEquals(0, restricted.get("size").asInt());
    }

    // ---------------------------------------------------------------------------------------------

    /** Turns the user properties setting on or off. */
    private static void settingIs(final boolean on) {
        Config.setProperty(ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY, Boolean.toString(on));
    }

    /** Turns the setting on and returns the filter of an anonymous caller, which must be active. */
    private static ContentFieldFilter activeFilter() {
        settingIs(true);
        final ContentFieldFilter filter = ContentFieldFilter.forUser(anonymous());
        assertTrue(filter.isActive(), "the setting must make the filter active for an anonymous caller");
        return filter;
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

    /** Returns a content map holding every user property and a few other properties. */
    private static Map<String, Object> content() {
        final Map<String, Object> content = new LinkedHashMap<>();
        content.put("identifier", "abc-123");
        content.put("inode", "def-456");
        content.put("contentType", "Blog");
        content.put("title", "Hello");
        content.put("tags", "news");
        content.put("owner", "dotcms.org.1");
        content.put("ownerUserName", "Admin User");
        content.put("modUser", "dotcms.org.1");
        content.put("modUserName", "Admin User");
        content.put("publishUser", "dotcms.org.1");
        content.put("publishUserName", "Admin User");
        content.put("lockedBy", "dotcms.org.1");
        content.put("lockedByName", "Admin User");
        content.put("wfCreatedBy", "dotcms.org.1");
        content.put("wfAssign", "dotcms.org.1");
        return content;
    }
}

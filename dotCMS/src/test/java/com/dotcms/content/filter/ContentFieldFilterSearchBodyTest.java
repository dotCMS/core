package com.dotcms.content.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.liferay.portal.model.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Elasticsearch search body rules of {@link ContentFieldFilter} when user properties are
 * hidden from callers who are neither CMS administrators nor back-end users.
 *
 * @author hassandotcms
 */
class ContentFieldFilterSearchBodyTest {

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
     * Given an anonymous caller, when a search body uses a user property in an aggregation, a sort, a query
     * clause or the text of a query string, then it is refused.
     */
    @Test
    void refusesSearchBodiesThatUseAUserProperty() {
        final ContentFieldFilter filter = ContentFieldFilter.forUser(anonymous());

        assertFalse(filter.allowsJsonSearch("{\"size\":0,\"aggs\":{\"a\":{\"terms\":{\"field\":\"moduser_dotraw\"}}}}"));
        assertFalse(filter.allowsJsonSearch("{\"sort\":[{\"owner\":\"asc\"}]}"));
        assertFalse(filter.allowsJsonSearch("{\"query\":{\"prefix\":{\"owner\":\"dot\"}}}"));
        assertFalse(filter.allowsJsonSearch("{\"query\":{\"query_string\":{\"query\":\"+sysPublishUser:dot*\"}}}"));
    }

    /**
     * Given an anonymous caller, when a search body uses a script, runtime mappings or the loose syntax the
     * legacy endpoint accepts, then it is refused.
     */
    @Test
    void refusesScriptsRuntimeMappingsAndLooseSyntaxNamingAUserProperty() {
        final ContentFieldFilter filter = ContentFieldFilter.forUser(anonymous());

        assertFalse(filter.allowsJsonSearch(
                "{\"query\":{\"script_score\":{\"query\":{\"match_all\":{}},\"script\":{\"source\":\"1\"}}}}"));
        assertFalse(filter.allowsJsonSearch("{\"runtime_mappings\":{\"x\":{\"type\":\"keyword\"}}}"));
        assertFalse(filter.allowsJsonSearch("{aggs:{u:{terms:{field:'owner_dotraw'}}}}"));
    }

    /**
     * Given an anonymous caller, when a search body uses only content properties, then it is allowed; and a
     * back-end user may run any search.
     */
    @Test
    void allowsOtherSearchesAndBackendUsers() {
        final ContentFieldFilter filter = ContentFieldFilter.forUser(anonymous());

        assertTrue(filter.allowsJsonSearch(
                "{\"query\":{\"query_string\":{\"query\":\"+contenttype:blog\"}},\"aggs\":{\"t\":{\"terms\":{\"field\":\"contenttype\"}}}}"));
        assertTrue(filter.allowsJsonSearch("{\"query\":{\"match\":{\"title\":\"the owner of the house\"}}}"));
        assertTrue(ContentFieldFilter.forUser(backendUser())
                .allowsJsonSearch("{\"aggs\":{\"a\":{\"terms\":{\"field\":\"moduser_dotraw\"}}}}"));
    }

    /**
     * Given an anonymous caller, when a search body has a {@code top_hits} aggregation, then its documents are
     * limited to identifier and inode.
     */
    @Test
    void limitsTopHitsToIdentifierAndInode() throws Exception {
        final String body = ContentFieldFilter.forUser(anonymous()).restrictTopHits(
                "{\"aggs\":{\"t\":{\"top_hits\":{\"size\":2,\"_source\":[\"modUser\"]}}}}");

        final JsonNode source = JsonUtil.JSON_MAPPER.readTree(body).at("/aggs/t/top_hits/_source");
        assertEquals(List.of("identifier", "inode"), List.of(source.get(0).asText(), source.get(1).asText()));
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
}

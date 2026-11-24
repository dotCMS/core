package com.dotcms.content.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.filter.PropertyTreeFilter.FieldTypes;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.StoryBlockField;
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
 * Unit tests for {@link PropertyTreeFilter}: user properties are removed at every depth, including from the
 * contentlets embedded in a Story Block, while the values of fields that hold customer data are kept.
 *
 * @author hassandotcms
 */
class PropertyTreeFilterTest {

    private static final List<String> SETTINGS = List.of(
            ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY,
            ContentFieldFilter.FIELDS_TO_MASK_PROPERTY,
            ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY);

    /** The test content type has a Key/Value field {@code meta} and a Story Block field {@code body}. */
    private static final FieldTypes FIELD_TYPES = (contentType, field) ->
            "meta".equals(field) ? KeyValueField.class.getName()
                    : "body".equals(field) ? StoryBlockField.class.getName() : null;

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
     * Given a contentlet with user properties, related content holding more of them and a Key/Value field
     * with a key named {@code owner}, when it is filtered, then the user properties are sent as the placeholder at
     * every depth and the Key/Value value is kept as stored.
     */
    @Test
    void replacesUserPropertiesAtEveryDepthAndKeepsCustomerFieldValues() {
        final Map<String, Object> related = content("rel-1");
        related.put("lockedBy", Map.of("userId", "dotcms.org.1"));
        final Map<String, Object> contentlet = content("abc-1");
        contentlet.put("meta", Map.of("owner", "customer-value"));
        contentlet.put("rel", List.of(related));

        final Map<?, ?> filtered = (Map<?, ?>) PropertyTreeFilter.filter(contentlet, anonymousFilter(), FIELD_TYPES);

        assertEquals(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER, filtered.get("modUser"));
        assertEquals(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER, filtered.get("ownerUserName"));
        assertEquals(Map.of("owner", "customer-value"), filtered.get("meta"));
        final Map<?, ?> filteredRelated = (Map<?, ?>) ((List<?>) filtered.get("rel")).get(0);
        assertEquals(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER, filteredRelated.get("lockedBy"));
        assertEquals("abc-1", filtered.get("identifier"));
        assertTrue(contentlet.containsKey("modUser"), "the input must not be changed");
    }

    /**
     * Given a Story Block value held as JSON text, and its {@code _raw} copy, that embed a contentlet with
     * user properties (one key written with a character code), when it is filtered, then the embedded user
     * properties are sent as the placeholder and the rest of the Story Block is kept.
     */
    @Test
    void replacesUserPropertiesOfContentEmbeddedInStoryBlockText() throws Exception {
        final String storyBlock = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\",\"attrs\":{\"data\":"
                + "{\"identifier\":\"emb-1\",\"contentType\":\"Product\",\"title\":\"Helmet\","
                + "\"modUser\":\"dotcms.org.1\",\"own\\u0065r\":\"dotcms.org.1\",\"modUserName\":\"Admin User\"}}}]}";
        final Map<String, Object> contentlet = content("abc-1");
        contentlet.put("body", storyBlock);
        contentlet.put("body_raw", storyBlock);

        final Map<?, ?> filtered = (Map<?, ?>) PropertyTreeFilter.filter(contentlet, anonymousFilter(), FIELD_TYPES);

        for (final String key : List.of("body", "body_raw")) {
            final String text = (String) filtered.get(key);
            assertFalse(text.contains("dotcms.org.1") || text.contains("Admin User"), key + ": " + text);
            final JsonNode data = JsonUtil.JSON_MAPPER.readTree(text).at("/content/0/attrs/data");
            assertEquals("Helmet", data.get("title").asText());
            assertEquals(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER, data.get("modUser").asText());
        }
    }

    /**
     * Given a Story Block field whose value is not JSON, such as HTML kept from a converted field, when it is
     * filtered, then the text is kept as it is.
     */
    @Test
    void keepsStoryBlockTextThatIsNotJson() {
        final String html = "<p>Written by the \"owner\" of the shop</p>";
        final Map<String, Object> contentlet = content("abc-1");
        contentlet.put("body", html);

        final Map<?, ?> filtered = (Map<?, ?>) PropertyTreeFilter.filter(contentlet, anonymousFilter(), FIELD_TYPES);

        assertSame(html, filtered.get("body"));
    }

    /**
     * Given one Story Block property of a contentlet, asked for on its own as GraphQL {@code _map(key:)} does,
     * when it is filtered for its content type, then the contentlets embedded in it get the placeholder, in the
     * field and in its {@code _raw} copy, and no other property is added.
     */
    @Test
    void replacesUserPropertiesInOneStoryBlockPropertyOfAContentlet() {
        final String storyBlock = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\",\"attrs\":{\"data\":"
                + "{\"identifier\":\"emb-1\",\"contentType\":\"Product\",\"title\":\"Helmet\","
                + "\"modUser\":\"dotcms.org.1\",\"modUserName\":\"Admin User\"}}}]}";

        for (final String key : List.of("body", "body_raw")) {
            final Map<String, Object> filtered = PropertyTreeFilter.filterContentletProperties(
                    "Product", Map.of(key, storyBlock), anonymousFilter(), FIELD_TYPES);

            assertEquals(List.of(key), List.copyOf(filtered.keySet()));
            final String text = (String) filtered.get(key);
            assertFalse(text.contains("dotcms.org.1") || text.contains("Admin User"), key + ": " + text);
            assertTrue(text.contains(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER), key + ": " + text);
        }
    }

    /**
     * Given single properties of a contentlet, when each is filtered for its content type, then a user property
     * gets the placeholder, a Key/Value value is kept as stored, and a property listed to hide is left out.
     */
    @Test
    void filtersSinglePropertiesOfAContentletLikeTheWholeContentlet() {
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, "title");
        final ContentFieldFilter filter = anonymousFilter();

        assertEquals(Map.of("modUser", ContentFieldFilter.USER_PROPERTY_PLACEHOLDER),
                PropertyTreeFilter.filterContentletProperties("Product", Map.of("modUser", "dotcms.org.1"), filter, FIELD_TYPES));
        assertEquals(Map.of("meta", Map.of("owner", "customer-value")), PropertyTreeFilter.filterContentletProperties(
                "Product", Map.of("meta", Map.of("owner", "customer-value")), filter, FIELD_TYPES));
        assertEquals(Map.of(), PropertyTreeFilter.filterContentletProperties("Product", Map.of("title", "Hello"), filter, FIELD_TYPES));
    }

    /**
     * Given a contentlet map that names its content type only by {@code stInode}, as the content relationships
     * endpoint returns it, with a Story Block embedding a contentlet and a property listed to hide, when it is
     * filtered, then it is filtered as a contentlet: the embedded user properties get the placeholder in the
     * field and in its {@code _raw} copy, and the hidden property is left out.
     */
    @Test
    void filtersContentletsThatNameTheirTypeByStructureInode() {
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, "title");
        final String storyBlock = "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\",\"attrs\":{\"data\":"
                + "{\"identifier\":\"emb-1\",\"contentType\":\"Product\",\"modUser\":\"dotcms.org.1\","
                + "\"modUserName\":\"Admin User\"}}}]}";
        final Map<String, Object> contentlet = content("abc-1");
        contentlet.remove("contentType");
        contentlet.put("stInode", "product-type-id");
        contentlet.put("body", storyBlock);
        contentlet.put("body_raw", storyBlock);

        final Map<?, ?> filtered = (Map<?, ?>) PropertyTreeFilter.filter(contentlet, anonymousFilter(), FIELD_TYPES);

        for (final String key : List.of("body", "body_raw")) {
            final String text = (String) filtered.get(key);
            assertFalse(text.contains("dotcms.org.1") || text.contains("Admin User"), key + ": " + text);
            assertTrue(text.contains(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER), key + ": " + text);
        }
        assertFalse(filtered.containsKey("title"));
        assertEquals("product-type-id", filtered.get("stInode"));
    }

    /**
     * Given a Story Block whose embedded contentlet is stored as JSON text in the node's {@code data}, when it is
     * filtered, then the embedded user properties get the placeholder and {@code data} is still JSON text.
     */
    @Test
    void replacesUserPropertiesOfContentEmbeddedAsJsonText() throws Exception {
        final String data = "{\"identifier\":\"emb-1\",\"contentType\":\"Product\",\"title\":\"Helmet\","
                + "\"modUser\":\"dotcms.org.1\",\"modUserName\":\"Admin User\"}";
        final String storyBlock = JsonUtil.JSON_MAPPER.writeValueAsString(Map.of("type", "doc", "content",
                List.of(Map.of("type", "dotContent", "attrs", Map.of("data", data)))));
        final Map<String, Object> contentlet = content("abc-1");
        contentlet.put("body", storyBlock);

        final Map<?, ?> filtered = (Map<?, ?>) PropertyTreeFilter.filter(contentlet, anonymousFilter(), FIELD_TYPES);

        final String text = (String) filtered.get("body");
        assertFalse(text.contains("dotcms.org.1") || text.contains("Admin User"), text);
        final JsonNode filteredData = JsonUtil.JSON_MAPPER.readTree(text).at("/content/0/attrs/data");
        assertTrue(filteredData.isTextual(), text);
        final JsonNode embedded = JsonUtil.JSON_MAPPER.readTree(filteredData.asText());
        assertEquals("Helmet", embedded.get("title").asText());
        assertEquals(ContentFieldFilter.USER_PROPERTY_PLACEHOLDER, embedded.get("modUser").asText());
    }

    /** Returns a contentlet map of the test content type with the usual user properties. */
    private static Map<String, Object> content(final String identifier) {
        final Map<String, Object> content = new LinkedHashMap<>();
        content.put("identifier", identifier);
        content.put("inode", identifier + "-inode");
        content.put("contentType", "Product");
        content.put("title", "Hello");
        content.put("modUser", "dotcms.org.1");
        content.put("ownerUserName", "Admin User");
        return content;
    }

    /** Returns the filter that applies to an anonymous caller. */
    private static ContentFieldFilter anonymousFilter() {
        final User anonymous = mock(User.class);
        when(anonymous.isAnonymousUser()).thenReturn(true);
        return ContentFieldFilter.forUser(anonymous);
    }
}

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
     * with a key named {@code owner}, when it is filtered, then the user properties are removed at every depth
     * and the Key/Value value is kept as stored.
     */
    @Test
    void removesUserPropertiesAtEveryDepthAndKeepsCustomerFieldValues() {
        final Map<String, Object> related = content("rel-1");
        related.put("lockedBy", Map.of("userId", "dotcms.org.1"));
        final Map<String, Object> contentlet = content("abc-1");
        contentlet.put("meta", Map.of("owner", "customer-value"));
        contentlet.put("rel", List.of(related));

        final Map<?, ?> filtered = (Map<?, ?>) PropertyTreeFilter.filter(contentlet, anonymousFilter(), FIELD_TYPES);

        assertFalse(filtered.containsKey("modUser"));
        assertFalse(filtered.containsKey("ownerUserName"));
        assertEquals(Map.of("owner", "customer-value"), filtered.get("meta"));
        final Map<?, ?> filteredRelated = (Map<?, ?>) ((List<?>) filtered.get("rel")).get(0);
        assertFalse(filteredRelated.containsKey("lockedBy"));
        assertEquals("abc-1", filtered.get("identifier"));
        assertTrue(contentlet.containsKey("modUser"), "the input must not be changed");
    }

    /**
     * Given a Story Block value held as JSON text, and its {@code _raw} copy, that embed a contentlet with
     * user properties (one key written with a character code), when it is filtered, then the embedded user
     * properties are removed and the rest of the Story Block is kept.
     */
    @Test
    void removesUserPropertiesOfContentEmbeddedInStoryBlockText() throws Exception {
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
            assertEquals("Helmet", JsonUtil.JSON_MAPPER.readTree(text).at("/content/0/attrs/data/title").asText());
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

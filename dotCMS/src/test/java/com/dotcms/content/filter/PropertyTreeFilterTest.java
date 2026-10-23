package com.dotcms.content.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for filtering a JSON-like response tree: which objects lose the user properties, which
 * objects also get the configured content property lists, and which values are never touched.
 *
 * @author hassandotcms
 */
class PropertyTreeFilterTest {

    private static final List<String> SETTINGS = List.of(
            ContentFieldFilter.HIDE_USER_PROPERTIES_PROPERTY,
            ContentFieldFilter.FIELDS_TO_MASK_PROPERTY,
            ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY);

    /** Field types of the content type used by these tests, keyed by field variable. */
    private static final Map<String, String> BLOG_FIELDS = Map.of(
            "title", TextField.class.getName(),
            "related", RelationshipField.class.getName(),
            "body", StoryBlockField.class.getName(),
            "settings", JSONField.class.getName(),
            "metadata", KeyValueField.class.getName(),
            "widget", CustomField.class.getName());

    private static final PropertyTreeFilter.FieldTypes FIELD_TYPES =
            (contentType, fieldVariable) -> "Blog".equals(contentType) ? BLOG_FIELDS.get(fieldVariable) : null;

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
     * Given a content map and the user properties switch on, when the tree is filtered, then the user
     * properties are removed and every other key is kept.
     */
    @Test
    @SuppressWarnings("unchecked")
    void removesUserPropertiesFromContent() {
        final Map<String, Object> filtered = (Map<String, Object>) filter(content("abc-1"));

        for (final String userProperty : List.of("owner", "ownerUserName", "modUser", "modUserName",
                "publishUser", "publishUserName", "lockedBy", "lockedByName", "wfCreatedBy", "wfAssign")) {
            assertFalse(filtered.containsKey(userProperty), userProperty + " must be removed");
        }
        assertEquals("abc-1", filtered.get("identifier"));
        assertEquals("inode-abc-1", filtered.get("inode"));
        assertEquals("Blog", filtered.get("contentType"));
        assertEquals("Title abc-1", filtered.get("title"));
        assertEquals(1700000000000L, filtered.get("modDate"));
        assertEquals(true, filtered.get("locked"));
    }

    /**
     * Given a relationship field holding a list of related contents, when the tree is filtered, then the
     * related contents are filtered at every depth.
     */
    @Test
    @SuppressWarnings("unchecked")
    void filtersRelatedContentAtEveryDepth() {
        final Map<String, Object> grandChild = content("abc-3");
        final Map<String, Object> child = content("abc-2");
        child.put("related", List.of(grandChild));
        final Map<String, Object> parent = content("abc-1");
        parent.put("related", List.of(child));

        final Map<String, Object> filtered = (Map<String, Object>) filter(parent);
        final Map<String, Object> filteredChild = ((List<Map<String, Object>>) filtered.get("related")).get(0);
        final Map<String, Object> filteredGrandChild =
                ((List<Map<String, Object>>) filteredChild.get("related")).get(0);

        assertFalse(filtered.containsKey("modUser"));
        assertFalse(filteredChild.containsKey("modUser"));
        assertFalse(filteredChild.containsKey("owner"));
        assertFalse(filteredGrandChild.containsKey("modUser"));
        assertFalse(filteredGrandChild.containsKey("lockedBy"));
        assertEquals("Title abc-3", filteredGrandChild.get("title"));
    }

    /**
     * Given a Block Editor value with a content embedded in attrs.data, when the tree is filtered, then the
     * embedded content is filtered.
     */
    @Test
    @SuppressWarnings("unchecked")
    void filtersContentEmbeddedInBlockEditorValues() {
        final Map<String, Object> embedded = content("abc-2");
        final Map<String, Object> node = Map.of("type", "dotContent", "attrs", Map.of("data", embedded));
        final Map<String, Object> content = content("abc-1");
        content.put("body", Map.of("type", "doc", "content", List.of(node)));

        final Map<String, Object> filtered = (Map<String, Object>) filter(content);
        final Map<String, Object> body = (Map<String, Object>) filtered.get("body");
        final Map<String, Object> filteredNode = ((List<Map<String, Object>>) body.get("content")).get(0);
        final Map<String, Object> data =
                (Map<String, Object>) ((Map<String, Object>) filteredNode.get("attrs")).get("data");

        assertFalse(data.containsKey("modUser"));
        assertFalse(data.containsKey("owner"));
        assertEquals("abc-2", data.get("identifier"));
        assertEquals("dotContent", filteredNode.get("type"));
    }

    /**
     * Given a JSON field whose value contains an owner key, when the tree is filtered, then the value is
     * unchanged.
     */
    @Test
    @SuppressWarnings("unchecked")
    void leavesJsonFieldValuesUntouched() {
        final Map<String, Object> settings = Map.of("owner", "x", "nested", Map.of("modUser", "y"));
        final Map<String, Object> content = content("abc-1");
        content.put("settings", settings);

        final Map<String, Object> filtered = (Map<String, Object>) filter(content);

        assertFalse(filtered.containsKey("owner"), "the content itself must still be filtered");
        assertEquals(settings, filtered.get("settings"));
    }

    /**
     * Given Key/Value and Custom field values that contain an owner key, when the tree is filtered, then
     * the values are unchanged.
     */
    @Test
    @SuppressWarnings("unchecked")
    void leavesKeyValueAndCustomFieldValuesUntouched() {
        final Map<String, Object> metadata = Map.of("owner", "x", "modUser", "y");
        final Map<String, Object> widget = Map.of("owner", "z");
        final Map<String, Object> content = content("abc-1");
        content.put("metadata", metadata);
        content.put("widget", widget);

        final Map<String, Object> filtered = (Map<String, Object>) filter(content);

        assertFalse(filtered.containsKey("modUser"), "the content itself must still be filtered");
        assertEquals(metadata, filtered.get("metadata"));
        assertEquals(widget, filtered.get("widget"));
    }

    /**
     * Given a site object and title listed to hide, when the tree is filtered, then the site loses its user
     * properties but keeps its title, because the lists apply to content only.
     */
    @Test
    @SuppressWarnings("unchecked")
    void appliesConfiguredListsOnlyToContent() {
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, "title");
        final Map<String, Object> site = new LinkedHashMap<>();
        site.put("identifier", "site-1");
        site.put("hostname", "demo.example.com");
        site.put("title", "Demo");
        site.put("owner", "dotcms.org.1");
        site.put("modUser", "dotcms.org.1");

        final Map<String, Object> filtered = (Map<String, Object>) filter(site);

        assertFalse(filtered.containsKey("owner"));
        assertFalse(filtered.containsKey("modUser"));
        assertEquals("Demo", filtered.get("title"));
        assertEquals("demo.example.com", filtered.get("hostname"));
    }

    /**
     * Given a content type with an owner and fields in its layout that carry an owner, when the tree is
     * filtered, then every owner is removed.
     */
    @Test
    @SuppressWarnings("unchecked")
    void removesOwnerFromContentTypeAndItsFields() {
        final Map<String, Object> field = new LinkedHashMap<>(Map.of("variable", "title", "owner", "dotcms.org.1"));
        final Map<String, Object> column = Map.of(
                "columnDivider", Map.of("variable", "col1", "owner", "dotcms.org.1"),
                "fields", List.of(field));
        final Map<String, Object> row = Map.of(
                "divider", Map.of("variable", "row1", "owner", "dotcms.org.1"),
                "columns", List.of(column));
        final Map<String, Object> contentType = new LinkedHashMap<>();
        contentType.put("id", "type-1");
        contentType.put("variable", "Blog");
        contentType.put("owner", "dotcms.org.1");
        contentType.put("fields", List.of(field));
        contentType.put("layout", List.of(row));

        final Map<String, Object> filtered = (Map<String, Object>) filter(Map.of("entity", contentType));
        final Map<String, Object> type = (Map<String, Object>) filtered.get("entity");
        final Map<String, Object> filteredRow = ((List<Map<String, Object>>) type.get("layout")).get(0);
        final Map<String, Object> filteredColumn = ((List<Map<String, Object>>) filteredRow.get("columns")).get(0);

        assertFalse(type.containsKey("owner"));
        assertFalse(((List<Map<String, Object>>) type.get("fields")).get(0).containsKey("owner"));
        assertFalse(((Map<String, Object>) filteredRow.get("divider")).containsKey("owner"));
        assertFalse(((Map<String, Object>) filteredColumn.get("columnDivider")).containsKey("owner"));
        assertFalse(((List<Map<String, Object>>) filteredColumn.get("fields")).get(0).containsKey("owner"));
        assertEquals("Blog", type.get("variable"));
    }

    /**
     * Given push history rows, when the tree is filtered, then pushedBy is removed from each row.
     */
    @Test
    @SuppressWarnings("unchecked")
    void removesPushedByFromPushHistoryRows() {
        final List<Map<String, Object>> rows = List.of(
                Map.of("bundleId", "b-1", "pushedBy", "dotcms.org.1", "environment", "prod"),
                Map.of("bundleId", "b-2", "pushedBy", "dotcms.org.2", "environment", "prod"));

        final List<Map<String, Object>> filtered = (List<Map<String, Object>>) filter(rows);

        assertEquals(2, filtered.size());
        filtered.forEach(row -> {
            assertFalse(row.containsKey("pushedBy"));
            assertEquals("prod", row.get("environment"));
        });
    }

    /**
     * Given an immutable input tree, when it is filtered, then a filtered copy is returned and the input is
     * unchanged.
     */
    @Test
    @SuppressWarnings("unchecked")
    void neverChangesTheInputTree() {
        final Map<String, Object> related = Map.of("identifier", "abc-2", "contentType", "Blog", "modUser", "u");
        final Map<String, Object> input = Map.of("identifier", "abc-1", "contentType", "Blog",
                "owner", "dotcms.org.1", "related", List.of(related));

        final Object[] result = new Object[1];
        assertDoesNotThrow(() -> result[0] = filter(input));
        final Map<String, Object> filtered = (Map<String, Object>) result[0];

        assertFalse(filtered.containsKey("owner"));
        assertFalse(((List<Map<String, Object>>) filtered.get("related")).get(0).containsKey("modUser"));
        assertEquals("dotcms.org.1", input.get("owner"));
        assertEquals("u", related.get("modUser"));
    }

    /**
     * Given a JSONObject with a nested JSONArray, when it is filtered, then the result uses the same JSON
     * types.
     */
    @Test
    void keepsJsonLibraryTypes() {
        final JSONObject related = new JSONObject(content("abc-2"));
        final JSONObject content = new JSONObject(content("abc-1"));
        content.put("related", new JSONArray(List.of(related)));

        final Object filtered = filter(content);

        assertTrue(filtered instanceof JSONObject, "a JSONObject must stay a JSONObject");
        final JSONObject filteredContent = (JSONObject) filtered;
        assertFalse(filteredContent.has("owner"));
        final Object filteredRelated = filteredContent.get("related");
        assertTrue(filteredRelated instanceof JSONArray, "a JSONArray must stay a JSONArray");
        assertTrue(((JSONArray) filteredRelated).get(0) instanceof JSONObject);
        assertFalse(((JSONObject) ((JSONArray) filteredRelated).get(0)).has("modUser"));
        assertTrue(content.has("owner"), "the input must not be changed");
    }

    /**
     * Given a content whose content type cannot be resolved, when the tree is filtered, then its field
     * values are walked and lose their user properties.
     */
    @Test
    @SuppressWarnings("unchecked")
    void descendsWhenContentTypeIsUnknown() {
        final Map<String, Object> content = content("abc-1");
        content.put("contentType", "Unknown");
        content.put("x", Map.of("owner", "y", "label", "kept"));

        final Map<String, Object> filtered = (Map<String, Object>) filter(content);
        final Map<String, Object> value = (Map<String, Object>) filtered.get("x");

        assertFalse(value.containsKey("owner"));
        assertEquals("kept", value.get("label"));
    }

    /**
     * Given a null, text or number root, when it is filtered, then it is returned as is.
     */
    @Test
    void returnsScalarsUnchanged() {
        final ContentFieldFilter filter = anonymousFilter();
        assertNull(PropertyTreeFilter.filter(null, filter, FIELD_TYPES));
        final String text = "owner";
        assertSame(text, PropertyTreeFilter.filter(text, filter, FIELD_TYPES));
        assertEquals(42, PropertyTreeFilter.filter(42, filter, FIELD_TYPES));
    }

    // ---------------------------------------------------------------------------------------------

    /** Filters a tree for an anonymous caller with the field types of these tests. */
    private static Object filter(final Object tree) {
        return PropertyTreeFilter.filter(tree, anonymousFilter(), FIELD_TYPES);
    }

    /** Returns the filter of an anonymous caller under the current configuration. */
    private static ContentFieldFilter anonymousFilter() {
        final User anonymous = mock(User.class);
        when(anonymous.isAnonymousUser()).thenReturn(true);
        return ContentFieldFilter.forUser(anonymous);
    }

    /** Returns a mutable content map of the Blog type holding every user property. */
    private static Map<String, Object> content(final String identifier) {
        final Map<String, Object> content = new LinkedHashMap<>();
        content.put("identifier", identifier);
        content.put("inode", "inode-" + identifier);
        content.put("contentType", "Blog");
        content.put("baseType", "CONTENT");
        content.put("title", "Title " + identifier);
        content.put("modDate", 1700000000000L);
        content.put("locked", true);
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

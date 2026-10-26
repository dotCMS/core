package com.dotcms.rest.api.v1.content;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContentRelationshipsHelper}: which related content is serialized for the
 * requesting user, and which contents a lookup returns.
 */
class ContentRelationshipsHelperTest {

    private static final long DEFAULT_LANGUAGE_ID = 1L;

    private ContentletAPI contentletAPI;
    private PermissionAPI permissionAPI;
    private User user;
    private ContentRelationshipsHelper helper;

    @BeforeEach
    void setUp() {
        contentletAPI = mock(ContentletAPI.class);
        permissionAPI = mock(PermissionAPI.class);
        user = mock(User.class);

        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final Language defaultLanguage = mock(Language.class);
        when(defaultLanguage.getId()).thenReturn(DEFAULT_LANGUAGE_ID);
        when(languageAPI.getDefaultLanguage()).thenReturn(defaultLanguage);

        helper = new ContentRelationshipsHelper(contentletAPI, languageAPI, permissionAPI);
    }

    @Test
    void contentsAsJson_includesOnlyRelatedContentTheUserCanRead() throws Exception {
        // Given a parent with one relationship holding records A and B,
        //   and a permission filter that keeps only B for the user
        final Contentlet parent = content("parent-id");
        final Contentlet recordA = content("record-a-id");
        final Contentlet recordB = content("record-b-id");
        final List<Contentlet> records = List.of(recordA, recordB);
        givenRelationships(parent, relationship("rel-one"), records);
        when(permissionAPI.filterCollection(records, PermissionAPI.PERMISSION_READ, true, user))
                .thenReturn(List.of(recordB));

        // When the contents are serialized for that user
        final String json = helper.contentsAsJson(List.of(parent), user);

        // Then the relationship's array holds B only, and A's identifier appears nowhere
        assertEquals(List.of("record-b-id"), identifiers(json, "rel-one"));
        assertFalse(json.contains("record-a-id"), "A record the user cannot read must not be in the output");
    }

    @Test
    void contentsAsJson_checksReadForTheCallerRespectingFrontEndRoles() throws Exception {
        // Given a parent with one relationship holding some records
        final Contentlet parent = content("parent-id");
        final List<Contentlet> records = List.of(content("record-a-id"), content("record-b-id"));
        givenRelationships(parent, relationship("rel-one"), records);
        when(permissionAPI.filterCollection(records, PermissionAPI.PERMISSION_READ, true, user))
                .thenReturn(records);

        // When the contents are serialized for a user
        helper.contentsAsJson(List.of(parent), user);

        // Then the records are filtered once, for READ, respecting front-end roles, for that user
        verify(permissionAPI, times(1))
                .filterCollection(records, PermissionAPI.PERMISSION_READ, true, user);
    }

    @Test
    void contentsAsJson_keepsAllRelatedContentWhenTheUserCanReadIt() throws Exception {
        // Given a parent with one relationship and a filter that keeps every record
        final Contentlet parent = content("parent-id");
        final List<Contentlet> records = List.of(content("record-a-id"), content("record-b-id"));
        givenRelationships(parent, relationship("rel-one"), records);
        when(permissionAPI.filterCollection(records, PermissionAPI.PERMISSION_READ, true, user))
                .thenReturn(records);

        // When the contents are serialized
        final String json = helper.contentsAsJson(List.of(parent), user);

        // Then the parent's fields and every related record, with its fields, are in the output
        final JSONObject parentJson = new JSONArray(json).getJSONObject(0);
        assertEquals("parent-id", parentJson.getString("identifier"));
        assertEquals(List.of("record-a-id", "record-b-id"), identifiers(json, "rel-one"));
        assertEquals("title of record-a-id",
                parentJson.getJSONArray("rel-one").getJSONObject(0).getString("title"));
    }

    @Test
    void contentsAsJson_keepsTheRelationshipKeyWithAnEmptyArrayWhenNothingIsReadable()
            throws Exception {
        // Given a parent with one relationship and a filter that keeps nothing
        final Contentlet parent = content("parent-id");
        final List<Contentlet> records = List.of(content("record-a-id"), content("record-b-id"));
        givenRelationships(parent, relationship("rel-one"), records);
        when(permissionAPI.filterCollection(records, PermissionAPI.PERMISSION_READ, true, user))
                .thenReturn(List.of());

        // When the contents are serialized
        final String json = helper.contentsAsJson(List.of(parent), user);

        // Then the relationship key is present with an empty array
        final JSONObject parentJson = new JSONArray(json).getJSONObject(0);
        assertTrue(parentJson.has("rel-one"), "The relationship key must stay in the output");
        assertEquals(0, parentJson.getJSONArray("rel-one").length());
    }

    @Test
    void contentsAsJson_filtersEveryRelationshipSeparately() throws Exception {
        // Given a parent with two relationship record sets, each with a different readable record
        final Contentlet parent = content("parent-id");
        final Contentlet firstA = content("first-a-id");
        final Contentlet firstB = content("first-b-id");
        final Contentlet secondA = content("second-a-id");
        final Contentlet secondB = content("second-b-id");
        final List<Contentlet> firstRecords = List.of(firstA, firstB);
        final List<Contentlet> secondRecords = List.of(secondA, secondB);
        final ContentletRelationships relationships = new ContentletRelationships(parent);
        addRecords(relationships, relationship("rel-one"), firstRecords);
        addRecords(relationships, relationship("rel-two"), secondRecords);
        when(contentletAPI.getAllRelationships(parent)).thenReturn(relationships);
        when(permissionAPI.filterCollection(firstRecords, PermissionAPI.PERMISSION_READ, true, user))
                .thenReturn(List.of(firstB));
        when(permissionAPI.filterCollection(secondRecords, PermissionAPI.PERMISSION_READ, true, user))
                .thenReturn(List.of(secondA));

        // When the contents are serialized
        final String json = helper.contentsAsJson(List.of(parent), user);

        // Then each set is filtered on its own and each key holds only its own readable records
        assertEquals(List.of("first-b-id"), identifiers(json, "rel-one"));
        assertEquals(List.of("second-a-id"), identifiers(json, "rel-two"));
    }

    @Test
    void contentsAsJson_leavesOutRelatedContentWhenItsPermissionsCannotBeChecked()
            throws Exception {
        // Given a parent with two relationships, where the permission check fails for the first
        //   and keeps one record of the second
        final Contentlet parent = content("parent-id");
        final List<Contentlet> firstRecords = List.of(content("first-a-id"));
        final Contentlet secondA = content("second-a-id");
        final List<Contentlet> secondRecords = List.of(secondA);
        final ContentletRelationships relationships = new ContentletRelationships(parent);
        addRecords(relationships, relationship("rel-one"), firstRecords);
        addRecords(relationships, relationship("rel-two"), secondRecords);
        when(contentletAPI.getAllRelationships(parent)).thenReturn(relationships);
        when(permissionAPI.filterCollection(firstRecords, PermissionAPI.PERMISSION_READ, true, user))
                .thenThrow(new DotSecurityException("check failed"));
        when(permissionAPI.filterCollection(secondRecords, PermissionAPI.PERMISSION_READ, true, user))
                .thenReturn(List.of(secondA));

        // When the contents are serialized
        final String json = assertDoesNotThrow(() -> helper.contentsAsJson(List.of(parent), user));

        // Then the first relationship is empty and the second keeps its readable record
        assertEquals(List.of(), identifiers(json, "rel-one"));
        assertEquals(List.of("second-a-id"), identifiers(json, "rel-two"));
    }

    @Test
    void getRelatedContent_returnsEmptyListWhenTheIdentifierIsNotFound() throws Exception {
        // Given an "id" parameter whose lookup finds no content
        when(contentletAPI.findContentletByIdentifier("missing-id", true, DEFAULT_LANGUAGE_ID,
                user, true)).thenReturn(null);

        // When the related content is requested
        final List<Contentlet> result = helper.getRelatedContent(mock(HttpServletRequest.class),
                user, Map.of("id", "missing-id"));

        // Then the result is an empty list with no null element
        assertTrue(result.isEmpty(), "Content that is not found must not be returned");
    }

    @Test
    void getRelatedContent_returnsEmptyListWhenTheInodeIsNotFound() throws Exception {
        // Given an "inode" parameter whose lookup finds no content
        when(contentletAPI.find("missing-inode", user, true)).thenReturn(null);

        // When the related content is requested
        final List<Contentlet> result = helper.getRelatedContent(mock(HttpServletRequest.class),
                user, Map.of("inode", "missing-inode"));

        // Then the result is an empty list
        assertTrue(result.isEmpty(), "Content that is not found must not be returned");
    }

    @Test
    void getRelatedContent_returnsEmptyListWhenTheUserCannotReadTheContent() throws Exception {
        // Given an "id" parameter whose lookup is refused for the user
        when(contentletAPI.findContentletByIdentifier("content-id", true, DEFAULT_LANGUAGE_ID,
                user, true)).thenThrow(new DotSecurityException("not allowed"));

        // When the related content is requested
        final List<Contentlet> result = helper.getRelatedContent(mock(HttpServletRequest.class),
                user, Map.of("id", "content-id"));

        // Then the result is an empty list, the same answer as for content that does not exist
        assertTrue(result.isEmpty(), "Content the user cannot read must not be returned");
    }

    @Test
    void getRelatedContent_returnsTheContentFoundByIdentifier() throws Exception {
        // Given an "id" parameter whose lookup finds a content
        final Contentlet found = content("content-id");
        when(contentletAPI.findContentletByIdentifier("content-id", true, DEFAULT_LANGUAGE_ID,
                user, true)).thenReturn(found);

        // When the related content is requested
        final List<Contentlet> result = helper.getRelatedContent(mock(HttpServletRequest.class),
                user, Map.of("id", "content-id"));

        // Then the result holds that content
        assertEquals(List.of(found), result);
    }

    /**
     * Creates a content mock whose field map holds its identifier and a title.
     */
    private static Contentlet content(final String identifier) {
        final Contentlet contentlet = mock(Contentlet.class);
        final Map<String, Object> fields = new HashMap<>();
        fields.put("identifier", identifier);
        fields.put("title", "title of " + identifier);
        when(contentlet.getMap()).thenReturn(fields);
        when(contentlet.getIdentifier()).thenReturn(identifier);
        return contentlet;
    }

    /**
     * Creates a relationship mock whose relation type value is the given key. Its title differs
     * from the key, as it does for real relationships.
     */
    private static Relationship relationship(final String relationTypeValue) {
        final Relationship relationship = mock(Relationship.class);
        when(relationship.getRelationTypeValue()).thenReturn(relationTypeValue);
        when(relationship.getTitle()).thenReturn("Title of " + relationTypeValue);
        return relationship;
    }

    /**
     * Makes the content API return one relationship, with the given records, for the parent.
     */
    private void givenRelationships(final Contentlet parent, final Relationship relationship,
            final List<Contentlet> records) throws Exception {
        final ContentletRelationships relationships = new ContentletRelationships(parent);
        addRecords(relationships, relationship, records);
        when(contentletAPI.getAllRelationships(parent)).thenReturn(relationships);
    }

    /**
     * Adds a record set for the given relationship to the parent's relationships.
     */
    private static void addRecords(final ContentletRelationships relationships,
            final Relationship relationship, final List<Contentlet> records) {
        final ContentletRelationshipRecords recordSet =
                relationships.new ContentletRelationshipRecords(relationship, true);
        recordSet.setRecords(records);
        relationships.getRelationshipsRecords().add(recordSet);
    }

    /**
     * Reads the identifiers listed under the given key of the first serialized parent.
     */
    private static List<String> identifiers(final String json, final String key) {
        final JSONArray related = new JSONArray(json).getJSONObject(0).getJSONArray(key);
        final List<String> identifiers = new ArrayList<>();
        for (int i = 0; i < related.length(); i++) {
            identifiers.add(related.getJSONObject(i).getString("identifier"));
        }
        return identifiers;
    }
}

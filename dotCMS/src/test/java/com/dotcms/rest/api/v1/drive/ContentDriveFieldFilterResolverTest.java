package com.dotcms.rest.api.v1.drive;

import com.dotcms.browser.FieldSearchCriteria;
import com.dotcms.browser.FieldSearchCriteria.FilterKind;
import com.dotcms.browser.FieldSearchCriteria.RoutingBucket;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Fast, dependency-free unit tests for {@link ContentDriveFieldFilterResolver}: the value-shape →
 * {@link FilterKind} inference, the field-type → {@link RoutingBucket} routing, and every 400
 * validation path. Content types and fields are mocked, so this runs under surefire with no DB or
 * index.
 */
public class ContentDriveFieldFilterResolverTest {

    private final ContentDriveFieldFilterResolver resolver = new ContentDriveFieldFilterResolver();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Builds a mock content type whose {@code fieldMap} exposes one searchable+indexed field per
     * supported type, plus a couple of rejectable fields.
     */
    private ContentType contentTypeWithAllFields() {
        final Map<String, Field> fields = new HashMap<>();
        fields.put("textF", searchableField(TextField.class));
        fields.put("selectF", searchableField(SelectField.class));
        fields.put("multiF", searchableField(MultiSelectField.class));
        fields.put("checkboxF", searchableField(CheckboxField.class));
        fields.put("dateF", searchableField(DateTimeField.class));
        fields.put("tagF", searchableField(TagField.class));
        fields.put("categoryF", searchableField(CategoryField.class));
        fields.put("relationshipF", searchableField(RelationshipField.class));
        fields.put("binaryF", searchableField(BinaryField.class));
        fields.put("notSearchableF", flaggedField(TextField.class, false, true));

        final ContentType contentType = mock(ContentType.class);
        when(contentType.variable()).thenReturn("myCt");
        when(contentType.fieldMap()).thenReturn(fields);
        return contentType;
    }

    private Field searchableField(final Class<? extends Field> type) {
        return flaggedField(type, true, true);
    }

    private Field flaggedField(final Class<? extends Field> type, final boolean searchable,
            final boolean indexed) {
        final Field field = mock(type);
        when(field.searchable()).thenReturn(searchable);
        when(field.indexed()).thenReturn(indexed);
        return field;
    }

    private FieldSearchCriteria single(final ContentType ct, final String var, final Object value) {
        final List<FieldSearchCriteria> criteria = resolver.parse(Map.of(var, value), ct);
        assertEquals("expected exactly one criterion", 1, criteria.size());
        return criteria.get(0);
    }

    // ---- value-shape → FilterKind inference + routing ----

    @Test
    public void textScalarRoutesToIndexAsScalar() {
        final ContentType ct = contentTypeWithAllFields();
        final FieldSearchCriteria c = single(ct, "textF", "angular");
        assertEquals(FilterKind.SCALAR, c.getKind());
        assertEquals(RoutingBucket.INDEX, c.getBucket());
        assertEquals(List.of("angular"), c.getValues());
    }

    @Test
    public void selectScalarRoutesToIndex() {
        final ContentType ct = contentTypeWithAllFields();
        assertEquals(RoutingBucket.INDEX, single(ct, "selectF", "news").getBucket());
    }

    @Test
    public void multiSelectListRoutesToIndexAsMulti() {
        final ContentType ct = contentTypeWithAllFields();
        final FieldSearchCriteria c = single(ct, "multiF", List.of("a", "b"));
        assertEquals(FilterKind.MULTI, c.getKind());
        assertEquals(RoutingBucket.INDEX, c.getBucket());
        assertEquals(List.of("a", "b"), c.getValues());
    }

    @Test
    public void checkboxBooleanRoutesToIndexAsBoolean() {
        final ContentType ct = contentTypeWithAllFields();
        final FieldSearchCriteria c = single(ct, "checkboxF", Boolean.TRUE);
        assertEquals(FilterKind.BOOLEAN, c.getKind());
        assertEquals(Boolean.TRUE, c.getBooleanValue());
    }

    @Test
    public void checkboxListRoutesToIndexAsMulti() {
        final ContentType ct = contentTypeWithAllFields();
        assertEquals(FilterKind.MULTI, single(ct, "checkboxF", List.of("x", "y")).getKind());
    }

    @Test
    public void dateRangeRoutesToIndexAsRange() {
        final ContentType ct = contentTypeWithAllFields();
        final FieldSearchCriteria c = single(ct, "dateF", Map.of("from", "2024-01-01", "to", "2025-01-01"));
        assertEquals(FilterKind.RANGE, c.getKind());
        assertEquals(RoutingBucket.INDEX, c.getBucket());
        assertEquals("2024-01-01", c.getRangeFrom());
        assertEquals("2025-01-01", c.getRangeTo());
    }

    @Test
    public void openEndedRangeKeepsOnlyOneBound() {
        final ContentType ct = contentTypeWithAllFields();
        final Map<String, Object> onlyFrom = new HashMap<>();
        onlyFrom.put("from", "2024-01-01");
        final FieldSearchCriteria c = single(ct, "dateF", onlyFrom);
        assertEquals("2024-01-01", c.getRangeFrom());
        assertEquals(null, c.getRangeTo());
    }

    @Test
    public void tagListRoutesToDatabase() {
        final ContentType ct = contentTypeWithAllFields();
        final FieldSearchCriteria c = single(ct, "tagF", List.of("angular", "cms"));
        assertEquals(FilterKind.MULTI, c.getKind());
        assertEquals(RoutingBucket.DB, c.getBucket());
    }

    @Test
    public void categoryListRoutesToIndex() {
        final ContentType ct = contentTypeWithAllFields();
        assertEquals(RoutingBucket.INDEX, single(ct, "categoryF", List.of("inode1")).getBucket());
    }

    @Test
    public void numberScalarIsTreatedAsString() {
        final ContentType ct = contentTypeWithAllFields();
        assertEquals(List.of("42"), single(ct, "textF", 42).getValues());
    }

    // ---- 400 validation paths ----

    @Test
    public void unknownKeyIsRejected() {
        final ContentType ct = contentTypeWithAllFields();
        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of("doesNotExist", "x"), ct));
    }

    @Test
    public void nonSearchableFieldIsRejected() {
        final ContentType ct = contentTypeWithAllFields();
        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of("notSearchableF", "x"), ct));
    }

    @Test
    public void relationshipGetsDistinctNotYetMessage() {
        final ContentType ct = contentTypeWithAllFields();
        final BadRequestException ex = assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of("relationshipF", List.of("id1")), ct));
        // The client-facing detail is carried in the JAX-RS response entity, not getMessage().
        final String detail = String.valueOf(ex.getResponse().getEntity()).toLowerCase();
        assertTrue("relationship rejection should mention it is not available yet: " + detail,
                detail.contains("not available yet") || detail.contains("v1.1"));
    }

    @Test
    public void outOfScopeTypeIsRejected() {
        final ContentType ct = contentTypeWithAllFields();
        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of("binaryF", "x"), ct));
    }

    @Test
    public void kindMismatchRangeOnTextIsRejected() {
        final ContentType ct = contentTypeWithAllFields();
        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of("textF", Map.of("from", "1", "to", "2")), ct));
    }

    @Test
    public void blankRangeIsRejected() {
        final ContentType ct = contentTypeWithAllFields();
        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of("dateF", Map.of("from", "", "to", "")), ct));
    }

    @Test
    public void emptyScalarIsRejected() {
        final ContentType ct = contentTypeWithAllFields();
        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of("textF", "   "), ct));
    }

    @Test
    public void nullContentTypeWithFiltersIsRejected() {
        assertThrows(BadRequestException.class,
                () -> resolver.parse(Map.of("textF", "x"), null));
    }

    @Test
    public void emptyMapReturnsNoCriteria() {
        final ContentType ct = contentTypeWithAllFields();
        assertTrue(resolver.parse(Map.of(), ct).isEmpty());
    }

    // ---- Jackson value shapes match inferKind branches ----

    /**
     * Feeds the resolver with the exact runtime types Jackson produces from a JSON body (String,
     * Boolean, List, LinkedHashMap, Integer) to confirm {@code inferKind} matches the real HTTP
     * contract, not just hand-built Java types.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void jacksonValueShapesInferCorrectKinds() throws Exception {
        final String json = "{"
                + "\"textF\": \"angular\","
                + "\"multiF\": [\"a\", \"b\"],"
                + "\"checkboxF\": true,"
                + "\"dateF\": {\"from\": \"2024-01-01\", \"to\": \"2025-01-01\"},"
                + "\"selectF\": 42"
                + "}";
        final Map<String, Object> userSearchable = mapper.readValue(json, Map.class);
        final ContentType ct = contentTypeWithAllFields();

        final Map<String, FilterKind> byField = new HashMap<>();
        for (final FieldSearchCriteria c : resolver.parse(userSearchable, ct)) {
            byField.put(c.getFieldVariable(), c.getKind());
        }
        assertEquals(FilterKind.SCALAR, byField.get("textF"));
        assertEquals(FilterKind.MULTI, byField.get("multiF"));
        assertEquals(FilterKind.BOOLEAN, byField.get("checkboxF"));
        assertEquals(FilterKind.RANGE, byField.get("dateF"));
        assertEquals(FilterKind.SCALAR, byField.get("selectF"));
    }
}

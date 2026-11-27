package com.dotcms.contenttype.transform.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.UnitTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotmarketing.business.DotStateException;
import java.util.List;
import org.junit.Test;

/**
 * Error messages produced by {@link JsonFieldTransformer} for field JSON a caller got wrong. The
 * message is what the content type endpoints return in their 400 response, so it has to name the
 * actual problem.
 */
public class JsonFieldTransformerErrorMessagesTest extends UnitTestBase {

    private static String messageOf(final Runnable action) {
        try {
            action.run();
        } catch (final DotStateException e) {
            return e.getMessage();
        }
        fail("Expected a DotStateException");
        return null;
    }

    /**
     * An unknown {@code clazz} inside a field ARRAY used to be re-parsed as a JSON object, and the
     * caller got "A JSONObject text must begin with '{'" instead of the real problem.
     */
    @Test
    public void test_unknown_field_class_in_an_array_names_the_class() {
        final String message = messageOf(() -> new JsonFieldTransformer(
                "[{\"clazz\":\"NUMERIC\",\"name\":\"Count\",\"variable\":\"count\"}]"));

        assertTrue(message, message.contains("Unknown field class 'NUMERIC'"));
        assertTrue(message, message.contains("TEXT"));
        assertTrue(message, message.contains("INTEGER or FLOAT"));
        assertFalse(message, message.contains("JSONObject text must begin"));
    }

    @Test
    public void test_unknown_field_class_in_an_object_names_the_class() {
        final String message = messageOf(() -> new JsonFieldTransformer(
                "{\"clazz\":\"com.dotcms.contenttype.model.field.ImmutableNumericField\","
                        + "\"name\":\"Count\",\"variable\":\"count\"}"));

        assertTrue(message, message.contains(
                "Unknown field class 'com.dotcms.contenttype.model.field.ImmutableNumericField'"));
    }

    /** A field without {@code clazz} used to get "Unknown field class 'null'". */
    @Test
    public void test_field_without_clazz_says_clazz_is_needed() {
        final String fromObject = messageOf(() -> new JsonFieldTransformer(
                "{\"name\":\"Count\",\"variable\":\"count\"}"));
        assertEquals(JsonFieldTransformer.MISSING_FIELD_CLASS_MESSAGE, fromObject);

        final String fromArray = messageOf(() -> new JsonFieldTransformer(
                "[{\"name\":\"Count\",\"variable\":\"count\"}]"));
        assertEquals(JsonFieldTransformer.MISSING_FIELD_CLASS_MESSAGE, fromArray);
    }

    /**
     * A relationship field without {@code relationType} (for instance sent as
     * {@code relationshipType}) used to fail with a NullPointerException message.
     */
    @Test
    public void test_relationship_field_without_relationType_explains_the_expected_properties() {
        final String message = messageOf(() -> new JsonFieldTransformer(
                "{\"clazz\":\"com.dotcms.contenttype.model.field.ImmutableRelationshipField\","
                        + "\"name\":\"Category\",\"variable\":\"category\","
                        + "\"relationshipType\":\"Category\",\"values\":\"1\","
                        + "\"indexed\":true,\"listed\":false,\"fieldVariables\":[]}").mapObject());

        assertTrue(message, message.contains("Relationship field 'Category' needs 'relationType'"));
        assertFalse(message, message.contains("Cannot invoke"));
    }

    /** Well-formed input is unaffected: arrays and single objects both still parse. */
    @Test
    public void test_valid_array_and_object_still_parse() {
        final List<Field> fromArray = new JsonFieldTransformer(
                "[{\"clazz\":\"com.dotcms.contenttype.model.field.ImmutableTextField\","
                        + "\"name\":\"Title\",\"variable\":\"title\"},"
                        + "{\"clazz\":\"TextField\",\"name\":\"Subtitle\",\"variable\":\"subtitle\"}]")
                .asList();
        assertEquals(2, fromArray.size());
        assertTrue(fromArray.get(1) instanceof ImmutableTextField);

        final Field fromObject = new JsonFieldTransformer(
                "  {\"clazz\":\"TEXT\",\"name\":\"Title\",\"variable\":\"title\"}").from();
        assertEquals("title", fromObject.variable());
    }
}

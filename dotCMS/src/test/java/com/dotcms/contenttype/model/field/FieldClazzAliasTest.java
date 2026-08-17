package com.dotcms.contenttype.model.field;

import static org.junit.Assert.assertEquals;

import com.dotcms.UnitTestBase;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

/**
 * Unit tests for the {@code clazz} discriminator resolution performed by
 * {@link Field.ClassNameAliasResolver}. In addition to the fully-qualified {@code Immutable*} field
 * class name, the resolver accepts ergonomic short forms so callers (e.g. AI agents) can pass a
 * field-type name/legacy value ({@code "TEXT"}, {@code "text"}, {@code "STORY_BLOCK_FIELD"}, ...) or
 * the concrete simple class name ({@code "TextField"}). Every accepted form must deserialize to the
 * same generated {@code Immutable*} field class.
 *
 * <p>These deserialize straight into {@link Field} (the polymorphic type carrying the
 * {@code @JsonTypeIdResolver}), which is the exact chokepoint every field {@code clazz} value flows
 * through and needs no database.</p>
 */
public class FieldClazzAliasTest extends UnitTestBase {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private String resolvedClassName(final String clazz) throws Exception {
        final String json = "{\"clazz\":\"" + clazz + "\",\"name\":\"Test\",\"variable\":\"testVar\"}";
        return MAPPER.readValue(json, Field.class).getClass().getSimpleName();
    }

    private void assertResolves(final String clazz, final String expectedImmutableClass)
            throws Exception {
        assertEquals("clazz '" + clazz + "' should deserialize to " + expectedImmutableClass,
                expectedImmutableClass, resolvedClassName(clazz));
    }

    @Test
    public void test_fieldType_enum_names_resolve() throws Exception {
        assertResolves("TEXT", "ImmutableTextField");
        assertResolves("TEXT_AREA", "ImmutableTextAreaField");
        assertResolves("WYSIWYG", "ImmutableWysiwygField");
        assertResolves("CHECKBOX", "ImmutableCheckboxField");
        assertResolves("RADIO", "ImmutableRadioField");
        assertResolves("SELECT", "ImmutableSelectField");
        assertResolves("MULTI_SELECT", "ImmutableMultiSelectField");
        assertResolves("DATE", "ImmutableDateField");
        assertResolves("TIME", "ImmutableTimeField");
        assertResolves("DATE_TIME", "ImmutableDateTimeField");
        assertResolves("TAG", "ImmutableTagField");
        assertResolves("CONSTANT", "ImmutableConstantField");
        assertResolves("HIDDEN", "ImmutableHiddenField");
        assertResolves("BINARY", "ImmutableBinaryField");
        assertResolves("CUSTOM_FIELD", "ImmutableCustomField");
        assertResolves("KEY_VALUE", "ImmutableKeyValueField");
        assertResolves("STORY_BLOCK_FIELD", "ImmutableStoryBlockField");
        assertResolves("JSON_FIELD", "ImmutableJSONField");
        assertResolves("CATEGORY", "ImmutableCategoryField");
        assertResolves("RELATIONSHIP", "ImmutableRelationshipField");
    }

    @Test
    public void test_fieldType_names_are_case_insensitive() throws Exception {
        assertResolves("text", "ImmutableTextField");
        assertResolves("Text", "ImmutableTextField");
        assertResolves("story_block_field", "ImmutableStoryBlockField");
        assertResolves("Custom_Field", "ImmutableCustomField");
    }

    @Test
    public void test_legacy_values_resolve() throws Exception {
        // The lowercase legacy values (LegacyFieldTypes#legacyValue) also resolve.
        assertResolves("checkbox", "ImmutableCheckboxField");
        assertResolves("multi_select", "ImmutableMultiSelectField");
        assertResolves("wysiwyg", "ImmutableWysiwygField");
    }

    @Test
    public void test_concrete_simple_class_name_still_resolves() throws Exception {
        // Pre-existing leniency (bare simple class name -> Immutable*) must be preserved.
        assertResolves("TextField", "ImmutableTextField");
        assertResolves("StoryBlockField", "ImmutableStoryBlockField");
        assertResolves("CustomField", "ImmutableCustomField");
    }

    @Test
    public void test_fully_qualified_immutable_class_name_still_resolves() throws Exception {
        // The documented canonical form must keep working unchanged.
        assertResolves("com.dotcms.contenttype.model.field.ImmutableTextField",
                "ImmutableTextField");
        assertResolves("com.dotcms.contenttype.model.field.ImmutableStoryBlockField",
                "ImmutableStoryBlockField");
    }
}

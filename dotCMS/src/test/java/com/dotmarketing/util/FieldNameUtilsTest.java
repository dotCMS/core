package com.dotmarketing.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link FieldNameUtils}
 */
public class FieldNameUtilsTest {

    @Test
    public void testConvertFieldClassName_withFullyQualifiedClassName() {
        // Test with fully qualified class names
        assertEquals("text", 
                FieldNameUtils.convertFieldClassName("com.dotcms.contenttype.model.field.TextField"));
        assertEquals("textarea", 
                FieldNameUtils.convertFieldClassName("com.dotcms.contenttype.model.field.TextareaField"));
        assertEquals("checkbox", 
                FieldNameUtils.convertFieldClassName("com.dotcms.contenttype.model.field.CheckboxField"));
        assertEquals("date", 
                FieldNameUtils.convertFieldClassName("com.dotcms.contenttype.model.field.DateField"));
        assertEquals("datetime", 
                FieldNameUtils.convertFieldClassName("com.dotcms.contenttype.model.field.DateTimeField"));
    }

    @Test
    public void testConvertFieldClassName_withSimpleClassName() {
        // Test with simple class names
        assertEquals("text", FieldNameUtils.convertFieldClassName("TextField"));
        assertEquals("textarea", FieldNameUtils.convertFieldClassName("TextareaField"));
        assertEquals("checkbox", FieldNameUtils.convertFieldClassName("CheckboxField"));
        assertEquals("number", FieldNameUtils.convertFieldClassName("NumberField"));
    }

    @Test
    public void testConvertFieldClassName_withoutFieldSuffix() {
        // Test with class names that don't end with "Field"
        assertEquals("text", FieldNameUtils.convertFieldClassName("Text"));
        assertEquals("number", FieldNameUtils.convertFieldClassName("Number"));
        assertEquals("custom", FieldNameUtils.convertFieldClassName("Custom"));
    }

    @Test
    public void testConvertFieldClassName_withNullAndEmpty() {
        // Test edge cases
        assertEquals("", FieldNameUtils.convertFieldClassName((String)null));
        assertEquals("", FieldNameUtils.convertFieldClassName(""));
        assertEquals("", FieldNameUtils.convertFieldClassName("   "));
    }

    @Test
    public void testConvertFieldClassName_withJustField() {
        // Test with just "Field" - should return empty
        assertEquals("", FieldNameUtils.convertFieldClassName("Field"));
    }

    @Test
    public void testConvertFieldClassName_withShortFieldName() {
        // Test with field names shorter than "Field" suffix
        assertEquals("a", FieldNameUtils.convertFieldClassName("a"));
        assertEquals("ab", FieldNameUtils.convertFieldClassName("ab"));
        assertEquals("abcd", FieldNameUtils.convertFieldClassName("abcd"));
    }

    @Test
    public void testConvertFieldClassName_withClassParameter() {
        // Test with Class<?> parameter - using mock classes
        assertEquals("string", FieldNameUtils.convertFieldClassName(String.class));
        assertEquals("integer", FieldNameUtils.convertFieldClassName(Integer.class));
        assertEquals("object", FieldNameUtils.convertFieldClassName(Object.class));
    }

    @Test
    public void testConvertSimpleClassName_basic() {
        // Test the simple class name method
        assertEquals("text", FieldNameUtils.convertFieldClassName("TextField"));
        assertEquals("textarea", FieldNameUtils.convertFieldClassName("TextareaField"));
        assertEquals("custom", FieldNameUtils.convertFieldClassName("Custom"));
    }

    @Test
    public void testConvertSimpleClassName_edgeCases() {
        // Test edge cases for simple class name method
        assertEquals("", FieldNameUtils.convertFieldClassName((String)null));
        assertEquals("", FieldNameUtils.convertFieldClassName(""));
        assertEquals("", FieldNameUtils.convertFieldClassName("   "));
        assertEquals("", FieldNameUtils.convertFieldClassName("Field"));
    }

    @Test
    public void testConvertSimpleClassName_withoutFieldSuffix() {
        // Test simple class names without Field suffix
        assertEquals("text", FieldNameUtils.convertFieldClassName("Text"));
        assertEquals("number", FieldNameUtils.convertFieldClassName("Number"));
        assertEquals("boolean", FieldNameUtils.convertFieldClassName("Boolean"));
    }

    @Test
    public void testCaseHandling() {
        // Test that output is always lowercase
        assertEquals("text", FieldNameUtils.convertFieldClassName("TEXT"));
        assertEquals("textarea", FieldNameUtils.convertFieldClassName("TEXTAREAFIELD"));
        assertEquals("custom", FieldNameUtils.convertFieldClassName("CUSTOM"));
        assertEquals("mixedcase", FieldNameUtils.convertFieldClassName("MixedCaseField"));
    }

    @Test
    public void testWhitespaceHandling() {
        // Test that whitespace is properly trimmed
        assertEquals("text", FieldNameUtils.convertFieldClassName("  TextField  "));
        assertEquals("textarea", FieldNameUtils.convertFieldClassName("\tTextareaField\n"));
        assertEquals("number", FieldNameUtils.convertFieldClassName("  NumberField  "));
    }

    @Test
    public void testSpecialCharacterRemoval() {
        // Test that special characters are removed from field names
        assertEquals("custom", FieldNameUtils.convertFieldClassName("Custom-Field"));
        assertEquals("custom", FieldNameUtils.convertFieldClassName("Custom_Field"));
    }
}
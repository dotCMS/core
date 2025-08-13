package com.dotcms.util;

import com.dotcms.util.JsonUtil.JSONValidationResult;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for JsonUtil.validateJSON method
 */
public class JsonUtilTest {

    @Test
    public void testValidateJSON_ValidObjectJSON() {
        String validObjectJson = "{\"name\":\"John\",\"age\":30}";
        JSONValidationResult result = JsonUtil.validateJSON(validObjectJson);
        
        assertTrue("Valid object JSON should be valid", result.isValid());
        assertNull("Valid JSON should have no error message", result.errorMessage);
        assertEquals("Valid JSON should have no line number", -1, result.line);
        assertEquals("Valid JSON should have no column number", -1, result.column);
        assertNotNull("Valid JSON should have a JsonNode", result.node);
        assertTrue("Should be recognized as an object", result.node.isObject());
    }

    @Test
    public void testValidateJSON_ValidArrayJSON() {
        String validArrayJson = "[{\"name\":\"John\"},{\"name\":\"Jane\"}]";
        JSONValidationResult result = JsonUtil.validateJSON(validArrayJson);
        
        assertTrue("Valid array JSON should be valid", result.isValid());
        assertNull("Valid JSON should have no error message", result.errorMessage);
        assertEquals("Valid JSON should have no line number", -1, result.line);
        assertEquals("Valid JSON should have no column number", -1, result.column);
        assertNotNull("Valid JSON should have a JsonNode", result.node);
        assertTrue("Should be recognized as an array", result.node.isArray());
    }

    @Test
    public void testValidateJSON_ValidEmptyObject() {
        String emptyObjectJson = "{}";
        JSONValidationResult result = JsonUtil.validateJSON(emptyObjectJson);
        
        assertTrue("Empty object should be valid", result.isValid());
        assertNull("Valid JSON should have no error message", result.errorMessage);
        assertNotNull("Valid JSON should have a JsonNode", result.node);
        assertTrue("Should be recognized as an object", result.node.isObject());
    }

    @Test
    public void testValidateJSON_ValidEmptyArray() {
        String emptyArrayJson = "[]";
        JSONValidationResult result = JsonUtil.validateJSON(emptyArrayJson);
        
        assertTrue("Empty array should be valid", result.isValid());
        assertNull("Valid JSON should have no error message", result.errorMessage);
        assertNotNull("Valid JSON should have a JsonNode", result.node);
        assertTrue("Should be recognized as an array", result.node.isArray());
    }

    @Test
    public void testValidateJSON_ValidNestedJSON() {
        String nestedJson = "{\"user\":{\"profile\":{\"name\":\"John\",\"settings\":[\"email\",\"sms\"]}}}";
        JSONValidationResult result = JsonUtil.validateJSON(nestedJson);
        
        assertTrue("Nested JSON should be valid", result.isValid());
        assertNull("Valid JSON should have no error message", result.errorMessage);
        assertNotNull("Valid JSON should have a JsonNode", result.node);
        assertTrue("Should be recognized as an object", result.node.isObject());
    }

    @Test
    public void testValidateJSON_InvalidStringLiteral() {
        String literalString = "test";
        JSONValidationResult result = JsonUtil.validateJSON(literalString);
        
        assertFalse("String literal 'test' should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertEquals("JSON must be an object or array, not a primitive value", result.errorMessage);
        assertEquals("Error should have no line number", -1, result.line);
        assertEquals("Error should have no column number", -1, result.column);
    }

    @Test
    public void testValidateJSON_InvalidQuotedString() {
        String quotedString = "\"test\"";
        JSONValidationResult result = JsonUtil.validateJSON(quotedString);
        
        assertFalse("Quoted string should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertEquals("JSON must be an object or array, not a primitive value", result.errorMessage);
    }

    @Test
    public void testValidateJSON_InvalidNumber() {
        String numberString = "123";
        JSONValidationResult result = JsonUtil.validateJSON(numberString);
        
        assertFalse("Number should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertEquals("JSON must be an object or array, not a primitive value", result.errorMessage);
    }

    @Test
    public void testValidateJSON_InvalidBoolean() {
        String booleanString = "true";
        JSONValidationResult result = JsonUtil.validateJSON(booleanString);
        
        assertFalse("Boolean should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertEquals("JSON must be an object or array, not a primitive value", result.errorMessage);
    }

    @Test
    public void testValidateJSON_InvalidNull() {
        String nullString = "null";
        JSONValidationResult result = JsonUtil.validateJSON(nullString);
        
        assertFalse("Null value should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertEquals("JSON must be an object or array, not a primitive value", result.errorMessage);
    }

    @Test
    public void testValidateJSON_MalformedJSON() {
        String malformedJson = "{\"name\":\"John\",\"age\":}";
        JSONValidationResult result = JsonUtil.validateJSON(malformedJson);
        
        assertFalse("Malformed JSON should not be valid", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertTrue("Error should be about JSON parsing", 
                result.errorMessage.contains("Unexpected") || 
                result.errorMessage.contains("expected") ||
                result.errorMessage.contains("Invalid"));
    }

    @Test
    public void testValidateJSON_UnmatchedBraces() {
        String unmatchedJson = "{\"name\":\"John\"";
        JSONValidationResult result = JsonUtil.validateJSON(unmatchedJson);
        
        assertFalse("JSON with unmatched braces should not be valid", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
    }

    @Test
    public void testValidateJSON_EmptyString() {
        String emptyString = "";
        JSONValidationResult result = JsonUtil.validateJSON(emptyString);
        
        assertFalse("Empty string should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
    }

    @Test
    public void testValidateJSON_WhitespaceOnly() {
        String whitespaceString = "   \n\t   ";
        JSONValidationResult result = JsonUtil.validateJSON(whitespaceString);
        
        assertFalse("Whitespace-only string should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
    }

    @Test
    public void testValidateJSON_NullInput() {
        String nullInput = null;
        JSONValidationResult result = JsonUtil.validateJSON(nullInput);
        
        assertFalse("Null input should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
    }
}
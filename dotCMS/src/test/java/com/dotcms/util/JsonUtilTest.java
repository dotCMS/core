package com.dotcms.util;

import com.dotcms.util.JsonUtil.JSONValidationResult;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for JsonUtil.validateJSON method
 */
public class JsonUtilTest {

    /**
     * Tests validation of a well-formed JSON object.
     * <p>
     * Scenario: Validates a simple JSON object with string and number properties.
     * Expected outcome: The validation should succeed, returning isValid=true with 
     * no error details and a populated JsonNode representing the object.
     */
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

    /**
     * Tests validation of a well-formed JSON array.
     * <p>
     * Scenario: Validates a JSON array containing multiple objects with string properties.
     * Expected outcome: The validation should succeed, returning isValid=true with
     * no error details and a populated JsonNode representing the array.
     */
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

    /**
     * Tests validation of an empty JSON object.
     * <p>
     * Scenario: Validates a minimal JSON object with no properties (empty braces).
     * Expected outcome: The validation should succeed, recognizing the empty object
     * as valid JSON and returning a JsonNode representing an empty object.
     */
    @Test
    public void testValidateJSON_ValidEmptyObject() {
        String emptyObjectJson = "{}";
        JSONValidationResult result = JsonUtil.validateJSON(emptyObjectJson);
        
        assertTrue("Empty object should be valid", result.isValid());
        assertNull("Valid JSON should have no error message", result.errorMessage);
        assertNotNull("Valid JSON should have a JsonNode", result.node);
        assertTrue("Should be recognized as an object", result.node.isObject());
    }

    /**
     * Tests validation of an empty JSON array.
     * <p>
     * Scenario: Validates a minimal JSON array with no elements (empty brackets).
     * Expected outcome: The validation should succeed, recognizing the empty array
     * as valid JSON and returning a JsonNode representing an empty array.
     */
    @Test
    public void testValidateJSON_ValidEmptyArray() {
        String emptyArrayJson = "[]";
        JSONValidationResult result = JsonUtil.validateJSON(emptyArrayJson);
        
        assertTrue("Empty array should be valid", result.isValid());
        assertNull("Valid JSON should have no error message", result.errorMessage);
        assertNotNull("Valid JSON should have a JsonNode", result.node);
        assertTrue("Should be recognized as an array", result.node.isArray());
    }

    /**
     * Tests validation of deeply nested JSON structures.
     * <p>
     * Scenario: Validates a complex JSON object with multiple levels of nesting,
     * including nested objects and arrays with mixed data types.
     * Expected outcome: The validation should succeed despite the complexity,
     * parsing the entire nested structure and returning a valid JsonNode.
     */
    @Test
    public void testValidateJSON_ValidNestedJSON() {
        String nestedJson = "{\"user\":{\"profile\":{\"name\":\"John\",\"settings\":[\"email\",\"sms\"]}}}";
        JSONValidationResult result = JsonUtil.validateJSON(nestedJson);
        
        assertTrue("Nested JSON should be valid", result.isValid());
        assertNull("Valid JSON should have no error message", result.errorMessage);
        assertNotNull("Valid JSON should have a JsonNode", result.node);
        assertTrue("Should be recognized as an object", result.node.isObject());
    }

    /**
     * Tests rejection of unquoted string literals.
     * <p>
     * Scenario: Attempts to validate a plain unquoted string that is not valid JSON.
     * The JsonUtil should only accept objects or arrays as valid JSON, not primitive values.
     * Expected outcome: The validation should fail, returning isValid=false with
     * an appropriate error message explaining why the string literal is not valid JSON.
     */
    @Test
    public void testValidateJSON_InvalidStringLiteral() {
        String literalString = "test";
        JSONValidationResult result = JsonUtil.validateJSON(literalString);
        
        assertFalse("String literal 'test' should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertNotNull( "Invalid Json must have error message",result.errorMessage);
        assertEquals("Error should have no line number", 1, result.line);
        assertEquals("Error should have no column number", 5, result.column);
    }

    /**
     * Tests rejection of quoted string primitives.
     * <p>
     * Scenario: Attempts to validate a properly quoted string primitive that is
     * valid JSON syntax but not accepted by this validator which only allows objects/arrays.
     * Expected outcome: The validation should fail with isValid=false and a specific
     * error message indicating that primitive values are not accepted.
     */
    @Test
    public void testValidateJSON_InvalidQuotedString() {
        String quotedString = "\"test\"";
        JSONValidationResult result = JsonUtil.validateJSON(quotedString);
        
        assertFalse("Quoted string should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertEquals("JSON must be an object or array, not a primitive value", result.errorMessage);
    }

    /**
     * Tests rejection of numeric primitives.
     * <p>
     * Scenario: Attempts to validate a numeric primitive that is valid JSON syntax
     * but not accepted by this validator which only allows objects/arrays.
     * Expected outcome: The validation should fail with isValid=false and a specific
     * error message indicating that primitive values (including numbers) are not accepted.
     */
    @Test
    public void testValidateJSON_InvalidNumber() {
        String numberString = "123";
        JSONValidationResult result = JsonUtil.validateJSON(numberString);
        
        assertFalse("Number should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertEquals("JSON must be an object or array, not a primitive value", result.errorMessage);
    }

    /**
     * Tests rejection of boolean primitives.
     * <p>
     * Scenario: Attempts to validate a boolean primitive that is valid JSON syntax
     * but not accepted by this validator which only allows objects/arrays.
     * Expected outcome: The validation should fail with isValid=false and a specific
     * error message indicating that primitive values (including booleans) are not accepted.
     */
    @Test
    public void testValidateJSON_InvalidBoolean() {
        String booleanString = "true";
        JSONValidationResult result = JsonUtil.validateJSON(booleanString);
        
        assertFalse("Boolean should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertEquals("JSON must be an object or array, not a primitive value", result.errorMessage);
    }

    /**
     * Tests rejection of null primitives.
     * <p>
     * Scenario: Attempts to validate a null primitive that is valid JSON syntax
     * but not accepted by this validator which only allows objects/arrays.
     * Expected outcome: The validation should fail with isValid=false and a specific
     * error message indicating that primitive values (including null) are not accepted.
     */
    @Test
    public void testValidateJSON_InvalidNull() {
        String nullString = "null";
        JSONValidationResult result = JsonUtil.validateJSON(nullString);
        
        assertFalse("Null value should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
        assertEquals("JSON must be an object or array, not a primitive value", result.errorMessage);
    }

    /**
     * Tests detection of malformed JSON with incomplete property values.
     * <p>
     * Scenario: Attempts to validate JSON with a missing property value (age property
     * has no value after the colon). This represents a common syntax error in JSON.
     * Expected outcome: The validation should fail with isValid=false and an error
     * message containing parsing-related terms like 'Unexpected', 'expected', or 'Invalid'.
     */
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

    /**
     * Tests detection of unmatched opening braces.
     * <p>
     * Scenario: Attempts to validate JSON that starts an object but never closes it,
     * missing the closing brace. This is a common structural error in JSON.
     * Expected outcome: The validation should fail with isValid=false and an error
     * message indicating the structural problem with the JSON.
     */
    @Test
    public void testValidateJSON_UnmatchedBraces() {
        String unmatchedJson = "{\"name\":\"John\"";
        JSONValidationResult result = JsonUtil.validateJSON(unmatchedJson);
        
        assertFalse("JSON with unmatched braces should not be valid", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
    }

    /**
     * Tests rejection of empty string input.
     * <p>
     * Scenario: Attempts to validate an empty string, which contains no JSON content.
     * This tests the validator's handling of completely empty input.
     * Expected outcome: The validation should fail with isValid=false and an error
     * message indicating that empty input is not valid JSON.
     */
    @Test
    public void testValidateJSON_EmptyString() {
        String emptyString = "";
        JSONValidationResult result = JsonUtil.validateJSON(emptyString);
        
        assertFalse("Empty string should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
    }

    /**
     * Tests rejection of whitespace-only input.
     * <p>
     * Scenario: Attempts to validate a string containing only whitespace characters
     * (spaces, newlines, tabs) with no actual JSON content. This tests edge cases
     * where input appears non-empty but contains no meaningful JSON.
     * Expected outcome: The validation should fail with isValid=false and an error
     * message indicating that whitespace-only input is not valid JSON.
     */
    @Test
    public void testValidateJSON_WhitespaceOnly() {
        String whitespaceString = "   \n\t   ";
        JSONValidationResult result = JsonUtil.validateJSON(whitespaceString);
        
        assertFalse("Whitespace-only string should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
    }

    /**
     * Tests handling of null input parameter.
     * <p>
     * Scenario: Attempts to validate a null string reference, which represents
     * the case where no input is provided at all (different from empty string).
     * This tests the validator's null-safety and error handling.
     * Expected outcome: The validation should fail gracefully with isValid=false
     * and an appropriate error message indicating that null input cannot be validated.
     */
    @Test
    public void testValidateJSON_NullInput() {
        String nullInput = null;
        JSONValidationResult result = JsonUtil.validateJSON(nullInput);
        
        assertFalse("Null input should not be valid JSON", result.isValid());
        assertNotNull("Invalid JSON should have error message", result.errorMessage);
    }
}
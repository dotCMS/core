package com.dotcms.jitsu.validators;

import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test class for the analytics validators.
 * 
 * <p>This class tests the functionality of the different validator implementations:
 * {@link RequiredFieldValidator}, {@link StringTypeValidator}, 
 * {@link JsonObjectTypeValidator}, and {@link JsonArrayTypeValidator}.</p>
 * 
 * <p>Each test verifies both the {@link AnalyticsValidator#test(JSONObject)} method
 * to ensure validators are applied correctly based on configuration, and the
 * {@link AnalyticsValidator#validate(Object)} method to ensure proper validation
 * of field values.</p>
 */
public class AnalyticsValidatorsTest {


    /**
     * Method to test: {@link RequiredFieldValidator#test(JSONObject)} and {@link RequiredFieldValidator#validate(Object)}
     * when: testing the RequiredFieldValidator with different configurations and field values
     * should: correctly identify when to apply the validator based on the "required" attribute
     *         and validate that field values are not null
     */
    @Test
    public void testRequiredFieldValidator() throws Exception {
        RequiredFieldValidator validator = new RequiredFieldValidator();

        // Test test() method
        JSONObject config = new JSONObject();
        assertFalse(validator.test(config));

        config.put("required", false);
        assertFalse(validator.test(config));

        config.put("required", true);
        assertTrue(validator.test(config));

        // Test validate() method
        validator.validate("not null"); // Should not throw exception
        validator.validate(123); // Should not throw exception

        try {
            validator.validate(null);
            fail("Should have thrown AnalyticsValidationException");
        } catch (AnalyticsValidator.AnalyticsValidationException e) {
            assertEquals(ValidationErrorCode.REQUIRED_FIELD_MISSING, e.getCode());
        }
    }

    /**
     * Method to test: {@link StringTypeValidator#test(JSONObject)} and {@link StringTypeValidator#validate(Object)}
     * when: testing the StringTypeValidator with different configurations and field values
     * should: correctly identify when to apply the validator based on the "type" attribute
     *         and validate that field values are strings
     */
    @Test
    public void testStringTypeValidator() throws Exception {
        StringTypeValidator validator = new StringTypeValidator();

        // Test test() method
        JSONObject config = new JSONObject();
        assertFalse(validator.test(config));

        config.put("type", "json object");
        assertFalse(validator.test(config));

        config.put("type", "string");
        assertTrue(validator.test(config));

        // Test validate() method
        validator.validate(null); // Should not throw exception for null
        validator.validate("string value"); // Should not throw exception for string

        try {
            validator.validate(123);
            fail("Should have thrown AnalyticsValidationException");
        } catch (AnalyticsValidator.AnalyticsValidationException e) {
            assertEquals(ValidationErrorCode.INVALID_STRING_TYPE, e.getCode());
        }
    }

    /**
     * Method to test: {@link JsonObjectTypeValidator#test(JSONObject)} and {@link JsonObjectTypeValidator#validate(Object)}
     * when: testing the JsonObjectTypeValidator with different configurations and field values
     * should: correctly identify when to apply the validator based on the "type" attribute
     *         and validate that field values are JSON objects (Map or JSONObject)
     */
    @Test
    public void testJsonObjectTypeValidator() throws Exception {
        JsonObjectTypeValidator validator = new JsonObjectTypeValidator();

        // Test test() method
        JSONObject config = new JSONObject();
        assertFalse(validator.test(config));

        config.put("type", "string");
        assertFalse(validator.test(config));

        config.put("type", "json_object");
        assertTrue(validator.test(config));

        // Test validate() method
        validator.validate(null); // Should not throw exception for null
        validator.validate(new HashMap<>()); // Should not throw exception for Map
        validator.validate(new JSONObject()); // Should not throw exception for JSONObject

        try {
            validator.validate("not a json object");
            fail("Should have thrown AnalyticsValidationException");
        } catch (AnalyticsValidator.AnalyticsValidationException e) {
            assertEquals(ValidationErrorCode.INVALID_JSON_OBJECT_TYPE, e.getCode());
        }
    }

    /**
     * Method to test: {@link JsonArrayTypeValidator#test(JSONObject)} and {@link JsonArrayTypeValidator#validate(Object)}
     * when: testing the JsonArrayTypeValidator with different configurations and field values
     * should: correctly identify when to apply the validator based on the "type" attribute
     *         and validate that field values are JSON arrays (Collection or JSONArray)
     */
    @Test
    public void testJsonArrayTypeValidator() throws Exception {
        JsonArrayTypeValidator validator = new JsonArrayTypeValidator();

        // Test test() method
        JSONObject config = new JSONObject();
        assertFalse(validator.test(config));

        config.put("type", "string");
        assertFalse(validator.test(config));

        config.put("type", "json_array");
        assertTrue(validator.test(config));

        // Test validate() method
        validator.validate(null); // Should not throw exception for null
        validator.validate(new ArrayList<>()); // Should not throw exception for Collection
        validator.validate(new JSONArray()); // Should not throw exception for JSONArray

        try {
            validator.validate("not a json array");
            fail("Should have thrown AnalyticsValidationException");
        } catch (AnalyticsValidator.AnalyticsValidationException e) {
            assertEquals(ValidationErrorCode.INVALID_JSON_ARRAY_TYPE, e.getCode());
        }
    }

}

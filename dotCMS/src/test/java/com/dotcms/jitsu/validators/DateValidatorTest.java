package com.dotcms.jitsu.validators;

import com.dotmarketing.util.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for DateValidator
 */
public class DateValidatorTest {

    /**
     * Tests that the DateValidator correctly identifies configuration with type="date"
     */
    @Test
    public void testShouldApplyValidator() {
        DateValidator validator = new DateValidator();
        
        JSONObject config = new JSONObject();
        config.put("type", "date");
        
        assertTrue(validator.test(config));
    }
    
    /**
     * Tests that the DateValidator does not apply to configuration with other types
     */
    @Test
    public void testShouldNotApplyValidator() {
        DateValidator validator = new DateValidator();
        
        JSONObject config = new JSONObject();
        config.put("type", "string");
        
        assertFalse(validator.test(config));
    }
    
    /**
     * Tests that the DateValidator accepts valid date strings in the format '2025-06-09T14:30:00+02:00'
     */
    @Test
    public void testValidDate() throws AnalyticsValidator.AnalyticsValidationException {
        DateValidator validator = new DateValidator();
        
        // Valid ISO 8601 date with offset
        String validDate = "2025-06-09T14:30:00+02:00";
        
        // Should not throw an exception
        validator.validate(validDate);
    }
    
    /**
     * Tests that the DateValidator rejects invalid date strings
     */
    @Test
    public void testInvalidDateFormat() {
        DateValidator validator = new DateValidator();
        
        // Invalid date formats
        String[] invalidDates = {
            "2025-06-09", // Missing time
            "14:30:00", // Missing date
            "2025-06-09 14:30:00", // Missing T separator
            "2025-06-09T14:30:00", // Missing timezone offset
            "2025/06/09T14:30:00+02:00", // Wrong date separator
            "2025-13-09T14:30:00+02:00", // Invalid month
            "2025-06-32T14:30:00+02:00", // Invalid day
            "2025-06-09T25:30:00+02:00", // Invalid hour
            "2025-06-09T14:61:00+02:00", // Invalid minute
            "2025-06-09T14:30:61+02:00", // Invalid second
            "not a date" // Not a date at all
        };
        
        for (String invalidDate : invalidDates) {
            try {
                validator.validate(invalidDate);
                fail("Expected validation to fail for: " + invalidDate);
            } catch (AnalyticsValidator.AnalyticsValidationException e) {
                assertEquals(ValidationErrorCode.INVALID_DATE_FORMAT, e.getCode());
            }
        }
    }
    
    /**
     * Tests that the DateValidator rejects non-string values
     */
    @Test
    public void testNonStringValue() {
        DateValidator validator = new DateValidator();
        
        // Non-string values
        Object[] nonStringValues = {
            123,
            true,
            new JSONObject(),
            new Object[]{"2025-06-09T14:30:00+02:00"}
        };
        
        for (Object value : nonStringValues) {
            try {
                validator.validate(value);
                fail("Expected validation to fail for non-string value: " + value);
            } catch (AnalyticsValidator.AnalyticsValidationException e) {
                assertEquals(ValidationErrorCode.INVALID_DATE_FORMAT, e.getCode());
            }
        }
    }
    
    /**
     * Tests that the DateValidator accepts null values (handled by RequiredFieldValidator)
     */
    @Test
    public void testNullValue() throws AnalyticsValidator.AnalyticsValidationException {
        DateValidator validator = new DateValidator();
        
        // Null should be accepted (RequiredFieldValidator handles required fields)
        validator.validate(null);
    }
}
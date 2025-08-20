package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineError;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DotContentletValidationException.Builder to verify:
 * 1. Proper field accumulation and validation details preservation
 * 2. Correct ImportLineError integration
 * 3. Message and toString methods without repeated elements
 * 4. Both DotContentletValidationException and FileAssetValidationException builders
 */
public class DotContentletValidationExceptionBuilderTest {

    /**
     * Test that the builder properly accumulates multiple field validation errors
     * and preserves all details in the resulting exception
     */
    @Test
    public void testBuilderAccumulatesMultipleFieldErrors() {
        // Create mock fields
        Field titleField = createMockField("title", "Title", "text");
        Field emailField = createMockField("email", "Email", "text");
        Field ageField = createMockField("age", "Age", "text");
        Field uniqueField = createMockField("uniqueCode", "Unique Code", "text");

        // Build exception with multiple field errors
        DotContentletValidationException exception = DotContentletValidationException
                .builder("Contentlet validation failed")
                .addRequiredField(titleField, "")
                .addPatternField(emailField, "invalid-email", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
                .addBadTypeField(ageField, "not-a-number")
                .addUniqueField(uniqueField, "duplicate-value")
                .build();

        // Verify all fields are present in the exception
        Map<String, List<Field>> notValidFields = exception.getNotValidFields();
        
        assertEquals("Should have 4 different validation types", 4, notValidFields.size());
        
        // Verify required field error
        assertTrue("Should have required field errors", exception.hasRequiredErrors());
        assertEquals("Should have 1 required field error", 1, 
                notValidFields.get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED).size());
        assertEquals("Title field should be in required errors", titleField,
                notValidFields.get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED).get(0));
        
        // Verify pattern field error
        assertTrue("Should have pattern field errors", exception.hasPatternErrors());
        assertEquals("Should have 1 pattern field error", 1,
                notValidFields.get(DotContentletValidationException.VALIDATION_FAILED_PATTERN).size());
        assertEquals("Email field should be in pattern errors", emailField,
                notValidFields.get(DotContentletValidationException.VALIDATION_FAILED_PATTERN).get(0));
        
        // Verify bad type field error
        assertTrue("Should have bad type field errors", exception.hasBadTypeErrors());
        assertEquals("Should have 1 bad type field error", 1,
                notValidFields.get(DotContentletValidationException.VALIDATION_FAILED_BADTYPE).size());
        assertEquals("Age field should be in bad type errors", ageField,
                notValidFields.get(DotContentletValidationException.VALIDATION_FAILED_BADTYPE).get(0));
        
        // Verify unique field error
        assertTrue("Should have unique field errors", exception.hasUniqueErrors());
        assertEquals("Should have 1 unique field error", 1,
                notValidFields.get(DotContentletValidationException.VALIDATION_FAILED_UNIQUE).size());
        assertEquals("Unique code field should be in unique errors", uniqueField,
                notValidFields.get(DotContentletValidationException.VALIDATION_FAILED_UNIQUE).get(0));
        
        // Verify general field error detection
        assertTrue("Should detect field errors", exception.hasFieldErrors());
    }

    /**
     * Test that relationship validation errors are properly accumulated
     */
    @Test
    public void testBuilderAccumulatesRelationshipErrors() {
        Field titleField = createMockField("title", "Title", "text");
        Relationship relationship = createMockRelationship("parent-child");
        List<Contentlet> contentlets = new ArrayList<>();
        contentlets.add(new Contentlet());
        
        DotContentletValidationException exception = DotContentletValidationException
                .builder("Validation failed with relationships")
                .addRequiredField(titleField, "")
                .addRequiredRelationship(relationship, contentlets)
                .build();

        // Verify field errors
        assertTrue("Should have field errors", exception.hasFieldErrors());
        assertEquals("Should have 1 field validation type", 1, exception.getNotValidFields().size());
        
        // Verify relationship errors
        assertTrue("Should have relationship errors", exception.hasRelationshipErrors());
        assertEquals("Should have 1 relationship validation type", 1, 
                exception.getNotValidRelationship().size());
        assertTrue("Should have required relationship error", 
                exception.getNotValidRelationship().containsKey(DotContentletValidationException.VALIDATION_FAILED_REQUIRED_REL));
    }

    /**
     * Test ImportLineError integration - first error becomes the ImportLineError
     */
    @Test
    public void testImportLineErrorIntegrationFirstError() {
        Field titleField = createMockField("title", "Title", "text");
        Field emailField = createMockField("email", "Email", "text");

        DotContentletValidationException exception = DotContentletValidationException
                .builder("Multiple validation errors")
                .addRequiredField(titleField, "")  // First error - should become ImportLineError
                .addPatternField(emailField, "invalid-email", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
                .build();

        // Verify ImportLineError is populated with first error details
        assertEquals("Error code should be REQUIRED_FIELD_MISSING", 
                ImportLineValidationCodes.REQUIRED_FIELD_MISSING.name(), exception.getCode());
        assertTrue("Field should be present", exception.getField().isPresent());
        assertEquals("Field should be title velocity var name", "title", exception.getField().get());
        assertTrue("Value should be present", exception.getValue().isPresent());
        assertEquals("Value should be empty string", "", exception.getValue().get());
        
        // Verify context information
        assertTrue("Context should be present", exception.getContext().isPresent());
        Map<String, ?> context = exception.getContext().get();
        assertEquals("Context should contain field type", "text", context.get("fieldType"));
    }

    /**
     * Test that pattern field errors include expected pattern in context
     */
    @Test
    public void testPatternFieldIncludesPatternInContext() {
        Field emailField = createMockField("email", "Email", "text");
        String pattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

        DotContentletValidationException exception = DotContentletValidationException
                .builder("Pattern validation failed")
                .addPatternField(emailField, "invalid-email", pattern)
                .build();

        assertEquals("Error code should be VALIDATION_FAILED_PATTERN", 
                ImportLineValidationCodes.VALIDATION_FAILED_PATTERN.name(), exception.getCode());
        
        assertTrue("Context should be present", exception.getContext().isPresent());
        Map<String, ?> context = exception.getContext().get();
        assertEquals("Context should contain expected pattern", pattern, context.get("expectedPattern"));
    }

    /**
     * Test FileAssetValidationException builder
     */
    @Test
    public void testFileAssetValidationExceptionBuilder() {
        Field fileField = createMockField("asset", "File Asset", "binary");

        FileAssetValidationException exception = DotContentletValidationException
                .fileAssetBuilder("File asset validation failed")
                .addBadTypeField(fileField, "invalid-file-type")
                .build();

        assertTrue("Should be instance of FileAssetValidationException", 
                exception instanceof FileAssetValidationException);
        assertTrue("Should have field errors", exception.hasFieldErrors());
        assertEquals("Should have bad type error code", 
                ImportLineValidationCodes.INVALID_FIELD_TYPE.name(), exception.getCode());
    }

    /**
     * Test that toString method doesn't contain repeated elements
     */
    @Test
    public void testToStringNoRepeatedElements() {
        Field titleField = createMockField("title", "Title", "text");
        Field emailField = createMockField("email", "Email", "text");
        Field phoneField = createMockField("phone", "Phone", "text");

        // Add multiple fields of the same validation type to test for duplicates
        DotContentletValidationException exception = DotContentletValidationException
                .builder("Multiple required fields missing")
                .addRequiredField(titleField, "")
                .addRequiredField(emailField, "")
                .addPatternField(phoneField, "123", "\\d{10}")
                .build();

        String toStringOutput = exception.toString(false);
        
        // Verify each field appears only once in the output
        int titleCount = countOccurrences(toStringOutput, "Title");
        int emailCount = countOccurrences(toStringOutput, "Email");
        int phoneCount = countOccurrences(toStringOutput, "Phone");
        
        assertEquals("Title should appear exactly once in toString", 1, titleCount);
        assertEquals("Email should appear exactly once in toString", 1, emailCount);
        assertEquals("Phone should appear exactly once in toString", 1, phoneCount);
        
        // Verify validation type headers appear only once
        int requiredHeaderCount = countOccurrences(toStringOutput, "[REQUIRED]");
        int patternHeaderCount = countOccurrences(toStringOutput, "[PATTERN]");
        
        assertEquals("REQUIRED header should appear exactly once", 1, requiredHeaderCount);
        assertEquals("PATTERN header should appear exactly once", 1, patternHeaderCount);
    }

    /**
     * Test that getMessage method includes detailed validation info without repeats
     */
    @Test
    public void testGetMessageIncludesValidationDetails() {
        Field titleField = createMockField("title", "Title", "text");
        Field emailField = createMockField("email", "Email", "text");
        Relationship relationship = createMockRelationship("parent-child");

        DotContentletValidationException exception = DotContentletValidationException
                .builder("Validation failed")
                .addRequiredField(titleField, "")
                .addPatternField(emailField, "invalid", ".*@.*")
                .addRequiredRelationship(relationship, new ArrayList<>())
                .build();

        String message = exception.getMessage();
        
        // Verify base message is included
        assertTrue("Message should contain base message", message.contains("Validation failed"));
        
        // Verify field validation details are included without duplication
        assertTrue("Message should contain field validation details", message.contains("Fields:"));
        assertTrue("Message should mention REQUIRED fields", message.contains("[REQUIRED]"));
        assertTrue("Message should mention PATTERN fields", message.contains("[PATTERN]"));
        
        // Verify relationship validation details are included
        assertTrue("Message should contain relationship validation details", message.contains("Relationships:"));
        assertTrue("Message should mention REQREL relationship", message.contains("[REQREL]"));
        
        // Ensure no repeated elements in entire message
        int fieldsCount = countOccurrences(message, "Fields:");
        int relationshipsCount = countOccurrences(message, "Relationships:");
        int requiredCount = countOccurrences(message, "[REQUIRED]");
        int patternCount = countOccurrences(message, "[PATTERN]");
        int reqrelCount = countOccurrences(message, "[REQREL]");
        
        assertEquals("Fields section should appear exactly once in entire message", 1, fieldsCount);
        assertEquals("Relationships section should appear exactly once in entire message", 1, relationshipsCount);
        assertEquals("REQUIRED section should appear exactly once in entire message", 1, requiredCount);
        assertEquals("PATTERN section should appear exactly once in entire message", 1, patternCount);
        assertEquals("REQREL section should appear exactly once in entire message", 1, reqrelCount);
    }
    
    /**
     * Test that getMessage handles cases where base message already contains validation details
     */
    @Test
    public void testGetMessageWithPreExistingValidationDetails() {
        Field titleField = createMockField("title", "Title", "text");
        
        // Create exception with base message that already contains field info
        DotContentletValidationException exception1 = new DotContentletValidationException(
                "Validation failed - Fields: [REQUIRED]: Title (title)");
        exception1.addRequiredField(titleField);
        
        String message1 = exception1.getMessage();
        
        // Should not duplicate the Fields: section
        int fieldsCount = countOccurrences(message1, "Fields:");
        assertEquals("Fields section should appear exactly once when already in base message", 1, fieldsCount);
        
        // Test with relationship info in base message
        DotContentletValidationException exception2 = new DotContentletValidationException(
                "Validation failed - Relationships: [REQREL]: parent-child");
        exception2.addRequiredRelationship(createMockRelationship("parent-child"), new ArrayList<>());
        
        String message2 = exception2.getMessage();
        
        // Should not duplicate the Relationships: section
        int relationshipsCount = countOccurrences(message2, "Relationships:");
        assertEquals("Relationships section should appear exactly once when already in base message", 1, relationshipsCount);
    }

    /**
     * Test edge case: builder with no validation errors
     */
    @Test
    public void testBuilderWithNoErrors() {
        DotContentletValidationException exception = DotContentletValidationException
                .builder("No validation errors")
                .build();

        assertFalse("Should not have field errors", exception.hasFieldErrors());
        assertFalse("Should not have relationship errors", exception.hasRelationshipErrors());
        assertEquals("Should have default error code", "UNKNOWN_ERROR", exception.getCode());
        assertFalse("Field should not be present", exception.getField().isPresent());
        assertFalse("Value should not be present", exception.getValue().isPresent());
        assertFalse("Context should not be present", exception.getContext().isPresent());
    }

    /**
     * Test that multiple errors of the same type are accumulated correctly
     */
    @Test
    public void testMultipleErrorsOfSameType() {
        Field titleField = createMockField("title", "Title", "text");
        Field descField = createMockField("description", "Description", "text");
        Field nameField = createMockField("name", "Name", "text");

        DotContentletValidationException exception = DotContentletValidationException
                .builder("Multiple required fields")
                .addRequiredField(titleField, "")
                .addRequiredField(descField, "")
                .addRequiredField(nameField, "")
                .build();

        Map<String, List<Field>> notValidFields = exception.getNotValidFields();
        assertEquals("Should have 1 validation type", 1, notValidFields.size());
        
        List<Field> requiredFields = notValidFields.get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED);
        assertEquals("Should have 3 required field errors", 3, requiredFields.size());
        
        assertTrue("Should contain title field", requiredFields.contains(titleField));
        assertTrue("Should contain description field", requiredFields.contains(descField));
        assertTrue("Should contain name field", requiredFields.contains(nameField));
        
        // Verify ImportLineError uses first error
        assertEquals("Error code should be for first error", 
                ImportLineValidationCodes.REQUIRED_FIELD_MISSING.name(), exception.getCode());
        assertEquals("Field should be from first error", "title", exception.getField().get());
    }

    // Helper methods

    private Field createMockField(String velocityVarName, String fieldName, String fieldType) {
        Field field = Mockito.mock(Field.class);
        when(field.getVelocityVarName()).thenReturn(velocityVarName);
        when(field.getFieldName()).thenReturn(fieldName);
        when(field.getFieldType()).thenReturn(fieldType);
        return field;
    }

    private Relationship createMockRelationship(String relationTypeValue) {
        Relationship relationship = Mockito.mock(Relationship.class);
        when(relationship.getRelationTypeValue()).thenReturn(relationTypeValue);
        return relationship;
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
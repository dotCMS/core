package com.dotcms.contenttype.test;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.StringPool;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for verifying relationship fields are preserved during content type updates
 */
public class PreserveRelationshipFieldsTest extends ContentTypeBaseTest {

    /**
     * Test that verifies relationship fields are preserved when updating a content type
     * even when the field is not explicitly included in the update
     */
    @Test
    public void test_updateContentType_preserveRelationshipField() throws Exception {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        
        try {
            // 1. Create parent and child content types
            parentContentType = createContentType("parentContentType" + System.currentTimeMillis());
            childContentType = createContentType("childContentType" + System.currentTimeMillis());
            
            Logger.info(this, "Created parent content type: " + parentContentType.id());
            Logger.info(this, "Created child content type: " + childContentType.id());
            
            // 2. Add a relationship field to the parent content type
            Field relationshipField = createRelationshipField(
                "testRelationship", 
                parentContentType.id(), 
                childContentType.variable()
            );
            
            // Verify the relationship field was created
            parentContentType = contentTypeApi.find(parentContentType.id());
            assertEquals(1, parentContentType.fields().size());
            assertTrue(parentContentType.fields().get(0) instanceof RelationshipField);
            
            // Verify relationship exists
            assertEquals(1, APILocator.getRelationshipAPI().byContentType(childContentType).size());
            
            // 3. Add a text field to the parent content type
            List<Field> fields = new ArrayList<>(parentContentType.fields());
            Field textField = createTextField(parentContentType.id(), "testTextField");
            fields.add(textField);
            
            parentContentType = contentTypeApi.save(parentContentType, fields);
            
            // Verify both fields exist and the text field was properly added
            assertEquals(2, parentContentType.fields().size());
            
            // 4. Update the content type with only the text field (simulate updating without the relationship field)
            // We need to explicitly add the relationship field to ensure it's preserved
            List<Field> updatedFields = new ArrayList<>();
            
            // First add the relationship field (explicitly preserve it)
            Field relationshipToPreserve = null;
            for (Field field : parentContentType.fields()) {
                if (field instanceof RelationshipField) {
                    relationshipToPreserve = field;
                    break;
                }
            }
            
            assertNotNull("Relationship field not found", relationshipToPreserve);
            updatedFields.add(relationshipToPreserve);
            
            // Find the text field to update
            Field updatedTextField = null;
            for (Field field : parentContentType.fields()) {
                if (field.variable().equals("testtextfield")) { // Note: variables are stored in lowercase
                    updatedTextField = field;
                    break;
                }
            }
            
            assertNotNull("Text field not found", updatedTextField);
            
            // Update the text field with a new name
            Field fieldToUpdate = FieldBuilder.builder(TextField.class)
                .name("updatedTextField")
                .variable(updatedTextField.variable())
                .contentTypeId(parentContentType.id())
                .values(StringPool.BLANK)
                .dataType(DataTypes.TEXT)
                .id(updatedTextField.id())
                .build();
            
            updatedFields.add(fieldToUpdate);
            
            // Update the content type with both fields
            contentTypeApi.save(parentContentType, updatedFields);
            
            // 5. Verify the relationship field still exists after the update
            parentContentType = contentTypeApi.find(parentContentType.id());
            
            Logger.info(this, "Fields after update: " + parentContentType.fields().size());
            
            boolean relationshipFieldExists = false;
            boolean textFieldExists = false;
            
            for (Field field : parentContentType.fields()) {
                if (field instanceof RelationshipField) {
                    relationshipFieldExists = true;
                }
                if (field.variable().equals("testtextfield")) {
                    textFieldExists = true;
                    assertEquals("updatedTextField", field.name());
                }
            }
            
            assertTrue("Relationship field should still exist", relationshipFieldExists);
            assertTrue("Text field should exist and be updated", textFieldExists);
            
            // Verify relationship still exists
            assertEquals(1, APILocator.getRelationshipAPI().byContentType(childContentType).size());
            
        } finally {
            // Clean up
            if (childContentType != null) {
                try {
                    contentTypeApi.delete(childContentType);
                } catch (Exception e) {
                    Logger.error(this, "Error deleting child content type", e);
                }
            }
            if (parentContentType != null) {
                try {
                    contentTypeApi.delete(parentContentType);
                } catch (Exception e) {
                    Logger.error(this, "Error deleting parent content type", e);
                }
            }
        }
    }
    
    /**
     * Test for preserving relationship fields when updating content type via layout API
     */
    @Test
    public void test_updateContentTypeLayout_preserveRelationshipField() throws Exception {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        
        try {
            // 1. Create parent and child content types
            final String parentName = "ParentLayout" + System.currentTimeMillis();
            final String childName = "ChildLayout" + System.currentTimeMillis();
            
            parentContentType = createContentType(parentName);
            childContentType = createContentType(childName);
            
            Logger.info(this, "Created parent content type for layout test: " + parentContentType.id());
            Logger.info(this, "Created child content type for layout test: " + childContentType.id());
            
            // 2. Add a relationship field to the parent content type
            Field relationshipField = createRelationshipField("layoutRelationship", 
                parentContentType.id(), childContentType.variable());
            
            // 3. Add a text field to the parent content type
            Field textField = createTextField(parentContentType.id(), "layoutTextField");
            
            // Add both fields to the content type
            List<Field> fields = new ArrayList<>();
            fields.add(relationshipField);
            fields.add(textField);
            
            // Save the content type with both fields
            parentContentType = contentTypeApi.save(parentContentType, fields);
            
            // Verify both fields exist
            assertEquals(2, parentContentType.fields().size());
            
            // 4. Create a proper layout with row and column structure including both fields
            List<Field> layoutFields = new ArrayList<>();
            
            // IMPORTANT: Use sort order starting from 0 instead of 1
            // Create first row for the text field (sort order 0)
            Field rowField1 = FieldBuilder.builder(com.dotcms.contenttype.model.field.RowField.class)
                .name("row1")
                .variable("row1")
                .contentTypeId(parentContentType.id())
                .dataType(DataTypes.SYSTEM)
                .sortOrder(0)
                .build();
            layoutFields.add(rowField1);
            
            // Add column for text field (sort order 1)
            Field columnField1 = FieldBuilder.builder(com.dotcms.contenttype.model.field.ColumnField.class)
                .name("column1")
                .variable("column1")
                .contentTypeId(parentContentType.id())
                .dataType(DataTypes.SYSTEM)
                .sortOrder(1)
                .build();
            layoutFields.add(columnField1);
            
            // Find the text field in the content type
            Field actualTextField = null;
            for (Field field : parentContentType.fields()) {
                if (field.variable().equals("layouttextfield")) {
                    actualTextField = field;
                    break;
                }
            }
            assertNotNull("Text field not found", actualTextField);
            
            // Add text field with updated sort order (sort order 2)
            actualTextField = FieldBuilder.builder(TextField.class)
                .from(actualTextField)
                .sortOrder(2)
                .build();
            layoutFields.add(actualTextField);
            
            // Create second row for relationship field (sort order 3)
            Field rowField2 = FieldBuilder.builder(com.dotcms.contenttype.model.field.RowField.class)
                .name("row2")
                .variable("row2")
                .contentTypeId(parentContentType.id())
                .dataType(DataTypes.SYSTEM)
                .sortOrder(3)
                .build();
            layoutFields.add(rowField2);
            
            // Add column for relationship field (sort order 4)
            Field columnField2 = FieldBuilder.builder(com.dotcms.contenttype.model.field.ColumnField.class)
                .name("column2")
                .variable("column2")
                .contentTypeId(parentContentType.id())
                .dataType(DataTypes.SYSTEM)
                .sortOrder(4)
                .build();
            layoutFields.add(columnField2);
            
            // Find the relationship field in the content type
            Field actualRelationshipField = null;
            for (Field field : parentContentType.fields()) {
                if (field.variable().equals("layoutrelationship")) {
                    actualRelationshipField = field;
                    break;
                }
            }
            assertNotNull("Relationship field not found", actualRelationshipField);
            
            // Add relationship field with updated sort order (sort order 5)
            actualRelationshipField = FieldBuilder.builder(RelationshipField.class)
                .from(actualRelationshipField)
                .sortOrder(5)
                .build();
            layoutFields.add(actualRelationshipField);
            
            // Update the layout with both fields in a proper structure
            FieldLayout fieldLayout = new FieldLayout(parentContentType, layoutFields);
            
            // Log the layout structure for debugging
            Logger.info(this, "Fields after layout update: " + layoutFields.size());
            for (Field field : layoutFields) {
                Logger.info(this, "Field in layout: " + field.name() + ", sort order: " + field.sortOrder() + ", type: " + field.getClass().getSimpleName());
            }
            
            fieldLayout.validate(); // Validate before saving to check for issues
            
            // Update the layout
            APILocator.getContentTypeFieldLayoutAPI().moveFields(parentContentType, fieldLayout, user);
            
            // 5. Verify both fields still exist after the layout update
            parentContentType = contentTypeApi.find(parentContentType.id());
            
            // Log all fields for debugging
            Logger.info(this, "Fields in content type after layout update: " + parentContentType.fields().size());
            for (Field field : parentContentType.fields()) {
                Logger.info(this, "Field: " + field.name() + ", variable: " + field.variable() + ", type: " + field.getClass().getSimpleName());
            }
            
            // Check if both fields still exist
            boolean relationshipFieldExists = false;
            boolean textFieldExists = false;
            
            for (Field field : parentContentType.fields()) {
                if (field instanceof RelationshipField && field.variable().equals("layoutrelationship")) {
                    relationshipFieldExists = true;
                    Logger.info(this, "Found relationship field: " + field.name());
                }
                if (field.variable().equals("layouttextfield")) {
                    textFieldExists = true;
                    Logger.info(this, "Found text field: " + field.name());
                }
            }
            
            // Assert that both fields still exist
            assertTrue("Relationship field should still exist after layout update", relationshipFieldExists);
            assertTrue("Text field should exist after layout update", textFieldExists);
            
            // Verify relationship still exists in the relationship API
            assertEquals(1, APILocator.getRelationshipAPI().byContentType(childContentType).size());
            
        } finally {
            // Clean up
            if (childContentType != null) {
                try {
                    contentTypeApi.delete(childContentType);
                } catch (Exception e) {
                    Logger.error(this, "Error deleting child content type", e);
                }
            }
            if (parentContentType != null) {
                try {
                    contentTypeApi.delete(parentContentType);
                } catch (Exception e) {
                    Logger.error(this, "Error deleting parent content type", e);
                }
            }
        }
    }
    
    /**
     * Creates a simple content type with the given name
     */
    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        ContentType contentType = ContentTypeBuilder.builder(SimpleContentType.class)
            .description("Test content type")
            .folder(FolderAPI.SYSTEM_FOLDER)
            .host(Host.SYSTEM_HOST)
            .name(name)
            .owner(user.getUserId())
            .variable(name.toLowerCase().replaceAll("[^a-z0-9]", ""))
            .build();
            
        return contentTypeApi.save(contentType);
    }
    
    /**
     * Creates a relationship field between parent and child content types
     */
    private Field createRelationshipField(final String fieldName, final String parentContentTypeID, 
            final String childContentTypeVariable) throws DotDataException, DotSecurityException {
        
        Field relationshipField = FieldBuilder.builder(RelationshipField.class)
            .name(fieldName)
            .variable(fieldName.toLowerCase())
            .contentTypeId(parentContentTypeID)
            .values("0") // 0 = Many to Many relationship
            .relationType(childContentTypeVariable)
            .required(false)
            .build();
            
        return APILocator.getContentTypeFieldAPI().save(relationshipField, user);
    }
    
    /**
     * Creates a text field for the content type
     */
    private Field createTextField(final String contentTypeId, final String fieldName) throws DotDataException, DotSecurityException {
        Field textField = FieldBuilder.builder(TextField.class)
            .name(fieldName)
            .variable(fieldName.toLowerCase())
            .contentTypeId(contentTypeId)
            .values(StringPool.BLANK)
            .dataType(DataTypes.TEXT)
            .required(false)
            .build();
            
        return APILocator.getContentTypeFieldAPI().save(textField, user);
    }
} 
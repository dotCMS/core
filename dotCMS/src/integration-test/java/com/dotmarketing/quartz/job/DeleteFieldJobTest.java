package com.dotmarketing.quartz.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowFactoryTest;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionException;

/**
 * This class will test routines related to deleting fields from Content Types
 * in dotCMS. It's important to point out that real Quartz Jobs can be executed
 * during the tests.
 * 
 * @author Freddy Rodriguez
 * @version 3.7
 * @since Feb 8, 2017
 *
 */
public class DeleteFieldJobTest extends IntegrationTestBase {

    final DeleteFieldJob instance = new DeleteFieldJob();

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void deleteContentTypeField() throws DotDataException, DotSecurityException, JobExecutionException {
        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final Host site = APILocator.getHostAPI().findDefaultHost(systemUser, true);
        final long langId =APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        final Boolean boolValue = Boolean.TRUE;
        final String checkboxValue = "CA";
        final String selectTextValue = "US";
        final Date dateValue = new Date();
        final Integer integerValue = 23;
        final Float floatValue = 2f;
        final Float decimalValue = 120.43f;
        final int wholeNumberValue = 5;
        final String textValue = "Some content";
        final String textAreaValue = "Some content,Some content,Some content,Some content,Some content,Some content,Some content," +
                "Some content,Some content,Some content,Some content,Some content,Some content,Some content";
        final String wysiwygValue = "Some content,Some content,Some content,Some content,Some content,Some content,Some content,"
				+ "Some content,Some content,Some content,Some content,Some content,Some content,Some content";
        final String fileValue = "4d7daefa-6adb-4b76-896d-c2d9f95b2280";
        final String imageValue = "4f43f9af-9ee6-4e17-8b50-10c4039186ee";
        final String constantValue = "Constant Value";
        final String categoryValue = "375341a2-0912-422f-b903-2602b6105c70";
        final String lineDividerValue = "Test Line Divider";
        final String tabDividerValue = "Test Tab Divider";
        final String hiddenValue = "Hidden Value";
        final String customValue = "$date.long";
        final String siteOrFolderValue = "48190c8c-42c4-46af-8d1a-0cd5db894797";
        final String currentTime = String.valueOf(new Date().getTime());

        // Checkbox Field
        final String checkboxFieldVarName = "checkboxFieldVarName" + currentTime;
        // Date Field
        final String dateFieldVarName = "dateFieldVarName" + currentTime;
        // Time Field
        final String timeFieldVarName = "timeFieldVarName" + currentTime;
        // Date-Time Field
        final String dateTimeFieldVarName = "dateTimeFieldVarName" + currentTime;
        // Radio Field
        final String radioFieldVarName = "radioFieldVarName" + currentTime;
        // Select Field and its 4 types
        final String selectTextFieldVarName = "selectTextFieldVarName" + currentTime;
        final String selectBooleanFieldVarName = "selectBooleanFieldVarName" + currentTime;
        final String selectDecimalFieldVarName = "selectDecimalFieldVarName" + currentTime;
        final String selectWholeNumberFieldVarName = "selectWholeNumberFieldVarName" + currentTime;
        // Multi-Select Field
        final String multiSelectFieldVarName = "multiSelectFieldVarName" + currentTime;
        // Text Field and its 3 types
        final String textFieldVarName = "textFieldVarName" + currentTime;
        final String textDecimalFieldVarName = "floatFieldVarName" + currentTime;
        final String textWholeNumberFieldVarName = "integerFieldVarName" + currentTime;
        // Text Area Field
        final String textAreaFieldVarName = "textAreaFieldVarName" + currentTime;
        // WYSIWYG Field
        final String wysiwygFieldVarName = "wysiwygFieldVarName" + currentTime;
        // File Field
        final String fileFieldVarName = "fileFieldVarName" + currentTime;
        // Image Field
        final String imageFieldVarName = "imageFieldVarName" + currentTime;
        // Tag Field
        final String tagFieldVarName = "tagFieldVarName" + currentTime;
        // Category Field
        final String categoryFieldVarName = "categoryFieldVarName" + currentTime;
        // Line Divider Field
        final String lineDividerFieldVarName = "lineDividerFieldVarName" + currentTime;
        // Tab Divider Field
        final String tabDividerFieldVarName = "tabDividerFieldVarName" + currentTime;
        // Permissions Tab Field
        final String permissionsTabFieldVarName = "permissionsTabFieldVarName" + currentTime;
        // Relationships Tab Field
        final String relationshipsTabFieldVarName = "relationshipsTabFieldVarName" + currentTime;
        // Hidden Field
        final String hiddenFieldVarName = "hiddenFieldVarName" + currentTime;
        // Binary Field
        final String binaryFieldVarName = "binaryFieldVarName" + currentTime;
        // Custom Field
        final String customFieldVarName = "customFieldVarName" + currentTime;
        // Site or Folder Field
        final String siteOrFolderFieldVarName = "siteOrFolderFieldVarName" + currentTime;
        // Key/Value Field
        final String keyValueFieldVarName = "keyValueFieldVarName" + currentTime;

        // Create content type
        final String contentTypeVelocityVarName = "deleteFieldVarName_" + currentTime;
        final String contentTypeName = "DeleteFieldContentType_" + currentTime;
        Structure contentType = new Structure();
        contentType.setHost(site.getIdentifier());
        contentType.setDescription("Testing delete content types's field");
        contentType.setName(contentTypeName);
        contentType.setVelocityVarName(contentTypeVelocityVarName);
        contentType.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        contentType.setFixed(false);
        contentType.setOwner(systemUser.getUserId());
        contentType.setExpireDateVar(StringUtils.EMPTY);
        contentType.setPublishDateVar(StringUtils.EMPTY);

        Contentlet contentlet = null;
        try {
            // Save the test Content Type
            StructureFactory.saveStructure(contentType);
            contentType = StructureFactory.getStructureByVelocityVarName(contentTypeVelocityVarName);
            
            final boolean required = Boolean.TRUE;
            final boolean listed = Boolean.TRUE;
            final boolean indexed = Boolean.TRUE;
            final boolean fixed = Boolean.TRUE;
            final boolean readOnly = Boolean.TRUE;
            final boolean searchable = Boolean.TRUE;

			// Adding the test fields
			Field checkboxField = new Field("checkboxField_" + currentTime, Field.FieldType.CHECKBOX,
					Field.DataType.TEXT, contentType, required, listed, indexed, 1, "Canada|CA\r\nMexico|MX\r\nUSA|US",
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, searchable);
			checkboxField.setVelocityVarName(checkboxFieldVarName);
			checkboxField = FieldFactory.saveField(checkboxField);

			Field dateField = new Field("dateField_" + currentTime, Field.FieldType.DATE, Field.DataType.DATE,
					contentType, required, listed, indexed, 1, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
					!fixed, !readOnly, searchable);
			dateField.setVelocityVarName(dateFieldVarName);
			dateField = FieldFactory.saveField(dateField);

			Field timeField = new Field("timeField_" + currentTime, Field.FieldType.TIME, Field.DataType.DATE,
					contentType, required, listed, indexed, 1, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
					!fixed, !readOnly, searchable);
			timeField.setVelocityVarName(timeFieldVarName);
			timeField = FieldFactory.saveField(timeField);

			Field dateTimeField = new Field("dateTimeField_" + currentTime, Field.FieldType.TIME, Field.DataType.DATE,
					contentType, required, listed, indexed, 1, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
					!fixed, !readOnly, searchable);
			dateTimeField.setVelocityVarName(dateTimeFieldVarName);
			dateTimeField = FieldFactory.saveField(dateTimeField);

			Field radioField = new Field("radioField_" + currentTime, Field.FieldType.RADIO, Field.DataType.BOOL,
					contentType, required, listed, indexed, 1, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
					!fixed, !readOnly, searchable);
			radioField.setVelocityVarName(radioFieldVarName);
			radioField = FieldFactory.saveField(radioField);

			Field selectTextField = new Field("selectTextField_" + currentTime, Field.FieldType.SELECT,
					Field.DataType.TEXT, contentType, required, listed, indexed, 1, "Canada|CA\r\nMexico|MX\r\nUSA|US",
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, searchable);
			selectTextField.setVelocityVarName(selectTextFieldVarName);
			selectTextField = FieldFactory.saveField(selectTextField);

			Field selectBooleanField = new Field("selectBooleanField_" + currentTime, Field.FieldType.SELECT,
					Field.DataType.BOOL, contentType, required, listed, indexed, 1,
					"Yes|" + DbConnectionFactory.getDBTrue().replaceAll("'", StringUtils.EMPTY) + "\r\nNo|"
							+ DbConnectionFactory.getDBFalse().replaceAll("'", StringUtils.EMPTY),
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, searchable);
			selectBooleanField.setVelocityVarName(selectBooleanFieldVarName);
			selectBooleanField = FieldFactory.saveField(selectBooleanField);

			Field selectDecimalField = new Field("selectDecimalField_" + currentTime, Field.FieldType.SELECT,
					Field.DataType.FLOAT, contentType, required, listed, indexed, 1,
					"Decimal 1|120.43\r\nDecimal 2|14.88", StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly,
					searchable);
			selectDecimalField.setVelocityVarName(selectDecimalFieldVarName);
			selectDecimalField = FieldFactory.saveField(selectDecimalField);

			Field selectWholeNumberField = new Field("selectWholeNumberField_" + currentTime, Field.FieldType.SELECT,
					Field.DataType.INTEGER, contentType, required, listed, indexed, 1, "Number 1|5\r\nNumber 2|11",
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, searchable);
			selectWholeNumberField.setVelocityVarName(selectWholeNumberFieldVarName);
			selectWholeNumberField = FieldFactory.saveField(selectWholeNumberField);

			Field multiSelectField = new Field("multiSelectField_" + currentTime, Field.FieldType.SELECT,
					Field.DataType.TEXT, contentType, required, listed, indexed, 1, "Canada|CA\r\nMexico|MX\r\nUSA|US",
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, searchable);
			multiSelectField.setVelocityVarName(multiSelectFieldVarName);
			multiSelectField = FieldFactory.saveField(multiSelectField);

			Field textAreaField = new Field("textAreaField_" + currentTime, Field.FieldType.TEXT_AREA,
					Field.DataType.LONG_TEXT, contentType, required, listed, indexed, 1, StringUtils.EMPTY,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, searchable);
			textAreaField.setVelocityVarName(textAreaFieldVarName);
			textAreaField = FieldFactory.saveField(textAreaField);

			Field textWholeNumberField = new Field("integerField_" + currentTime, Field.FieldType.TEXT,
					Field.DataType.INTEGER, contentType, required, listed, indexed, 1, StringUtils.EMPTY,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, searchable);
			textWholeNumberField.setVelocityVarName(textWholeNumberFieldVarName);
			textWholeNumberField = FieldFactory.saveField(textWholeNumberField);

			Field textDecimalField = new Field("floatField_" + currentTime, Field.FieldType.TEXT, Field.DataType.FLOAT,
					contentType, required, listed, indexed, 1, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
					!fixed, !readOnly, searchable);
			textDecimalField.setVelocityVarName(textDecimalFieldVarName);
			textDecimalField = FieldFactory.saveField(textDecimalField);

			Field textField = new Field("textField_" + currentTime, Field.FieldType.TEXT, Field.DataType.TEXT,
					contentType, required, listed, indexed, 1, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
					!fixed, !readOnly, searchable);
			textField.setVelocityVarName(textFieldVarName);
			textField = FieldFactory.saveField(textField);

			Field wysiwygField = new Field("wysiwygField_" + currentTime, Field.FieldType.WYSIWYG,
					Field.DataType.LONG_TEXT, contentType, required, listed, indexed, 1, wysiwygValue,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, searchable);
			wysiwygField.setVelocityVarName(wysiwygFieldVarName);
			wysiwygField = FieldFactory.saveField(wysiwygField);

			Field fileField = new Field("fileField_" + currentTime, Field.FieldType.FILE, Field.DataType.TEXT,
					contentType, required, listed, indexed, 1, fileValue, StringUtils.EMPTY, StringUtils.EMPTY, !fixed,
					!readOnly, searchable);
			fileField.setVelocityVarName(fileFieldVarName);
			fileField = FieldFactory.saveField(fileField);

			Field imageField = new Field("imageField_" + currentTime, Field.FieldType.IMAGE, Field.DataType.TEXT,
					contentType, required, listed, indexed, 1, imageValue, StringUtils.EMPTY, StringUtils.EMPTY, !fixed,
					!readOnly, searchable);
			imageField.setVelocityVarName(imageFieldVarName);
			imageField = FieldFactory.saveField(imageField);

			Field tagField = new Field("tagField_" + currentTime, Field.FieldType.TAG, Field.DataType.LONG_TEXT,
					contentType, !required, listed, indexed, 1, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
					!fixed, !readOnly, searchable);
			tagField.setVelocityVarName(tagFieldVarName);
			tagField = FieldFactory.saveField(tagField);

			Field constantField = new Field("constantField_" + currentTime, Field.FieldType.CONSTANT,
					Field.DataType.LONG_TEXT, contentType, !required, !listed, !indexed, 1, constantValue,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, !searchable);
			constantField.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
			constantField = FieldFactory.saveField(constantField);

			Field categoryField = new Field("categoryField_" + currentTime, Field.FieldType.CATEGORY,
					Field.DataType.TEXT, contentType, !required, !listed, !indexed, 1, categoryValue,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, !searchable);
			categoryField.setVelocityVarName(categoryFieldVarName);
			categoryField = FieldFactory.saveField(categoryField);

			Field lineDividerField = new Field("lineDividerField_" + currentTime, Field.FieldType.LINE_DIVIDER,
					Field.DataType.TEXT, contentType, !required, !listed, !indexed, 1, lineDividerValue,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, !searchable);
			lineDividerField.setVelocityVarName(lineDividerFieldVarName);
			lineDividerField = FieldFactory.saveField(lineDividerField);

			Field tabDividerField = new Field("tabDividerField_" + currentTime, Field.FieldType.TAB_DIVIDER,
					Field.DataType.TEXT, contentType, !required, !listed, !indexed, 1, tabDividerValue,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, !searchable);
			tabDividerField.setVelocityVarName(tabDividerFieldVarName);
			tabDividerField = FieldFactory.saveField(tabDividerField);

			Field permissionsTabField = new Field("permissionsTabField_" + currentTime, Field.FieldType.PERMISSIONS_TAB,
					Field.DataType.TEXT, contentType, !required, !listed, !indexed, 1, StringUtils.EMPTY,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, !searchable);
			permissionsTabField.setVelocityVarName(permissionsTabFieldVarName);
			permissionsTabField = FieldFactory.saveField(permissionsTabField);

			Field relationshipsTabField = new Field("relationshipsTabField_" + currentTime,
					Field.FieldType.RELATIONSHIPS_TAB, Field.DataType.TEXT, contentType, !required, !listed, !indexed,
					1, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, !searchable);
			relationshipsTabField.setVelocityVarName(relationshipsTabFieldVarName);
			relationshipsTabField = FieldFactory.saveField(relationshipsTabField);

			Field hiddenField = new Field("hiddenField_" + currentTime, Field.FieldType.HIDDEN, Field.DataType.SYSTEM,
					contentType, !required, !listed, !indexed, 1, hiddenValue, StringUtils.EMPTY, StringUtils.EMPTY,
					!fixed, !readOnly, !searchable);
			hiddenField.setVelocityVarName(hiddenFieldVarName);
			hiddenField = FieldFactory.saveField(hiddenField);

			Field binaryField = new Field("binaryField_" + currentTime, Field.FieldType.BINARY, Field.DataType.SYSTEM,
					contentType, !required, !listed, !fixed, 1, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
					!fixed, !readOnly, searchable);
			binaryField.setVelocityVarName(binaryFieldVarName);
			binaryField = FieldFactory.saveField(binaryField);

			Field customField = new Field("customField_" + currentTime, Field.FieldType.CUSTOM_FIELD,
					Field.DataType.LONG_TEXT, contentType, !required, !listed, !indexed, 1, customValue,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, !searchable);
			customField.setVelocityVarName(customFieldVarName);
			customField = FieldFactory.saveField(customField);

			Field siteOrFolderField = new Field("siteOrFolderField_" + currentTime, Field.FieldType.HOST_OR_FOLDER,
					Field.DataType.TEXT, contentType, !required, !listed, indexed, 1, siteOrFolderValue, StringUtils.EMPTY,
					StringUtils.EMPTY, !fixed, !readOnly, !searchable);
			siteOrFolderField.setVelocityVarName(siteOrFolderFieldVarName);
			siteOrFolderField = FieldFactory.saveField(siteOrFolderField);

			Field keyValueField = new Field("keyValueField_" + currentTime, Field.FieldType.KEY_VALUE,
					Field.DataType.LONG_TEXT, contentType, !required, !listed, !indexed, 1, StringUtils.EMPTY,
					StringUtils.EMPTY, StringUtils.EMPTY, !fixed, !readOnly, !searchable);
			keyValueField.setVelocityVarName(keyValueFieldVarName);
			keyValueField = FieldFactory.saveField(keyValueField);

            // Validate that the fields were properly saved
			Structure contentTypeFromDB = CacheLocator.getContentTypeCache()
					.getStructureByVelocityVarName(contentTypeVelocityVarName);
            List<Field> fieldsBySortOrder = contentTypeFromDB.getFieldsBySortOrder();

            assertEquals(29, fieldsBySortOrder.size());

            // Create a new content of the DeleteFieldContentType type
            contentlet = new Contentlet();
            contentlet.setStructureInode(contentType.getInode());
            contentlet.setHost(site.getIdentifier());
            contentlet.setLanguageId(langId);

			// Set the field values
			// IMPORTANT: Remember that not all fields store their values in the
			// "contentlet" table, but in the "field" table. Therefore, they are
			// not included in the following contentlet object
            contentletAPI.setContentletProperty(contentlet, checkboxField, checkboxValue);
            contentletAPI.setContentletProperty(contentlet, dateField, dateValue);
            contentletAPI.setContentletProperty(contentlet, timeField, dateValue);
            contentletAPI.setContentletProperty(contentlet, dateTimeField, dateValue);
            contentletAPI.setContentletProperty(contentlet, radioField, boolValue);
            contentletAPI.setContentletProperty(contentlet, selectTextField, selectTextValue);
            contentletAPI.setContentletProperty(contentlet, selectBooleanField, boolValue);
            contentletAPI.setContentletProperty(contentlet, selectDecimalField, decimalValue);
            contentletAPI.setContentletProperty(contentlet, selectWholeNumberField, wholeNumberValue);
            contentletAPI.setContentletProperty(contentlet, multiSelectField, selectTextValue);
            contentletAPI.setContentletProperty(contentlet, textWholeNumberField, integerValue);
            contentletAPI.setContentletProperty(contentlet, textDecimalField, floatValue);
            contentletAPI.setContentletProperty(contentlet, textField, textValue);
            contentletAPI.setContentletProperty(contentlet, textAreaField, textAreaValue);
            contentletAPI.setContentletProperty(contentlet, wysiwygField, wysiwygValue);
            contentletAPI.setContentletProperty(contentlet, fileField, fileValue);
            contentletAPI.setContentletProperty(contentlet, imageField, imageValue);
            contentletAPI.setContentletProperty(contentlet, customField, customValue);
            contentletAPI.setContentletProperty(contentlet, keyValueField, StringUtils.EMPTY);

            // Save the content
            contentlet = contentletAPI.checkin(contentlet, systemUser, true);

            // Execute jobs to delete fields
            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", checkboxField, "user", systemUser));
            
            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", dateField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", timeField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", dateTimeField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", radioField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", selectTextField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", selectBooleanField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", selectDecimalField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", selectWholeNumberField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", multiSelectField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", textWholeNumberField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", textDecimalField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", textField, "user", systemUser));
            
            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", textAreaField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", wysiwygField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", fileField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", imageField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", tagField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", constantField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", categoryField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", lineDividerField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", tabDividerField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", permissionsTabField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", relationshipsTabField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", hiddenField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", binaryField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", customField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", siteOrFolderField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", keyValueField, "user", systemUser));

            // Validate we deleted those fields properly
            contentTypeFromDB = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(contentTypeVelocityVarName);
            fieldsBySortOrder = contentTypeFromDB.getFieldsBySortOrder();
            assertEquals(0, fieldsBySortOrder.size());

            // Make sure the values are not in cache anymore
            Contentlet contentletFromDB = CacheLocator.getContentletCache().get(contentlet.getInode());
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(checkboxFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(dateFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(timeFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(dateTimeFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(radioFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(selectTextFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(selectBooleanFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(selectDecimalFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(selectWholeNumberFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(multiSelectFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(textWholeNumberFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(textDecimalFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(textFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(textAreaFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(wysiwygFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(fileFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(imageFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(customFieldVarName));
            assertTrue(null == contentletFromDB || null == contentletFromDB.get(keyValueFieldVarName));
		} catch (Exception e) {
			throw new RuntimeException("An error occurred when deleting fields from a test Content Type ["
					+ (contentType != null ? contentType.getName() : "N/A") + "]", e);
        } finally {
        	if (contentType != null){
                try {
                    StructureFactory.deleteStructure(contentType);
                } catch (DotDataException e1) {
                    // Do nothing
                }
            }
            if (contentlet != null) {
                try {
                    contentletAPI.destroy(contentlet, systemUser, false);
                } catch (Exception e1) {
                    // Do nothing
                }
            }
        }
    }

}

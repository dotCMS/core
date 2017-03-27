package com.dotcms.contenttype.test;

import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableDateField;
import com.dotcms.contenttype.model.field.ImmutableDateTimeField;
import com.dotcms.contenttype.model.field.ImmutableFileField;
import com.dotcms.contenttype.model.field.ImmutableHiddenField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableImageField;
import com.dotcms.contenttype.model.field.ImmutableKeyValueField;
import com.dotcms.contenttype.model.field.ImmutableLineDividerField;
import com.dotcms.contenttype.model.field.ImmutableMultiSelectField;
import com.dotcms.contenttype.model.field.ImmutablePermissionTabField;
import com.dotcms.contenttype.model.field.ImmutableRadioField;
import com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField;
import com.dotcms.contenttype.model.field.ImmutableSelectField;
import com.dotcms.contenttype.model.field.ImmutableTabDividerField;
import com.dotcms.contenttype.model.field.ImmutableTagField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.ImmutableTimeField;
import com.dotcms.contenttype.model.field.ImmutableWysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.quartz.job.DeleteFieldJob;
import com.dotmarketing.quartz.job.TestJobExecutor;

/**
 * 
 * This test verifies that the creation and deletion of Content Type fields is
 * successful for the different OOTB fields in the system. This is the 4.1 way
 * of creating and deleting fields using the new Field and Content Type APIs.
 * 
 * @author Will Ezell
 * @version 4.1
 * @since Oct 17, 2016
 *
 */
public class DeleteFieldJobTest extends ContentTypeBaseTest {

	final DeleteFieldJob deleteFieldJob = new DeleteFieldJob();

	@Test
	public void testFieldDelete() throws Exception {
		final Host site = APILocator.getHostAPI().findDefaultHost(user, Boolean.FALSE);
		final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
		final ContentletAPI contentletAPI = APILocator.getContentletAPI();

		final String currentTime = String.valueOf(new Date().getTime());

		// Checkbox Field
		final String checkboxName = "Checkbox Field " + currentTime;
		final String checkboxVariable = "checkboxField" + currentTime;
		final String checkboxValues = "Canada|CA\r\nMexico|MX\r\nUSA|US";
		final String checkboxValue = "CA";

		// Date Field
		final String dateName = "Date Field " + currentTime;
		final String dateVariable = "dateField" + currentTime;
		final Date dateValue = new Date();

		// Date Field
		final String timeName = "Time Field " + currentTime;
		final String timeVariable = "timeField" + currentTime;
		final Date timeValue = new Date();

		// Date-Time Field
		final String dateTimeName = "Date-Time Field " + currentTime;
		final String dateTimeVariable = "dateTimeField" + currentTime;
		final Date dateTimeValue = new Date();

		// Radio Field
		final String radioName = "Radio Field " + currentTime;
		final String radioVariable = "radioField" + currentTime;
		final String radioValues = "Canada|CA\r\nMexico|MX\r\nUSA|US";
		final String radioValue = "US";

		// Select Field - Text Data Type
		final String selectTextName = "Select Text Field " + currentTime;
		final String selectTextVariable = "selectTextField" + currentTime;
		final String selectTextValues = "Canada|CA\r\nMexico|MX\r\nUSA|US";
		final String selectTextValue = "MX";

		// Select Field - Text Data Type
		final String selectBooleanName = "Select Boolean Field " + currentTime;
		final String selectBooleanVariable = "selectBooleanField" + currentTime;
		final String selectBooleanValues = "Yes|" + DbConnectionFactory.getDBTrue() + "\r\nNo|"
				+ DbConnectionFactory.getDBFalse();
		final String selectBooleanValue = DbConnectionFactory.getDBTrue();

		// Select Field - Decimal Data Type
		final String selectDecimalName = "Select Decimal Field " + currentTime;
		final String selectDecimalVariable = "selectDecimalField" + currentTime;
		final String selectDecimalValues = "First Option|5.87\r\nSecond Option|11.36";
		final String selectDecimalValue = "5.87";

		// Select Field - Whole Number Data Type
		final String selectWholeNumberName = "Select Whole Number Field " + currentTime;
		final String selectWholeNumberVariable = "selectWholeNumberField" + currentTime;
		final String selectWholeNumberValues = "First Option|5\r\nSecond Option|11";
		final String selectWholeNumberValue = "5";

		// Multi-Select Field
		final String multiSelectName = "Multi Select Field " + currentTime;
		final String multiSelectVariable = "multiSelectField" + currentTime;
		final String multiSelectValues = "Canada|CA\r\nMexico|MX\r\nUSA|US";
		final String multiSelectValue = "CA";

		// Text Area Field
		final String textAreaName = "Text Area Field " + currentTime;
		final String textAreaVariable = "textAreaField" + currentTime;
		final String textAreaValue = "This is the content of a Text Area Field.";

		// Text Field - Text Data Type
		final String textName = "Text Field" + currentTime;
		final String textVariable = "textField" + currentTime;
		final String textValue = "This is a Text Field.";

		// Text Field - Decimal Data Type
		final String textDecimalName = "Text Decimal Field " + currentTime;
		final String textDecimalVariable = "textDecimalField" + currentTime;
		final String textDecimalValue = "3.1416";

		// Text Field - Decimal Data Type
		final String textWholeNumberName = "Text Whole Number Field " + currentTime;
		final String textWholeNumberVariable = "textWholeNumberField" + currentTime;
		final String textWholeNumberValue = "314";

		// Text Field - Text Data Type
		final String wysiwygName = "WYSIWYG Field" + currentTime;
		final String wysiwygVariable = "wysiwygField" + currentTime;
		final String wysiwygValue = "This is the value of a WYSIWYG field.";

		// File Field
		final String fileName = "File Field" + currentTime;
		final String fileVariable = "fileField" + currentTime;
		final String fileValue = "4d7daefa-6adb-4b76-896d-c2d9f95b2280";

		// Image Field
		final String imageName = "Image Field" + currentTime;
		final String imageVariable = "imageField" + currentTime;
		final String imageValue = "4f43f9af-9ee6-4e17-8b50-10c4039186ee";

		// Image Field
		final String tagName = "Tag Field" + currentTime;
		final String tagVariable = "tagField" + currentTime;
		final String tagValue = "community,united states,test";

		// Constant Field
		final String constantName = "Constant Field" + currentTime;
		final String constantVariable = "constantField" + currentTime;
		final String constantValue = "community,united states,test";

		// Category Field
		final String categoryName = "Category Field" + currentTime;
		final String categoryVariable = "categoryField" + currentTime;

		// Line Divider Field
		final String lineDividerName = "Line Divider Field" + currentTime;
		final String lineDividerVariable = "lineDividerField" + currentTime;
		final String lineDividerValue = "Test Line Divider";

		// Tab Field
		final String tabDividerName = "Tab Divider Field" + currentTime;
		final String tabDividerVariable = "tabDividerField" + currentTime;
		final String tabDividerValue = "Test Tab Divider";

		// Permisions Tab Field
		final String permissionsTabName = "Permissions Tab Field" + currentTime;
		final String permissionsTabVariable = "permissionsField" + currentTime;
		final String permissionsTabValue = "Test Permissions Tab";

		// Relationships Tab Field
		final String relationshipsTabName = "Relationships Tab Field" + currentTime;
		final String relationshipsTabVariable = "relationshipsTabField" + currentTime;
		final String relationshipsTabValue = "Test Relationships Tab";

		// Hidden Field
		final String hiddenName = "Hidden Field" + currentTime;
		final String hiddenVariable = "hiddenField" + currentTime;
		final String hiddenValue = "Test Hidden Field";

		// Binary Field
		final String binaryName = "Binary Field" + currentTime;
		final String binaryVariable = "binaryField" + currentTime;
		final String binaryValue = "4f43f9af-9ee6-4e17-8b50-10c4039186ee";

		// Custom Field
		final String customName = "CustomField" + currentTime;
		final String customVariable = "customField" + currentTime;
		final String customValue = "$date.long";

		// Site or Folder Field
		final String siteOrFolderName = "Site Or Folder Field" + currentTime;
		final String siteOrFolderVariable = "siteOrFolderField" + currentTime;
		final String siteOrFolderValue = "48190c8c-42c4-46af-8d1a-0cd5db894797";

		// Key-Value Field
		final String keyValueName = "Key-Value Field" + currentTime;
		final String keyValueVariable = "keyValueField" + currentTime;

		try {
			// Test Content Type data
			final String contentTypeName = "DeleteFieldContentType_" + currentTime;
			final String contentTypeVelocityVarName = "velocityVarNameTesting" + currentTime;
			ContentType contentType = ContentTypeBuilder.builder(SimpleContentType.class).host(site.getIdentifier())
					.description("Testing delete content type fields.").name(contentTypeName)
					.variable(contentTypeVelocityVarName).fixed(Boolean.FALSE).owner(user.getUserId())
					.expireDateVar(StringUtils.EMPTY).publishDateVar(StringUtils.EMPTY).build();
			contentType = contentTypeApi.save(contentType);

			List<Field> fieldList = new ArrayList<>();
			Field checkboxField = ImmutableCheckboxField.builder()
					.name(checkboxName)
					.variable(checkboxVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.values(checkboxValues)
					.defaultValue("false")
					.build();
			checkboxField = fieldApi.save(checkboxField, user);
			com.dotmarketing.portlets.structure.model.Field oldCheckboxField = asOldField(checkboxField);
			fieldList.add(checkboxField);

			Field dateField = ImmutableDateField.builder()
					.name(dateName)
					.variable(dateVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			dateField = fieldApi.save(dateField, user);
			com.dotmarketing.portlets.structure.model.Field oldDateField = asOldField(dateField);
			fieldList.add(dateField);

			Field timeField = ImmutableTimeField.builder()
					.name(timeName)
					.variable(timeVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			timeField = fieldApi.save(timeField, user);
			com.dotmarketing.portlets.structure.model.Field oldTimeField = asOldField(timeField);
			fieldList.add(timeField);

			Field dateTimeField = ImmutableDateTimeField.builder()
					.name(dateTimeName)
					.variable(dateTimeVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			dateTimeField = fieldApi.save(dateTimeField, user);
			com.dotmarketing.portlets.structure.model.Field oldDateTimeField = asOldField(dateTimeField);
			fieldList.add(dateTimeField);

			Field radioField = ImmutableRadioField.builder()
					.name(radioName)
					.variable(radioVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.values(radioValues)
					.defaultValue("false")
					.build();
			radioField = fieldApi.save(radioField, user);
			com.dotmarketing.portlets.structure.model.Field oldRadioField = asOldField(radioField);
			fieldList.add(radioField);

			Field selectTextField = ImmutableSelectField.builder()
					.name(selectTextName)
					.variable(selectTextVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.values(selectTextValues)
					.build();
			selectTextField = fieldApi.save(selectTextField, user);
			com.dotmarketing.portlets.structure.model.Field oldSelectTextField = asOldField(selectTextField);
			fieldList.add(selectTextField);

			Field selectBooleanField = ImmutableSelectField.builder()
					.name(selectBooleanName)
					.variable(selectBooleanVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.values(selectBooleanValues)
					.defaultValue("false")
					.build();
			selectBooleanField = fieldApi.save(selectBooleanField, user);
			com.dotmarketing.portlets.structure.model.Field oldSelectBooleanField = asOldField(selectBooleanField);
			fieldList.add(selectBooleanField);

			Field selectDecimalField = ImmutableSelectField.builder()
					.name(selectDecimalName)
					.variable(selectDecimalVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.values(selectDecimalValues)
					.build();
			selectDecimalField = fieldApi.save(selectDecimalField, user);
			com.dotmarketing.portlets.structure.model.Field oldSelectDecimalField = asOldField(selectDecimalField);
			fieldList.add(selectDecimalField);

			Field selectWholeNumberField = ImmutableSelectField.builder()
					.name(selectWholeNumberName)
					.variable(selectWholeNumberVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.values(selectWholeNumberValues)
					.build();
			selectWholeNumberField = fieldApi.save(selectWholeNumberField, user);
			com.dotmarketing.portlets.structure.model.Field oldSelectWholeNumberField = asOldField(selectWholeNumberField);
			fieldList.add(selectWholeNumberField);

			Field multiSelectField = ImmutableMultiSelectField.builder()
					.name(multiSelectName)
					.variable(multiSelectVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.values(multiSelectValues)
					.build();
			multiSelectField = fieldApi.save(multiSelectField, user);
			com.dotmarketing.portlets.structure.model.Field oldMultiSelectField = asOldField(multiSelectField);
			fieldList.add(multiSelectField);

			Field textAreaField = ImmutableTextAreaField.builder()
					.name(textAreaName)
					.variable(textAreaVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			textAreaField = fieldApi.save(textAreaField, user);
			com.dotmarketing.portlets.structure.model.Field oldTextAreaField = asOldField(textAreaField);
			fieldList.add(textAreaField);

			Field textField = ImmutableTextField.builder()
					.name(textName)
					.variable(textVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			textField = fieldApi.save(textField, user);
			com.dotmarketing.portlets.structure.model.Field oldTextField = asOldField(textField);
			fieldList.add(textField);

			Field textDecimalField = ImmutableTextField.builder()
					.name(textDecimalName)
					.variable(textDecimalVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			textDecimalField = fieldApi.save(textDecimalField, user);
			com.dotmarketing.portlets.structure.model.Field oldTextDecimalField = asOldField(textDecimalField);
			fieldList.add(textDecimalField);

			Field textWholeNumberField = ImmutableTextField.builder()
					.name(textWholeNumberName)
					.variable(textWholeNumberVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			textWholeNumberField = fieldApi.save(textWholeNumberField, user);
			com.dotmarketing.portlets.structure.model.Field oldTextWholeNumberField = asOldField(textWholeNumberField);
			fieldList.add(textWholeNumberField);

			Field wysiwygField = ImmutableWysiwygField.builder()
					.name(wysiwygName)
					.variable(wysiwygVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			wysiwygField = fieldApi.save(wysiwygField, user);
			com.dotmarketing.portlets.structure.model.Field oldWysiwygField = asOldField(wysiwygField);
			fieldList.add(wysiwygField);

			Field fileField = ImmutableFileField.builder()
					.name(fileName)
					.variable(fileVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			fileField = fieldApi.save(fileField, user);
			com.dotmarketing.portlets.structure.model.Field oldFileField = asOldField(fileField);
			fieldList.add(fileField);

			Field imageField = ImmutableImageField.builder()
					.name(imageName)
					.variable(imageVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			imageField = fieldApi.save(imageField, user);
			com.dotmarketing.portlets.structure.model.Field oldImageField = asOldField(imageField);
			fieldList.add(imageField);

			Field tagField = ImmutableTagField.builder()
					.name(tagName)
					.variable(tagVariable)
					.contentTypeId(contentType.id())
					.required(Boolean.TRUE)
					.listed(Boolean.TRUE)
					.indexed(Boolean.TRUE)
					.searchable(Boolean.TRUE)
					.build();
			tagField = fieldApi.save(tagField, user);
			com.dotmarketing.portlets.structure.model.Field oldTagField = asOldField(tagField);
			fieldList.add(tagField);

			Field constantField = ImmutableConstantField.builder()
					.name(constantName)
					.variable(constantVariable)
					.contentTypeId(contentType.id())
					.build();
			constantField = fieldApi.save(constantField, user);
			com.dotmarketing.portlets.structure.model.Field oldConstantField = asOldField(constantField);
			fieldList.add(constantField);

			Field categoryField = ImmutableCategoryField.builder()
					.name(categoryName)
					.variable(categoryVariable)
					.contentTypeId(contentType.id())
					.build();
			categoryField = fieldApi.save(categoryField, user);
			com.dotmarketing.portlets.structure.model.Field oldCategoryField = asOldField(categoryField);
			fieldList.add(categoryField);

			Field lineDividerField = ImmutableLineDividerField.builder()
					.name(lineDividerName)
					.variable(lineDividerVariable)
					.contentTypeId(contentType.id())
					.values(lineDividerValue)
					.build();
			lineDividerField = fieldApi.save(lineDividerField, user);
			com.dotmarketing.portlets.structure.model.Field oldLineDividerField = asOldField(lineDividerField);
			fieldList.add(lineDividerField);

			Field tabDividerField = ImmutableTabDividerField.builder()
					.name(tabDividerName)
					.variable(tabDividerVariable)
					.contentTypeId(contentType.id())
					.values(tabDividerValue)
					.build();
			tabDividerField = fieldApi.save(tabDividerField, user);
			com.dotmarketing.portlets.structure.model.Field oldTabDividerField = asOldField(tabDividerField);
			fieldList.add(tabDividerField);

			Field permissionsTabField = ImmutablePermissionTabField.builder()
					.name(permissionsTabName)
					.variable(permissionsTabVariable)
					.contentTypeId(contentType.id())
					.values(permissionsTabValue)
					.build();
			permissionsTabField = fieldApi.save(permissionsTabField, user);
			com.dotmarketing.portlets.structure.model.Field oldPermissionsTabField = asOldField(permissionsTabField);
			fieldList.add(permissionsTabField);

			Field relationshipsTabField = ImmutableRelationshipsTabField.builder()
					.name(relationshipsTabName)
					.variable(relationshipsTabVariable)
					.contentTypeId(contentType.id())
					.values(relationshipsTabValue)
					.build();
			relationshipsTabField = fieldApi.save(relationshipsTabField, user);
			com.dotmarketing.portlets.structure.model.Field oldRelationshipsTabField = asOldField(relationshipsTabField);
			fieldList.add(relationshipsTabField);

			Field hiddenField = ImmutableHiddenField.builder()
					.name(hiddenName)
					.variable(hiddenVariable)
					.contentTypeId(contentType.id())
					.values(hiddenValue)
					.build();
			hiddenField = fieldApi.save(hiddenField, user);
			com.dotmarketing.portlets.structure.model.Field oldHiddenField = asOldField(hiddenField);
			fieldList.add(hiddenField);

			Field binaryField = ImmutableBinaryField.builder()
					.name(binaryName)
					.variable(binaryVariable)
					.contentTypeId(contentType.id())
					.searchable(Boolean.TRUE)
					.build();
			binaryField = fieldApi.save(binaryField, user);
			com.dotmarketing.portlets.structure.model.Field oldBinaryField = asOldField(binaryField);
			fieldList.add(binaryField);

			Field customField = ImmutableCustomField.builder()
					.name(customName)
					.variable(customVariable)
					.contentTypeId(contentType.id())
					.build();
			customField = fieldApi.save(customField, user);
			com.dotmarketing.portlets.structure.model.Field oldCustomField = asOldField(customField);
			fieldList.add(customField);

			Field siteOrFolderField = ImmutableHostFolderField.builder()
					.name(siteOrFolderName)
					.variable(siteOrFolderVariable)
					.contentTypeId(contentType.id())
					.indexed(Boolean.TRUE)
					.build();
			siteOrFolderField = fieldApi.save(siteOrFolderField, user);
			com.dotmarketing.portlets.structure.model.Field oldSiteOrFolderField = asOldField(siteOrFolderField);
			fieldList.add(siteOrFolderField);

			Field keyValueField = ImmutableKeyValueField.builder()
					.name(keyValueName)
					.variable(keyValueVariable)
					.contentTypeId(contentType.id())
					.build();
			keyValueField = fieldApi.save(keyValueField, user);
			com.dotmarketing.portlets.structure.model.Field oldKeyValueField = asOldField(keyValueField);
			fieldList.add(keyValueField);

			contentType = contentTypeApi.find(contentType.inode());
			List<Field> fields = contentType.fields();
			Assert.assertEquals(fieldList.size(), fields.size());

			Contentlet contentlet = new Contentlet();
            contentlet.setStructureInode(contentType.inode());
            contentlet.setHost(site.getIdentifier());
            contentlet.setLanguageId(langId);
			
			contentletAPI.setContentletProperty(contentlet, oldCheckboxField, checkboxValue);
			contentletAPI.setContentletProperty(contentlet, oldDateField, dateValue);
			contentletAPI.setContentletProperty(contentlet, oldTimeField, timeValue);
			contentletAPI.setContentletProperty(contentlet, oldDateTimeField, dateTimeValue);
			contentletAPI.setContentletProperty(contentlet, oldRadioField, radioValue);
			contentletAPI.setContentletProperty(contentlet, oldSelectTextField, selectTextValue);
			contentletAPI.setContentletProperty(contentlet, oldSelectBooleanField, selectBooleanValue);
			contentletAPI.setContentletProperty(contentlet, oldSelectDecimalField, selectDecimalValue);
			contentletAPI.setContentletProperty(contentlet, oldSelectWholeNumberField, selectWholeNumberValue);
			contentletAPI.setContentletProperty(contentlet, oldMultiSelectField, multiSelectValue);
			contentletAPI.setContentletProperty(contentlet, oldTextAreaField, textAreaValue);
			contentletAPI.setContentletProperty(contentlet, oldTextField, textValue);
			contentletAPI.setContentletProperty(contentlet, oldTextDecimalField, textDecimalValue);
			contentletAPI.setContentletProperty(contentlet, oldTextWholeNumberField, textWholeNumberValue);
			contentletAPI.setContentletProperty(contentlet, oldWysiwygField, wysiwygValue);
			contentletAPI.setContentletProperty(contentlet, oldFileField, fileValue);
			contentletAPI.setContentletProperty(contentlet, oldImageField, imageValue);
			contentletAPI.setContentletProperty(contentlet, oldTagField, tagValue);
			contentletAPI.setContentletProperty(contentlet, oldConstantField, constantValue);
			contentletAPI.setContentletProperty(contentlet, oldCategoryField, null);
			contentletAPI.setContentletProperty(contentlet, oldLineDividerField, lineDividerValue);
			contentletAPI.setContentletProperty(contentlet, oldTabDividerField, tabDividerValue);
			contentletAPI.setContentletProperty(contentlet, oldPermissionsTabField, permissionsTabValue);
			contentletAPI.setContentletProperty(contentlet, oldRelationshipsTabField, relationshipsTabValue);
			contentletAPI.setContentletProperty(contentlet, oldHiddenField, hiddenValue);
			contentletAPI.setContentletProperty(contentlet, oldBinaryField, binaryValue);
			contentletAPI.setContentletProperty(contentlet, oldCustomField, customValue);
			contentletAPI.setContentletProperty(contentlet, oldSiteOrFolderField, siteOrFolderValue);
			contentletAPI.setContentletProperty(contentlet, oldKeyValueField, StringUtils.EMPTY);

			// Save the content
			contentlet = contentletAPI.checkin(contentlet, user, Boolean.TRUE);
            
            // Execute jobs to delete fields
			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, checkboxField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, dateField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, timeField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, dateTimeField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, radioField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, selectTextField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, selectBooleanField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, selectDecimalField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, selectWholeNumberField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, multiSelectField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, textAreaField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, textField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, textDecimalField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, textWholeNumberField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, wysiwygField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, fileField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, imageField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, tagField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, constantField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, categoryField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, lineDividerField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, tabDividerField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, permissionsTabField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, relationshipsTabField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, hiddenField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, binaryField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, customField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, siteOrFolderField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

			TestJobExecutor.execute(deleteFieldJob,
					CollectionsUtils.map(DeleteFieldJob.JOB_DATA_MAP_CONTENT_TYPE, contentType,
							DeleteFieldJob.JOB_DATA_MAP_FIELD, keyValueField, DeleteFieldJob.JOB_DATA_MAP_USER, user));

            contentType = contentTypeApi.find(contentType.inode());
			fields = contentType.fields();
			Assert.assertEquals(0, fields.size());

			Contentlet contentletFromDB = CacheLocator.getContentletCache().get(contentlet.getInode());
			assertNull(contentletFromDB.get(checkboxVariable));
			assertNull(contentletFromDB.get(dateVariable));
			assertNull(contentletFromDB.get(timeVariable));
			assertNull(contentletFromDB.get(dateTimeVariable));
			assertNull(contentletFromDB.get(radioVariable));
			assertNull(contentletFromDB.get(selectTextVariable));
			assertNull(contentletFromDB.get(selectBooleanVariable));
			assertNull(contentletFromDB.get(selectDecimalVariable));
			assertNull(contentletFromDB.get(selectWholeNumberVariable));
			assertNull(contentletFromDB.get(multiSelectVariable));
			assertNull(contentletFromDB.get(textAreaVariable));
			assertNull(contentletFromDB.get(textVariable));
			assertNull(contentletFromDB.get(textDecimalVariable));
			assertNull(contentletFromDB.get(textWholeNumberVariable));
			assertNull(contentletFromDB.get(wysiwygVariable));
			assertNull(contentletFromDB.get(fileVariable));
			assertNull(contentletFromDB.get(imageVariable));
			assertNull(contentletFromDB.get(tagVariable));
			assertNull(contentletFromDB.get(constantVariable));
			assertNull(contentletFromDB.get(categoryVariable));
			assertNull(contentletFromDB.get(lineDividerVariable));
			assertNull(contentletFromDB.get(tabDividerVariable));
			assertNull(contentletFromDB.get(permissionsTabVariable));
			assertNull(contentletFromDB.get(relationshipsTabVariable));
			assertNull(contentletFromDB.get(hiddenVariable));
			assertNull(contentletFromDB.get(binaryVariable));
			assertNull(contentletFromDB.get(customVariable));
			assertNull(contentletFromDB.get(siteOrFolderVariable));
			assertNull(contentletFromDB.get(keyValueVariable));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utility method used to transform a new {@link Field} object into a legacy
	 * {@link com.dotmarketing.portlets.structure.model.Field}
	 * 
	 * @param newField
	 *            - The new {@link Field} object that will be transformed.
	 * @return The legacy
	 *         {@link com.dotmarketing.portlets.structure.model.Field}
	 *         representation.
	 */
	private com.dotmarketing.portlets.structure.model.Field asOldField(Field newField) {
		return new LegacyFieldTransformer(newField).asOldField();
	}

}

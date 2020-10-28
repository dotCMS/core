package com.dotcms.contenttype.model.type;

import java.util.ArrayList;
import java.util.List;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableKeyValueField;
import com.dotcms.contenttype.model.field.ImmutableTabDividerField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = ImmutableFileAssetContentType.class)
@JsonDeserialize(as = ImmutableFileAssetContentType.class)
@Gson.TypeAdapters
@Value.Immutable
public abstract class FileAssetContentType extends ContentType implements UrlMapable, Expireable{

	public static final String FILEASSET_SITE_OR_FOLDER_FIELD_VAR = "hostFolder";
	public static final String FILEASSET_FILEASSET_FIELD_VAR = "fileAsset";
	public static final String FILEASSET_TITLE_FIELD_VAR = "title";
	public static final String FILEASSET_FILE_NAME_FIELD_VAR = "fileName";
	public static final String FILEASSET_METADATA_FIELD_VAR = "metaData";
	public static final String FILEASSET_DESCRIPTION_FIELD_VAR = "description";
	public static final String FILEASSET_SHOW_ON_MENU_FIELD_VAR = "showOnMenu";
	public static final String FILEASSET_SORT_ORDER_FIELD_VAR = "sortOrder";

	
	private static final long serialVersionUID = 1L;

	@Override
	public  BaseContentType baseType() {
		return  BaseContentType.FILEASSET;
	}


	public  List<Field> requiredFields(){
		List<Field> fields = new ArrayList<Field>();
		
		
		fields.add(
				ImmutableHostFolderField.builder()
				.name("Site or Folder")
				.dataType(DataTypes.SYSTEM)
				.variable(FILEASSET_SITE_OR_FOLDER_FIELD_VAR)
				.sortOrder(fields.size())
				.required(true)
				.fixed(true)
				.searchable(true)
				.indexed(true)
				.build()
		);
		
		fields.add(
				ImmutableBinaryField.builder()
				.name("File Asset")
				.variable(FILEASSET_FILEASSET_FIELD_VAR)
				.sortOrder(fields.size())
				.fixed(true)
				.required(true)
				.readOnly(false)
				.searchable(true)
				.indexed(true)
				.build()
			);

		fields.add(
				ImmutableTextField.builder()
				.name("Title")
				.dataType(DataTypes.TEXT)
				.variable(FILEASSET_TITLE_FIELD_VAR)
				.fixed(true)
				.indexed(true)
				.searchable(true)
				.required(true)
				.sortOrder(fields.size())
				.build()
		);
		
		fields.add(
				ImmutableTextField.builder()
				.name("File Name")
				.dataType(DataTypes.TEXT)
				.variable(FILEASSET_FILE_NAME_FIELD_VAR)
				.fixed(true)
				.indexed(true)
				.searchable(true)
				.sortOrder(fields.size())
				.listed(true)
				.build()
		);
		
		
		fields.add(
				ImmutableTabDividerField.builder()
				.name("Metadata")
				.variable("metadataTab")
				.sortOrder(fields.size())
				.build()
		);
		fields.add(
				ImmutableKeyValueField.builder()
				.name("Metadata")
				.dataType(DataTypes.LONG_TEXT)
				.variable(FILEASSET_METADATA_FIELD_VAR)
				.fixed(true)
				.indexed(true)
				.readOnly(true)
				.searchable(true)
				.sortOrder(fields.size())
				.build()
		);
		fields.add(
				ImmutableCheckboxField.builder()
				.name("Show on Menu")
				.dataType(DataTypes.TEXT)
				.variable(FILEASSET_SHOW_ON_MENU_FIELD_VAR)
				.values("|true")
				.defaultValue("false")
				.fixed(true)
				.indexed(true)
				.sortOrder(fields.size())
				.build()
		);
		
		fields.add(
				ImmutableTextField.builder()
				.name("Sort Order")
				.dataType(DataTypes.INTEGER)
				.variable(FILEASSET_SORT_ORDER_FIELD_VAR)
				.fixed(true)
				.searchable(true)
				.defaultValue("0")
				.indexed(true)
				.sortOrder(fields.size())
				.build()
		);
		fields.add(
				ImmutableTextAreaField.builder()
				.name("Description")
				.dataType(DataTypes.LONG_TEXT)
				.variable(FILEASSET_DESCRIPTION_FIELD_VAR)
				.fixed(true)
				.indexed(true)
				.sortOrder(fields.size())
				.build()
		);
		return ImmutableList.copyOf(fields);
	}
	
	public abstract static class Builder implements ContentTypeBuilder {}

	
}

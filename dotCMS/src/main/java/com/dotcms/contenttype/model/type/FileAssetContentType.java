package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableKeyValueField;
import com.dotcms.contenttype.model.field.ImmutableTabDividerField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the basic definition and field layout of the File Asset Base Type. By default, all contents of type File
 * Asset will have the list of fields specified in this class.
 *
 * @author Will Ezell
 * @since Jun 29th, 2016
 */
@JsonSerialize(as = ImmutableFileAssetContentType.class)
@JsonDeserialize(as = ImmutableFileAssetContentType.class)
@Value.Immutable
public abstract class FileAssetContentType extends ContentType implements UrlMapable, Expireable{

	public static final String FILEASSET_SITE_OR_FOLDER_FIELD_VAR = "hostFolder";
	public static final String FILEASSET_FILEASSET_FIELD_VAR = "fileAsset";
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

	/**
	 * Returns the list of official or recommended fields for this Base Content Type. Some of them can be deleted by
	 * the User via the UI if necessary.
	 *
	 * @return The list of {@link Field} objects that make up the Base Content Type.
	 */
	public  List<Field> requiredFields(){
		final List<Field> fields = new ArrayList<>();
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
		return ImmutableList.copyOf(fields);
	}
	
	public abstract static class Builder implements ContentTypeBuilder {}

}

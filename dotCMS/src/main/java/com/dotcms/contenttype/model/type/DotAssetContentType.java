package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableKeyValueField;
import com.dotcms.contenttype.model.field.ImmutableTabDividerField;
import com.dotcms.contenttype.model.field.ImmutableTagField;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;

@JsonSerialize(as = ImmutableDotAssetContentType.class)
@JsonDeserialize(as = ImmutableDotAssetContentType.class)
@Gson.TypeAdapters
@Value.Immutable
/**
 * The dotAssets are distinguished from fileAssets in that the only required field in a dotAsset is the asset and they are only addressable via their ids,
 * e.g. /dA/2342354235235
 *
 * They not tree-able and do not have urls - can live on any host or on the system host and have five fields by default
 * @author jsanca
 */
public abstract class DotAssetContentType extends ContentType implements UrlMapable, Expireable {

	public static final String FILEASSET_FILEASSET_FIELD_VAR      = "asset";
	public static final String FILEASSET_MIME_TYPE_FIELD_VAR      = "mimeType";
	public static final String FILEASSET_SITE_OR_FOLDER_FIELD_VAR = "hostFolder";
	public static final String FILEASSET_METADATA_FIELD_VAR       = "metaData";
	public static final String FILEASSET_TAGS_FIELD_VAR           = "tags";


	@Override
	public  BaseContentType baseType() {
		return  BaseContentType.DOTASSET;
	}

	@Override
	public  List<Field> requiredFields(){
		final List<Field> fields = new ArrayList<>();

		fields.add(
				ImmutableHostFolderField.builder()
						.name("Site or Folder")
						.dataType(DataTypes.SYSTEM)
						.variable(FILEASSET_SITE_OR_FOLDER_FIELD_VAR)
						.sortOrder(fields.size())
						.required(false)
						.fixed(false)
						.searchable(true)
						.indexed(true)
						.build()
		);

		fields.add(
				ImmutableBinaryField.builder()
				.name("Binary Asset")
				.variable(FILEASSET_FILEASSET_FIELD_VAR)
				.sortOrder(fields.size())
				.fixed(true)
				.required(true)
				.readOnly(false)
				.build()
			);

		fields.add(
				ImmutableConstantField.builder()
				.name("mimeType")
				.variable(FILEASSET_MIME_TYPE_FIELD_VAR)
				.sortOrder(fields.size())
				.required(false)
				.fixed(false)
				.readOnly(true)
				.searchable(true)
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
						.required(false)
						.fixed(false)
						.indexed(true)
						.readOnly(true)
						.searchable(true)
						.sortOrder(fields.size())
						.build()
		);

		fields.add(
				ImmutableTagField.builder()
						.name("Tags")
						.variable(FILEASSET_TAGS_FIELD_VAR)
						.dataType(DataTypes.SYSTEM)
						.sortOrder(fields.size())
						.fixed(true)
						.indexed(true)
						.searchable(true)
						.build()
		);

		return ImmutableList.copyOf(fields);
	}
	
	public abstract static class Builder implements ContentTypeBuilder {}

	
}

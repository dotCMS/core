package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
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

	public static final String ASSET_FIELD_VAR 			= "asset";
	public static final String SITE_OR_FOLDER_FIELD_VAR = "hostFolder";
	public static final String TAGS_FIELD_VAR  			= "tags";


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
						.variable(SITE_OR_FOLDER_FIELD_VAR)
						.sortOrder(fields.size())
						.required(false)
						.fixed(false)
						.searchable(true)
						.indexed(true)
						.build()
		);

		fields.add(
				ImmutableBinaryField.builder()
				.name("Asset")
				.variable(ASSET_FIELD_VAR)
				.sortOrder(fields.size())
				.fixed(true)
				.required(true)
				.readOnly(false)
				.searchable(true)
				.indexed(true)
				.listed(true) // show the name of the file on the content search
				.build()
			);


		fields.add(
				ImmutableTagField.builder()
						.name("Tags")
						.variable(TAGS_FIELD_VAR)
						.dataType(DataTypes.SYSTEM)
						.sortOrder(fields.size())
						.required(false)
						.fixed(false)
						.indexed(true)
						.searchable(true)
						.build()
		);

		return ImmutableList.copyOf(fields);
	}
	
	public abstract static class Builder implements ContentTypeBuilder {}

	
}

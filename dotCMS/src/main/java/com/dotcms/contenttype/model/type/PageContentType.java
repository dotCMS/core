package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableTabDividerField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the basic definition and field layout of the Page Asset Base Type. By default, all contents of type Page
 * Asset will have the list of fields specified in this class.
 *
 * @author Will Ezell
 * @since Jun 29th, 2016
 */
@JsonSerialize(as = ImmutablePageContentType.class)
@JsonDeserialize(as = ImmutablePageContentType.class)
@Value.Immutable
public abstract class PageContentType extends ContentType implements Expireable{

	private static final long serialVersionUID = 1L;

	public static final String PAGE_URL_FIELD_VAR = "url";
	public static final String PAGE_HOST_FOLDER_FIELD_VAR = "hostFolder";
	public static final String PAGE_TEMPLATE_FIELD_VAR = "template";
	public static final String PAGE_SHOW_ON_MENU_FIELD_VAR = "showOnMenu";
	public static final String PAGE_SORT_ORDER_FIELD_VAR = "sortOrder";
	public static final String PAGE_CACHE_TTL_FIELD_VAR = "cachettl";
	public static final String PAGE_FRIENDLY_NAME_FIELD_VAR = "friendlyName";
	public static final String PAGE_REDIRECT_URL_FIELD_VAR = "redirecturl";
	public static final String PAGE_HTTP_REQUIRED_FIELD_VAR = "httpsreq";
	public static final String PAGE_SEO_DESCRIPTION_FIELD_VAR = "seodescription";
	public static final String PAGE_SEO_KEYWORDS_FIELD_VAR = "seokeywords";
	public static final String PAGE_PAGE_METADATA_FIELD_VAR = "pagemetadata";

	@Override
	public  BaseContentType baseType() {
		return  BaseContentType.HTMLPAGE;
	}
	public abstract static class Builder implements ContentTypeBuilder {}

	/**
	 * Returns the list of official or recommended fields for this Base Content Type. Some of them can be deleted by
	 * the User via the UI if necessary.
	 *
	 * @return The list of {@link Field} objects that make up the Base Content Type.
	 */
	public List<Field> requiredFields(){
		int i=0;

		final List<Field> fields = new ArrayList<>();
		fields.add(
				ImmutableCustomField.builder()
				.name("Title")
				.dataType(DataTypes.TEXT)
				.variable("title")
				.fixed(true)
				.indexed(true)
				.values("$velutil.mergeTemplate('/static/htmlpage_assets/title_custom_field.vtl')")
				.required(true)
				.sortOrder(i++)
				.listed(true)
				.build()
		);
		fields.add(
				ImmutableTextField.builder()
				.name("Url")
				.dataType(DataTypes.TEXT)
				.variable(PAGE_URL_FIELD_VAR)
				.indexed(true)
				.searchable(true)
				.required(true)
				.sortOrder(i++)
				.fixed(true)
				.listed(true)
				.build()
		);
		fields.add(
				ImmutableHostFolderField.builder()
				.name("Site or Folder")
				.dataType(DataTypes.SYSTEM)
				.variable(PAGE_HOST_FOLDER_FIELD_VAR)
				.sortOrder(i++)
				.required(true)
				.fixed(true)
				.indexed(true)
				.build()
		);
		fields.add(
				ImmutableCustomField.builder()
				.name("Template")
				.dataType(DataTypes.TEXT)
				.variable(PAGE_TEMPLATE_FIELD_VAR)
				.indexed(true)
				.required(true)
				.fixed(true)
				.values("$velutil.mergeTemplate('/static/htmlpage_assets/template_custom_field.vtl')")
				.sortOrder(i++)
				.searchable(true)
				.build()
		);
		fields.add(
				ImmutableCheckboxField.builder()
				.name("Show on Menu")
				.dataType(DataTypes.TEXT)
				.variable(PAGE_SHOW_ON_MENU_FIELD_VAR)
				.values("|true")
				.defaultValue("false")
				.forceIncludeInApi(Boolean.TRUE)
				.indexed(true)
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableTextField.builder()
				.name("Sort Order")
				.dataType(DataTypes.INTEGER)
				.variable(PAGE_SORT_ORDER_FIELD_VAR)
				.required(true)
				.forceIncludeInApi(Boolean.TRUE)
				.searchable(true)
				.defaultValue("0")
				.indexed(true)
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableCustomField.builder()
				.name("Cache TTL (seconds)")
				.dataType(DataTypes.TEXT)
				.variable(PAGE_CACHE_TTL_FIELD_VAR)
				.indexed(true)
				.forceIncludeInApi(Boolean.TRUE)
				.required(true)
				.values("$velutil.mergeTemplate('/static/htmlpage_assets/cachettl_custom_field.vtl')")
				.sortOrder(i++)
				.listed(true)
				.build()
		);
		fields.add(
				ImmutableTextField.builder()
				.name("Friendly Name")
				.dataType(DataTypes.TEXT)
				.variable(PAGE_FRIENDLY_NAME_FIELD_VAR)
				.indexed(true)
				.forceIncludeInApi(Boolean.TRUE)
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableTabDividerField.builder()
				.name("Advanced Properties")
				.variable("advancedtab")
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableCustomField.builder()
				.name("Redirect URL")
				.variable(PAGE_REDIRECT_URL_FIELD_VAR)
				.values("$velutil.mergeTemplate('/static/htmlpage_assets/redirect_custom_field.vtl')")
				.dataType(DataTypes.TEXT)
				.forceIncludeInApi(Boolean.TRUE)
				.listed(true)
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableCheckboxField.builder()
				.name("HTTPS Required")
				.dataType(DataTypes.TEXT)
				.variable(PAGE_HTTP_REQUIRED_FIELD_VAR)
				.forceIncludeInApi(Boolean.TRUE)
				.values("|true")
				.defaultValue("false")
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableTextAreaField.builder()
				.name("SEO Description")
				.dataType(DataTypes.LONG_TEXT)
				.variable(PAGE_SEO_DESCRIPTION_FIELD_VAR)
				.indexed(true)
				.forceIncludeInApi(Boolean.TRUE)
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableTextAreaField.builder()
				.name("SEO Keywords")
				.dataType(DataTypes.LONG_TEXT)
				.variable(PAGE_SEO_KEYWORDS_FIELD_VAR)
				.indexed(true)
				.forceIncludeInApi(Boolean.TRUE)
				.sortOrder(i++)
				.build()
		);
		fields.add(
				ImmutableTextAreaField.builder()
				.name("Page Metadata")
				.dataType(DataTypes.LONG_TEXT)
				.variable(PAGE_PAGE_METADATA_FIELD_VAR)
				.forceIncludeInApi(Boolean.TRUE)
				.sortOrder(i++)
				.build()
		);
		return ImmutableList.copyOf(fields);
	}

}

package com.dotcms.contenttype.model.type;

import java.util.List;

import com.dotmarketing.util.Config;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableSelectField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Provides the basic outline of a Vanity URL Content Type in dotCMS. Vanity
 * URLs are alternate reference paths to internal or external URL's. Vanity URLs
 * are most commonly used to give visitors to the website a more user-friendly
 * or memorable way of reaching an HTML page or File, that might actually live
 * “buried” in a much deeper path.
 *
 * @author Jose Castro
 * @version 4.2.0
 * @since May 25, 2017
 */
@JsonSerialize(as = ImmutableVanityUrlContentType.class)
@JsonDeserialize(as = ImmutableVanityUrlContentType.class)
@Gson.TypeAdapters
@Value.Immutable
public abstract class VanityUrlContentType extends ContentType implements Expireable,
		Multilinguable {

	private static final long serialVersionUID = 1L;

	private static final String TITLE_FIELD_NAME = "Title";
	public static final String TITLE_FIELD_VAR = "title";
	private static final String SITE_FIELD_NAME = "Site";
	public static final String SITE_FIELD_VAR = "site";
	private static final String URI_FIELD_NAME = "URI";
	public static final String URI_FIELD_VAR = "uri";
	private static final String FORWARD_TO_FIELD_NAME = "Forward To";
	public static final String FORWARD_TO_FIELD_VAR = "forwardTo";
	private static final String ACTION_FIELD_NAME = "Action";
	public static final String ACTION_FIELD_VAR = "action";
	private static final String ORDER_FIELD_NAME = "Order";
	public static final String ORDER_FIELD_VAR = "order";

	public abstract static class Builder implements ContentTypeBuilder {

	}

	@Override
	public BaseContentType baseType() {
		return BaseContentType.VANITY_URL;
	}

	@Override
	public List<Field> requiredFields() {
		int order = 0;
		Field titleField = ImmutableTextField.builder().name(TITLE_FIELD_NAME)
				.variable(TITLE_FIELD_VAR)
				.dataType(DataTypes.TEXT).required(Boolean.TRUE).listed(Boolean.TRUE)
				.indexed(Boolean.TRUE)
				.sortOrder(order++).fixed(Boolean.TRUE).searchable(Boolean.TRUE).build();
		Field siteField = ImmutableCustomField.builder().name(SITE_FIELD_NAME)
				.variable(SITE_FIELD_VAR)
				.dataType(DataTypes.TEXT).fixed(Boolean.TRUE).indexed(Boolean.TRUE)
				.values("$velutil.mergeTemplate('/static/content/site_selector_field_render.vtl')")
				.required(Boolean.TRUE).sortOrder(order++).listed(Boolean.FALSE).build();
		Field uriField = ImmutableTextField.builder().name(URI_FIELD_NAME).variable(URI_FIELD_VAR)
				.dataType(DataTypes.TEXT).indexed(Boolean.TRUE).searchable(Boolean.TRUE)
				.required(Boolean.TRUE)
				.sortOrder(order++).fixed(Boolean.TRUE).listed(Boolean.TRUE).build();
		Field actionField = ImmutableSelectField.builder().name(ACTION_FIELD_NAME)
				.variable(ACTION_FIELD_VAR)
				.required(Boolean.TRUE).fixed(Boolean.TRUE).indexed(Boolean.TRUE)
				.searchable(Boolean.TRUE).sortOrder(order++)
				.dataType(DataTypes.INTEGER)
				.values("200 - Forward|200\r\n301 - Permanent Redirect|301\r\n302 - Temporary Redirect|302\r\n401 - Auth Required|401\r\n403 - Auth Failed|403\r\n404 - Missing|404\r\n500 - Error|500")
				.build();
		Field forwardToField = ImmutableCustomField.builder().name(FORWARD_TO_FIELD_NAME)
				.variable(FORWARD_TO_FIELD_VAR)
				.dataType(DataTypes.TEXT).fixed(Boolean.TRUE).indexed(Boolean.TRUE)
				.values("$velutil.mergeTemplate('/static/content/file_browser_field_render.vtl')")
				.required(Boolean.TRUE).sortOrder(order++).listed(Boolean.TRUE).build();
		Field orderField = ImmutableTextField.builder().name(ORDER_FIELD_NAME)
				.variable(ORDER_FIELD_VAR)
				.dataType(DataTypes.INTEGER).required(Boolean.TRUE).fixed(Boolean.TRUE)
				.indexed(Boolean.TRUE)
				.sortOrder(order++).searchable(Boolean.TRUE).defaultValue("0").build();
		return ImmutableList
				.of(titleField, siteField, uriField, forwardToField, actionField, orderField);
	}

	@Override
	public boolean fallback() {
		return Config.getBooleanProperty("DEFAULT_VANITY_URL_TO_DEFAULT_LANGUAGE", false);
	}

}
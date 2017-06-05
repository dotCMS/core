package com.dotcms.contenttype.model.type;

import java.util.List;

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
 *
 */
@JsonSerialize(as = ImmutableVanityUrlContentType.class)
@JsonDeserialize(as = ImmutableVanityUrlContentType.class)
@Gson.TypeAdapters
@Value.Immutable
public abstract class VanityUrlContentType extends ContentType {

	private static final long serialVersionUID = 1L;

	private final String TITLE_FIELD_NAME = "Title";
	private final String TITLE_FIELD_VAR = "title";
	private final String SITE_FIELD_NAME = "Site";
	private final String SITE_FIELD_VAR = "site";

	public abstract static class Builder implements ContentTypeBuilder {
	}

	@Override
	public BaseContentType baseType() {
		return BaseContentType.VANITY_URL;
	}

	@Override
	public List<Field> requiredFields() {
		int order = 1;
		
		Field titleField = ImmutableTextField.builder().name(TITLE_FIELD_NAME).dataType(DataTypes.TEXT)
				.variable(TITLE_FIELD_VAR).required(Boolean.TRUE).listed(Boolean.TRUE).indexed(Boolean.TRUE).sortOrder(order).fixed(true)
				.searchable(Boolean.TRUE).build();
		/*Field siteField = ImmutableCustomField.builder().name(SITE_FIELD_NAME).dataType(DataTypes.TEXT).variable(SITE_FIELD_VAR)
				.fixed(true).indexed(true)
				.values("$velutil.mergeTemplate('/static/content/site_selector_field_render.vtl')").required(true)
				.sortOrder(order++).listed(true).build();*/
		Field uriField = ImmutableTextField.builder().name("Uri").dataType(DataTypes.TEXT).variable("uri").indexed(true)
				.searchable(Boolean.TRUE).required(Boolean.TRUE).sortOrder(order++).fixed(Boolean.TRUE)
				.listed(Boolean.TRUE).build();
		Field forwardToField = ImmutableTextField.builder().name("Forward To").dataType(DataTypes.TEXT)
				.variable("fowardTo").indexed(Boolean.TRUE).searchable(Boolean.TRUE).required(Boolean.TRUE).sortOrder(order++)
				.fixed(Boolean.TRUE).listed(Boolean.TRUE).build();
		Field responseCodesField = ImmutableSelectField.builder().name("Response Code").variable("responseCode")
				.required(Boolean.TRUE).listed(Boolean.TRUE).indexed(Boolean.TRUE).searchable(Boolean.TRUE).fixed(Boolean.TRUE)
				.values("200 OK|200\r\n301 Moved Permanently|301\r\n302 Found|302\r\n401 Unauthorized|401\r\n402 Payment Required|402\r\n403 Forbidden|403\r\n404 Not Found|404\r\n500 Internal Server Error|500")
				.build();
		Field actionField = ImmutableSelectField.builder().name("Action").variable("action").required(Boolean.TRUE).fixed(Boolean.TRUE)
				.listed(Boolean.TRUE).indexed(Boolean.TRUE).searchable(Boolean.TRUE)
				.values("Redirect|redirect\r\nForward|forward\r\nDie|die").build();
		Field orderField = ImmutableTextField.builder().name("Order").variable("order").dataType(DataTypes.INTEGER)
				.required(Boolean.TRUE).fixed(Boolean.TRUE).listed(Boolean.TRUE).indexed(Boolean.TRUE)
				.searchable(Boolean.TRUE).build();

		//return ImmutableList.of(titleField, siteField, uriField, forwardToField, responseCodesField, actionField, orderField);
		return ImmutableList.of(titleField, uriField, forwardToField, responseCodesField, actionField, orderField);
	}

}

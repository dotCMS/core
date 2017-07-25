package com.dotcms.contenttype.model.type;

import java.util.List;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 16, 2017
 *
 */
@JsonSerialize(as = ImmutableKeyValueContentType.class)
@JsonDeserialize(as = ImmutableKeyValueContentType.class)
@Gson.TypeAdapters
@Value.Immutable
public abstract class KeyValueContentType extends ContentType {

	private static final long serialVersionUID = 1L;

	public static final String KEY_VALUE_KEY_FIELD_NAME = "Key";
	public static final String KEY_VALUE_KEY_FIELD_VAR = "key";
	public static final String KEY_VALUE_VALUE_FIELD_NAME = "Value";
	public static final String KEY_VALUE_VALUE_FIELD_VAR = "value";

	public abstract static class Builder implements ContentTypeBuilder {
	}

	@Override
	public BaseContentType baseType() {
		return BaseContentType.KEY_VALUE;
	}

	@Value.Default
	public boolean multilingualable() {
		return false;
	}

	@Override
	public List<Field> requiredFields() {
		int order = 1;
		Field keyField = ImmutableTextField.builder().name(KEY_VALUE_KEY_FIELD_NAME).dataType(DataTypes.TEXT)
				.variable(KEY_VALUE_KEY_FIELD_VAR).required(Boolean.TRUE).listed(Boolean.TRUE).indexed(Boolean.TRUE)
				.sortOrder(order++).fixed(Boolean.TRUE).searchable(Boolean.TRUE).unique(Boolean.TRUE).build();
		Field valueField = ImmutableTextAreaField.builder().name(KEY_VALUE_VALUE_FIELD_NAME)
				.dataType(DataTypes.LONG_TEXT).variable(KEY_VALUE_VALUE_FIELD_VAR).required(Boolean.TRUE)
				.listed(Boolean.TRUE).fixed(Boolean.TRUE).searchable(Boolean.TRUE).sortOrder(order++).build();
		return ImmutableList.of(keyField, valueField);
	}

}

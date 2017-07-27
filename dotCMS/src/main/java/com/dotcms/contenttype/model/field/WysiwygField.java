package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableWysiwygField.class)
@JsonDeserialize(as = ImmutableWysiwygField.class)
@Value.Immutable
public abstract class WysiwygField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Class type() {
		return WysiwygField.class;
	}

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.LONG_TEXT);
	}

	@Value.Default
	@Override
	public DataTypes dataType() {
		return DataTypes.LONG_TEXT;
	};

	public abstract static class Builder implements FieldBuilder {
	}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.LABEL, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.DISPLAY_TYPE, ContentTypeFieldProperties.VALIDATION,
				ContentTypeFieldProperties.DEFAULT_TEXT, ContentTypeFieldProperties.HINT,
				ContentTypeFieldProperties.USER_SEARCHABLE, ContentTypeFieldProperties.INDEXED, ContentTypeFieldProperties.DATA_TYPE,
				ContentTypeFieldProperties.RADIO_BLOCK_TEXT);
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "WYSIWYG";
	}
}

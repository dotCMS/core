package com.dotcms.contenttype.model.field;

import com.dotcms.content.business.ContentletJsonAPIImpl;
import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.LongTextType;
import com.dotmarketing.util.Logger;
import java.util.Collection;
import java.util.List;

import java.util.Optional;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableTextAreaField.class)
@JsonDeserialize(as = ImmutableTextAreaField.class)
@Value.Immutable
public abstract class TextAreaField extends Field {


	private static final long serialVersionUID = 1L;
	
	@Override
	public  Class type() {
		return  TextAreaField.class;
	}

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.LONG_TEXT, DataTypes.SYSTEM, DataTypes.TEXT);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.LONG_TEXT;
	};
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
				ContentTypeFieldProperties.REGEX_CHECK, ContentTypeFieldProperties.DEFAULT_VALUE, ContentTypeFieldProperties.HINT,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED);
	}

	@Override
	public Optional<FieldValue<?>> fieldValue(Object value) {
			if (value instanceof String) {
				return Optional.of(LongTextType.of((String) value));
			}
		    return Optional.empty();
	}


}

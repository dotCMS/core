package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.text.FloatTextFieldType;
import com.dotcms.content.model.type.text.LongTextFieldType;
import com.dotcms.content.model.type.text.TextFieldType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableTextField.class)
@JsonDeserialize(as = ImmutableTextField.class)
@Value.Immutable
public abstract class TextField extends Field {

	private static final long serialVersionUID = 1L;

	@Override
	public  Class type() {
		return  TextField.class;
	}

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.TEXT,DataTypes.LONG_TEXT, DataTypes.FLOAT, DataTypes.INTEGER);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.TEXT;
	};
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.DATA_TYPE,
				ContentTypeFieldProperties.REGEX_CHECK, ContentTypeFieldProperties.DEFAULT_VALUE,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED, ContentTypeFieldProperties.LISTED,
				ContentTypeFieldProperties.UNIQUE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<FieldValueBuilder> fieldValue(final Object value) {
		if (null != value) {
			if (value instanceof String) {
				return Optional.of(TextFieldType.builder().value((String) value));
			}
			if (value instanceof Float) {
				return Optional.of(FloatTextFieldType.builder().value((Float) value));
			}
			if (value instanceof Long) {
				return Optional.of(LongTextFieldType.builder().value((Long) value));
			}
			if (value instanceof Integer) {
				return Optional.of(LongTextFieldType.builder().value(((Integer) value).longValue()));
			}
		} else {
			final DataTypes dataType = dataType();
			switch (dataType) {
				case FLOAT:
					return Optional.of(FloatTextFieldType.builder().value(0F));
				case INTEGER:
					return Optional.of(LongTextFieldType.builder().value(0L));
			}
		}
		return Optional.empty();
	}
}

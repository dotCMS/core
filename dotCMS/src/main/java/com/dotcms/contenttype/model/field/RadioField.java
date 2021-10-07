package com.dotcms.contenttype.model.field;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.BoolType;
import com.dotcms.content.model.type.FloatType;
import com.dotcms.content.model.type.LongType;
import com.dotcms.content.model.type.TextType;
import java.util.Collection;
import java.util.List;

import java.util.Optional;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableRadioField.class)
@JsonDeserialize(as = ImmutableRadioField.class)
@Value.Immutable
public abstract class RadioField extends SelectableValuesField {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  RadioField.class;
	}

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.TEXT, DataTypes.BOOL, DataTypes.FLOAT,DataTypes.INTEGER);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.TEXT;
	};
	

	
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.VALUES, ContentTypeFieldProperties.DEFAULT_VALUE,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.LISTED, ContentTypeFieldProperties.DATA_TYPE);
	}

	@Override
	public Optional<FieldValue<?>> fieldValue(final Object value){

		if (value instanceof String) {
			return Optional.of(TextType.of((String) value));
		}

		if (value instanceof Boolean) {
			return Optional.of(BoolType.of((Boolean) value));
		}

		if (value instanceof Long) {
			return Optional.of(LongType.of((Long) value));
		}

		if (value instanceof Float) {
			return Optional.of(FloatType.of((Float) value));
		}

		return Optional.empty();

	}

}

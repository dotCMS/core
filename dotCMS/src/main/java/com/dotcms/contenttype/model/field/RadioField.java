package com.dotcms.contenttype.model.field;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.radio.BoolRadioFieldType;
import com.dotcms.content.model.type.radio.FloatRadioFieldType;
import com.dotcms.content.model.type.radio.IntegerRadioFieldType;
import com.dotcms.content.model.type.radio.LongRadioFieldType;
import com.dotcms.content.model.type.radio.RadioFieldType;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<FieldValue<?>> fieldValue(final Object value){

		if (value instanceof String) {
			return Optional.of(RadioFieldType.of((String) value));
		}

		if (value instanceof Boolean) {
			return Optional.of(BoolRadioFieldType.of((Boolean) value));
		}

		if (value instanceof Long) {
			return Optional.of(LongRadioFieldType.of((Long) value));
		}

		if (value instanceof Integer) {
			return Optional.of(IntegerRadioFieldType.of((Integer) value));
		}

		if (value instanceof Float) {
			return Optional.of(FloatRadioFieldType.of((Float) value));
		}

		return Optional.empty();

	}

}

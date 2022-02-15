package com.dotcms.contenttype.model.field;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.select.BoolSelectFieldType;
import com.dotcms.content.model.type.select.FloatSelectFieldType;
import com.dotcms.content.model.type.select.LongSelectFieldType;
import com.dotcms.content.model.type.select.SelectFieldType;
import java.util.Collection;
import java.util.List;

import java.util.Optional;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableSelectField.class)
@JsonDeserialize(as = ImmutableSelectField.class)
@Value.Immutable
public abstract class SelectField extends SelectableValuesField {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  SelectField.class;
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
		return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
				ContentTypeFieldProperties.VALUES, ContentTypeFieldProperties.DEFAULT_VALUE,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.LISTED, ContentTypeFieldProperties.DATA_TYPE,
				ContentTypeFieldProperties.UNIQUE);
	}

	/**
	 * {@inheritDoc}
     * @return
     */
	@Override
	public Optional<FieldValueBuilder> fieldValue(final Object value) {
		if (value != null) {
			if (value instanceof String) {
				return Optional.of(SelectFieldType.builder().value((String) value));
			}
			if (value instanceof Boolean) {
				return Optional.of(BoolSelectFieldType.builder().value((Boolean) value));
			}
			if (value instanceof Float) {
				return Optional.of(FloatSelectFieldType.builder().value((Float) value));
			}
			if (value instanceof Long) {
				return Optional.of(LongSelectFieldType.builder().value((Long) value));
			}
			if (value instanceof Integer) {
				return Optional.of(LongSelectFieldType.builder().value(((Integer) value).longValue()));
			}
		} else {
			final DataTypes dataType = dataType();
			switch (dataType) {
				case BOOL:
					return Optional.of(BoolSelectFieldType.builder().value(false));
				case FLOAT:
					return Optional.of(FloatSelectFieldType.builder().value(0F));
				case INTEGER:
					return Optional.of(LongSelectFieldType.builder().value(0L));
			}
		}
		return Optional.empty();
	}

}

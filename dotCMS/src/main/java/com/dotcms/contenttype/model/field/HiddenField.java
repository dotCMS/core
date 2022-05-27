package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.hidden.BoolHiddenFieldType;
import com.dotcms.content.model.type.hidden.DateHiddenFieldType;
import com.dotcms.content.model.type.hidden.FloatHiddenFieldType;
import com.dotcms.content.model.type.hidden.HiddenFieldType;
import com.dotcms.content.model.type.hidden.LongHiddenFieldType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableHiddenField.class)
@JsonDeserialize(as = ImmutableHiddenField.class)
@Value.Immutable
public abstract class HiddenField extends Field {

	private static final long serialVersionUID = 1L;
	

	@Override
	public Class type() {
		return HiddenField.class;
	}
	
	
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SYSTEM;
	};
	
	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.SYSTEM, DataTypes.BOOL, DataTypes.DATE, DataTypes.FLOAT, DataTypes.TEXT,DataTypes.LONG_TEXT,DataTypes.INTEGER);
	}
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.VALUES);
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Hidden-Field";
	}


	/**
	 * {@inheritDoc}
     * @return
     */
	@Override
	public Optional<FieldValueBuilder> fieldValue(final Object value){
		if (null != value) {
			if (value instanceof String) {
				return Optional.of(HiddenFieldType.builder().value(((String) value)));
			}
			if (value instanceof Boolean) {
				return Optional.of(BoolHiddenFieldType.builder().value((Boolean) value));
			}
			if (value instanceof Date) {
				return Optional.of(DateHiddenFieldType.builder().value((Date) value));
			}
			if (value instanceof Float) {
				return Optional.of(FloatHiddenFieldType.builder().value((Float) value));
			}
			if (value instanceof Long) {
				return Optional.of(LongHiddenFieldType.builder().value((Long) value));
			}
			if (value instanceof Integer) {
				return Optional.of(LongHiddenFieldType.builder().value(((Integer)value).longValue()));
			}
		} else {
			final DataTypes dataType = dataType();
			switch (dataType) {
				case BOOL:
					return Optional.of(BoolHiddenFieldType.builder().value(false));
				case FLOAT:
					return Optional.of(FloatHiddenFieldType.builder().value(0F));
				case INTEGER:
					return Optional.of(LongHiddenFieldType.builder().value(0L));
			}
		}
		return Optional.empty();
	}

}

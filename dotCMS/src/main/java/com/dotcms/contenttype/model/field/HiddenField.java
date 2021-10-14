package com.dotcms.contenttype.model.field;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.hidden.BoolHiddenFieldType;
import com.dotcms.content.model.type.hidden.DateHiddenFieldType;
import com.dotcms.content.model.type.hidden.FloatHiddenFieldType;
import com.dotcms.content.model.type.hidden.HiddenFieldType;
import com.dotcms.content.model.type.ImageType;
import com.dotcms.content.model.type.hidden.IntegerHiddenFieldType;
import com.dotcms.content.model.type.hidden.LongHiddenFieldType;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import java.util.Optional;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

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
	 */
	@Override
	public Optional<FieldValue<?>> fieldValue(final Object value){
		if (value instanceof String) {
			return Optional.of(HiddenFieldType.of((String) value));
		}
		if (value instanceof Boolean) {
			return Optional.of(BoolHiddenFieldType.of((Boolean) value));
		}
		if (value instanceof Date) {
			return Optional.of(DateHiddenFieldType.of((Date) value));
		}
		if (value instanceof Float) {
			return Optional.of(FloatHiddenFieldType.of((Float) value));
		}
		if (value instanceof Integer) {
			return Optional.of(IntegerHiddenFieldType.of((Integer) value));
		}
		if (value instanceof Long) {
			return Optional.of(LongHiddenFieldType.of((Long) value));
		}
		return Optional.empty();
	}

}

package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.BoolType;
import com.dotcms.content.model.type.LongType;
import com.dotcms.content.model.type.TextType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableCheckboxField.class)
@JsonDeserialize(as = ImmutableCheckboxField.class)
@Value.Immutable
public abstract class CheckboxField extends SelectableValuesField{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  CheckboxField.class;
	}
	
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.TEXT;
	};

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.TEXT,DataTypes.LONG_TEXT);
	}
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.VALUES, ContentTypeFieldProperties.DEFAULT_VALUE,
				ContentTypeFieldProperties.HINT);
	}

	@Override
	public Optional<FieldValue<?>> fieldValue(final Object value){
		if (value instanceof String) {
			return Optional.of(TextType.of((String) value));
		}
		return Optional.empty();
	}
}

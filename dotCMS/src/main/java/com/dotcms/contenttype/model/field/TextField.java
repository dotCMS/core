package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

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
		return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
				ContentTypeFieldProperties.DEFAULT_VALUE, ContentTypeFieldProperties.HINT,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.DATA_TYPE);
	}
}

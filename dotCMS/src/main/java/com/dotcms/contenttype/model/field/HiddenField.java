package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
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
}

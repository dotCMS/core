package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableConstantField.class)
@JsonDeserialize(as = ImmutableConstantField.class)
@Value.Immutable
public abstract class ConstantField extends Field {


	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  ConstantField.class;
	}
	
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SYSTEM;
	};

	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.SYSTEM);
	}
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Constant-Field";
	}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.LABEL, ContentTypeFieldProperties.VALUE,
				ContentTypeFieldProperties.TEXT_AREA_VALUES, ContentTypeFieldProperties.DISPLAY_TYPE,
				ContentTypeFieldProperties.HINT);
	}
}

package com.dotcms.contenttype.model.field;

import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = ImmutableCustomField.class)
@JsonDeserialize(as = ImmutableCustomField.class)
@Value.Immutable
public abstract class CustomField extends Field {


	private static final long serialVersionUID = 1L;

	@Override
	public Class type() {
		return CustomField.class;
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.LONG_TEXT;
	};

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.LONG_TEXT,DataTypes.TEXT);
	}

	public abstract static class Builder implements FieldBuilder {
	}
}

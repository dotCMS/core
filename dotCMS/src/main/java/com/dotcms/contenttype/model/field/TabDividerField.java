package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
@JsonSerialize
@JsonDeserialize
@Value.Immutable
public abstract class TabDividerField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  TabDividerField.class;
	}
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.SYSTEM);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SYSTEM;
	};
	public abstract static class Builder implements FieldBuilder {}
}

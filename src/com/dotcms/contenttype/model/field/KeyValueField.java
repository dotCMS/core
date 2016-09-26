package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class KeyValueField extends Field {


	@Value.Default
	@Override
	public boolean indexed() {
		return true;
	};
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  KeyValueField.class;
	}
	
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.LONG_TEXT;
	};
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.LONG_TEXT);
	}
	public abstract static class Builder implements FieldBuilder {}
}

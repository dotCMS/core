package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class DateTimeField extends Field {


	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  DateTimeField.class;
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.DATE;
	};
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.DATE);
	}
	public abstract static class Builder implements FieldBuilder {}
}

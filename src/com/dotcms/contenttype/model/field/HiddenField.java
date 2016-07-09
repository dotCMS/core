package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;


@Value.Immutable
public abstract class HiddenField extends Field {

	private static final long serialVersionUID = 1L;

	
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.SYSTEM, DataTypes.CONSTANT);
	}
	public abstract static class Builder implements FieldBuilder {}
}

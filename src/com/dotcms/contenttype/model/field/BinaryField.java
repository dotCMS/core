package com.dotcms.contenttype.model.field;

import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class BinaryField extends Field {

	private static final long serialVersionUID = 1L;
	

	@Override
	public Class type() {
		return BinaryField.class;
	}
	
	@Override
	public List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.BINARY);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.BINARY;
	}

	
	public abstract static class Builder implements FieldBuilder {}
}

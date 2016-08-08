package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;

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
		return DataTypes.CONSTANT;
	};

	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.CONSTANT);
	}
	public abstract static class Builder implements FieldBuilder {}
	
	
	
	@Value.Check
	protected void check() {
		
		//Preconditions.checkArgument(dataType() == DataTypes.CONSTANT,"field must be a constant:" + this);
		//Preconditions.checkArgument(!indexed(),"field cannot be indexed:" + this);

	}
}

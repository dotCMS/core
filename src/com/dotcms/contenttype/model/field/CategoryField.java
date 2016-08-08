package com.dotcms.contenttype.model.field;

import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;


@Value.Immutable
public abstract class CategoryField extends Field {

	private static final long serialVersionUID = 1L;

	@Override
	public Class type() {
		return CategoryField.class;
	}
	
	@Value.Check
	protected void check() {
		Preconditions.checkArgument(indexed(),"Category Fields must be indexed:" + this);

		
		
	}
	@Value.Default
	@Override
	public boolean indexed() {
		return true;
	};
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SYSTEM;
	}
	@Override
	public final List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.SYSTEM);
	}
	public abstract static class Builder implements FieldBuilder {}
	
}

package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class RelationshipsTabField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  RelationshipsTabField.class;
	}
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.SECTION_DIVIDER);
	}
	
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SECTION_DIVIDER;
	};

	@Override
	public boolean onePerContentType() {
		return true;
	};
	
	public abstract static class Builder implements FieldBuilder {}
}

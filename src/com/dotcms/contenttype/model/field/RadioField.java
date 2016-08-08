package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.contenttype.util.FieldUtil;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class RadioField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  RadioField.class;
	}
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.TEXT, DataTypes.BOOL, DataTypes.FLOAT,DataTypes.INTEGER);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.TEXT;
	};
	
	@Value.Check
	protected void check() {
		new FieldUtil().checkFieldValues(dataType(), values());
	}
	
	public abstract static class Builder implements FieldBuilder {}
}

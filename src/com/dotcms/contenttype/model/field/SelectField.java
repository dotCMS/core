package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.contenttype.util.FieldUtil;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;
import com.liferay.util.StringUtil;

@Value.Immutable
public abstract class SelectField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  SelectField.class;
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
	public abstract static class Builder implements FieldBuilder {}
	
	@Value.Check
	protected void check() {
		new FieldUtil().checkFieldValues(dataType(), values());
	}
}

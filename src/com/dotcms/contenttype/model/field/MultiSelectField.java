package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;
import com.liferay.util.StringUtil;

@Value.Immutable
public abstract class MultiSelectField extends SelectableValuesField{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  MultiSelectField.class;
	}
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.LONG_TEXT);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.LONG_TEXT;
	};
	public abstract static class Builder implements FieldBuilder {}
	
}

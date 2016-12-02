package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class EmptyField extends Field {

	private static final long serialVersionUID = 1L;

	@Override
	public  Class type() {
		return  EmptyField.class;
	}
    @Value.Derived
    @Override
    public List<DataTypes> acceptedDataTypes(){
        return ImmutableList.of(
                DataTypes.LONG_TEXT,
                DataTypes.TEXT, 
                DataTypes.FLOAT, 
                DataTypes.INTEGER,

                DataTypes.BOOL,

                DataTypes.DATE,
                DataTypes.NONE,

                DataTypes.SYSTEM
                );
    }
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.TEXT;
	};
	public abstract static class Builder implements FieldBuilder {}
   @Value.Check
    public void check() {

    }
}

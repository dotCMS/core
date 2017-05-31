package com.dotcms.contenttype.model.field;

import java.util.List;

import org.immutables.value.Value;

import com.dotcms.contenttype.util.FieldUtil;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = ImmutableTimeField.class)
@JsonDeserialize(as = ImmutableTimeField.class)
@Value.Immutable
public abstract class TimeField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  TimeField.class;
	}

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.DATE);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.DATE;
	};
	public abstract static class Builder implements FieldBuilder {}
	
    @Value.Check
    public void check() {

        if(UtilMethods.isSet(defaultValue())){
          Preconditions.checkArgument(new FieldUtil().validTime(defaultValue()), this.getClass().getSimpleName() + " invalid defualt Value:" + defaultValue());
        }
    }
}

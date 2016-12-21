package com.dotcms.contenttype.model.field;

import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = ImmutableTagField.class)
@JsonDeserialize(as = ImmutableTagField.class)
@Value.Immutable
public abstract class TagField extends Field  implements OnePerContentType{


	private static final long serialVersionUID = 1L;

	@Override
	public  Class type() {
		return  TagField.class;
	}
	
	public String typeName(){
		return LegacyFieldTypes.getLegacyName(TagField.class);
	}

	@Value.Check
	public void check() {
		super.check();
		if(iDate().after(legacyFieldDate)){
			Preconditions.checkArgument(indexed(),"Tag Fields must be indexed");
		}
	}
	@Value.Default
	@Override
	public boolean indexed() {
		return true;
	};
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.SYSTEM);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SYSTEM;
	};
	public abstract static class Builder implements FieldBuilder {}
	
}

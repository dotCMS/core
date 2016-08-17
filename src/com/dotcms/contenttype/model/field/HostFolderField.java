package com.dotcms.contenttype.model.field;



import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;

@Value.Immutable
public abstract class HostFolderField extends Field implements OnePerContentType {

	@Value.Check
	public void check() {
		super.check();
		if(iDate().before(legacyFieldDate))return;
		Preconditions.checkArgument(indexed(),"Host Folder Fields must be indexed");
	}
	@Value.Default
	@Override
	public boolean indexed() {
		return true;
	};

	private static final long serialVersionUID = 1L;

	@Override
	public  Class type() {
		return  HostFolderField.class;
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SYSTEM;
	};
	@Override
	public final List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.SYSTEM);
	}
	public abstract static class Builder implements FieldBuilder {}
}

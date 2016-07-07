package com.dotcms.contenttype.model.field;

import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class CategoriesTabField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Class type() {
		return CategoriesTabField.class;
	}

	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.SECTION_DIVIDER);
	}
	public String typeName(){
		return LegacyFieldTypes.getLegacyName(ButtonField.class);
	}
	public abstract static class Builder implements FieldBuilder {}
}

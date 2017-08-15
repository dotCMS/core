package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableCategoryField.class)
@JsonDeserialize(as = ImmutableCategoryField.class)
@Value.Immutable
public abstract class CategoryField extends Field {

	private static final long serialVersionUID = 1L;

	


  @Override
	public Class type() {
		return CategoryField.class;
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

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.LABEL,
				ContentTypeFieldProperties.CATEGORY, ContentTypeFieldProperties.DISPLAY_TYPE,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.USER_SEARCHABLE,
				ContentTypeFieldProperties.RADIO_TEXT, ContentTypeFieldProperties.CATEGORIES);
	}
}

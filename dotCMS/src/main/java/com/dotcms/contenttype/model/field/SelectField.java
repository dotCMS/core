package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableSelectField.class)
@JsonDeserialize(as = ImmutableSelectField.class)
@Value.Immutable
public abstract class SelectField extends SelectableValuesField {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  SelectField.class;
	}

	@JsonIgnore
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

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
				ContentTypeFieldProperties.VALUES, ContentTypeFieldProperties.DEFAULT_VALUE,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.LISTED, ContentTypeFieldProperties.DATA_TYPE, ContentTypeFieldProperties.DATA_TYPE,
				ContentTypeFieldProperties.UNIQUE);
	}
}

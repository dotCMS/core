package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableImageField.class)
@JsonDeserialize(as = ImmutableImageField.class)
@Value.Immutable
@com.dotcms.business.Unexportable
public abstract class ImageField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public  Class type(){
		return  ImageField.class;
	}
	
	
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.TEXT;
	};

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.TEXT);
	}
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.HINT);
	}
}

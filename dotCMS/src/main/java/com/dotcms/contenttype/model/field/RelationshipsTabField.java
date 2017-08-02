package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableRelationshipsTabField.class)
@JsonDeserialize(as = ImmutableRelationshipsTabField.class)
@Value.Immutable
public abstract class RelationshipsTabField extends Field implements OnePerContentType{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  RelationshipsTabField.class;
	}

	@JsonIgnore
	@Value.Derived
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

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.LABEL, ContentTypeFieldProperties.DISPLAY_TYPE);
	}

	@JsonIgnore
	public String getContentTypeFieldHelpTextKey(){
		String legacyName = LegacyFieldTypes.getLegacyName(this.getClass());
		return "field.type.help." + legacyName;
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Relationships-Field";
	}
}

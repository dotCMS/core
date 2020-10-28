package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableKeyValueField.class)
@JsonDeserialize(as = ImmutableKeyValueField.class)
@Value.Immutable
public abstract class KeyValueField extends Field {


	@Value.Default
	@Override
	public boolean indexed() {
		return true;
	};
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  KeyValueField.class;
	}
	
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.LONG_TEXT;
	};

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.LONG_TEXT);
	}
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.HINT,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED);
	}

	@JsonIgnore
	public String getContentTypeFieldHelpTextKey(){
		return "field.type.help.keyvalue";
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Key-Value";
	}
}

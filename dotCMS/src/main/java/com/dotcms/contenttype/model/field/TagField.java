package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.ListType;
import com.dotcms.content.model.type.TagType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

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

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.DEFAULT_VALUE, ContentTypeFieldProperties.HINT,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.DATA_TYPE);
	}

	@Override
	public Optional<FieldValue<?>> fieldValue(Object value) {
		if (value instanceof String) {
			final String string = (String)value;
			final List<TagType> list = Arrays.stream(string.split("\\s*,\\s*")).map(TagType::of).collect(Collectors.toList());
			return Optional.of(ListType.of(list));
		}
		if(value instanceof List){
			return Optional.of(ListType.of((List)value));
		}
		return Optional.empty();
	}
	
}

package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.system.TagFieldType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

}

package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.TextAreaType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * This represents a JSON field. It's validated at checkin that the value actually parses to JSON
 * It renders as a textArea in the Content Editor.
 * When checking in content via REST (workflow resource) it takes a JSON object as the field value
 * When returning content from REST with a field of this type, it will return a JSON object
 * When accessing this field from velocity it will be available as a map
 * When accessing this field from GraphQL it will be available as a JSON object
 */

@JsonSerialize(as = ImmutableJSONField.class)
@JsonDeserialize(as = ImmutableJSONField.class)
@Value.Immutable
public abstract class JSONField extends Field {


	private static final long serialVersionUID = 1L;
	
	@Override
	public  Class type() {
		return  JSONField.class;
	}

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.LONG_TEXT);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.LONG_TEXT;
	};
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
				ContentTypeFieldProperties.REGEX_CHECK, ContentTypeFieldProperties.DEFAULT_VALUE, ContentTypeFieldProperties.HINT,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED);
	}

	/**
	 * {@inheritDoc}
     * @return
     */
	@Override
	public Optional<FieldValueBuilder> fieldValue(Object value) {
		if (value instanceof String) {
			return Optional.of(TextAreaType.builder().value((String) value));
		}
		return Optional.empty();
	}


}

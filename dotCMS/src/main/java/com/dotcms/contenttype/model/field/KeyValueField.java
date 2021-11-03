package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.keyvalue.Entry;
import com.dotcms.content.model.type.keyvalue.KeyValueType;
import com.dotcms.contenttype.util.KeyValueFieldUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<FieldValue<?>> fieldValue(final Object value) {
		if (value instanceof String) {
			final Map<String, Object> map = KeyValueFieldUtil
					.JSONValueToHashMap((String) value);

			final List<Entry<?>> asList = map.entrySet().stream()
					.map(entry -> Entry.of(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());

			return Optional.of(KeyValueType.of(asList));
		}

		if (value instanceof Map) {
			final Map<String, Object> map = (Map<String, Object>) value;

			final List<Entry<?>> asList = map.entrySet().stream()
					.map(entry -> Entry.of(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());

			return Optional.of(KeyValueType.of(asList));
		}
		return Optional.empty();
	}

	@JsonIgnore
    static public LinkedHashMap<String,?> asMap(final List<Entry<?>> asList){
		return asList.stream()
				.collect(Collectors.toMap(entry -> entry.key, entry -> entry.value,(k, k2) -> k, LinkedHashMap::new));
	}

}

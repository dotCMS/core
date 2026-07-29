package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.keyvalue.Entry;
import com.dotcms.content.model.type.keyvalue.KeyValueType;
import com.dotcms.contenttype.util.KeyValueFieldUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;

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
	 * Converts an incoming value (JSON string or Map) into a {@link FieldValueBuilder}
	 * (specifically a {@link KeyValueType} builder) whose {@code value} is a
	 * {@code List<Entry<?>>}.
	 *
	 * <p>The list form deliberately preserves insertion order for the DB write path:
	 * Jackson serializes it as a JSON <em>array</em>
	 * (e.g. {@code "value":[{"key":"B","value":"b"},{"key":"D","value":"d"}]}),
	 * which guarantees key order across the {@code contentlet_as_json} round-trip.
	 * A JSON object would not guarantee key order per specification.</p>
	 *
	 * <p>On the read path the list is converted back to an {@link OrderedKeyValueMap}
	 * via {@link #asMap(List)}.</p>
	 *
	 * @return a {@link FieldValueBuilder} ({@code KeyValueType.Builder}) wrapped in an
	 *         Optional, or empty if the value type is not a String or Map
	 */
	@Override
	public Optional<FieldValueBuilder> fieldValue(final Object value) {
		if (value instanceof String) {
			final Map<String, Object> map = KeyValueFieldUtil
					.JSONValueToHashMap((String) value);

			final List<Entry<?>> asList = map.entrySet().stream()
					.map(entry -> Entry.of(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());

			return Optional.of(KeyValueType.builder().value(asList));
		}

		if (value instanceof Map) {
			final Map<String, Object> map = (Map<String, Object>) value;

			final List<Entry<?>> asList = map.entrySet().stream()
					.map(entry -> Entry.of(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());

			return Optional.of(KeyValueType.builder().value(asList));
		}
		return Optional.empty();
	}

	/**
	 * Converts the {@code List<Entry<?>>} representation (used for DB storage) back into an
	 * {@link OrderedKeyValueMap}, preserving the insertion order from the JSON array.
	 *
	 * <p>{@link OrderedKeyValueMap} is a {@link java.util.LinkedHashMap} subclass annotated
	 * with a custom {@code @JsonSerialize} that writes entries in iteration order, bypassing
	 * {@code SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS} which would otherwise re-sort
	 * keys alphabetically in REST responses.</p>
	 *
	 * @see OrderedKeyValueMap for the full write/read cycle documentation
	 * @see com.dotcms.content.model.type.keyvalue.AbstractKeyValueType for why a List is used
	 *      in the DB rather than a JSON object
	 */
	@JsonIgnore
	public static Map<String,?> asMap(final List<Entry<?>> asList){
		final OrderedKeyValueMap result = new OrderedKeyValueMap();
		asList.forEach(entry -> result.put(entry.key, entry.value));
		return result;
	}

}

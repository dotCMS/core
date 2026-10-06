package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Insertion-order-preserving Map for KeyValue fields.
 *
 * <p>dotCMS's REST ObjectMapper ({@code DotObjectMapperProvider}) is configured with
 * {@code SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS=true}, which re-sorts every raw
 * {@link Map} alphabetically before writing JSON. This subclass carries a custom
 * {@code @JsonSerialize} so Jackson delegates to {@link Serializer} instead, writing entries
 * in the insertion order that was preserved from the database array representation.</p>
 *
 * <p>Full KeyValue write/read cycle:</p>
 * <pre>
 *   Write: REST input (JSON string or Map)
 *          → LinkedHashMap  ({@link com.dotcms.contenttype.util.KeyValueFieldUtil#JSONValueToHashMap})
 *          → List&lt;Entry&lt;?&gt;&gt;  ({@link KeyValueField#fieldValue} — preserves LinkedHashMap order)
 *          → JSON array stored in {@code contentlet_as_json}
 *            e.g. {@code "value": [{"key":"B","value":"b"}, {"key":"D","value":"d"}]}
 *
 *   Read:  JSON array → List&lt;Entry&lt;?&gt;&gt;  (Jackson / {@link com.dotcms.content.model.type.keyvalue.AbstractKeyValueType})
 *          → {@link OrderedKeyValueMap}  ({@link KeyValueField#asMap})
 *          → REST response written in insertion order  ({@link Serializer})
 * </pre>
 */
@JsonSerialize(using = OrderedKeyValueMap.Serializer.class)
public class OrderedKeyValueMap extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public OrderedKeyValueMap() {
        super();
    }

    public OrderedKeyValueMap(final Map<? extends String, ?> source) {
        super(source);
    }

    /**
     * Custom Jackson serializer that writes map entries in their natural iteration order,
     * bypassing {@code SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS} which would otherwise
     * sort keys alphabetically.
     */
    public static class Serializer extends JsonSerializer<OrderedKeyValueMap> {

        @Override
        public void serialize(final OrderedKeyValueMap value, final JsonGenerator gen,
                final SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            for (final Map.Entry<String, Object> entry : value.entrySet()) {
                gen.writeObjectField(entry.getKey(), entry.getValue());
            }
            gen.writeEndObject();
        }
    }
}

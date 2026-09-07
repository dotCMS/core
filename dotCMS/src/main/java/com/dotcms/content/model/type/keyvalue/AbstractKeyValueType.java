package com.dotcms.content.model.type.keyvalue;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;

/**
 * Immutable JSON representation of a KeyValue field stored in {@code contentlet_as_json}.
 *
 * <p>The {@code value} is deliberately typed as {@code List<Entry<?>>} rather than a JSON object
 * so that entry order is guaranteed by the JSON specification — arrays are ordered, but object
 * key order is not. Jackson serializes this as:
 * {@code "value": [{"key":"B","value":"b"}, {"key":"D","value":"d"}]}</p>
 *
 * <p>Full write/read cycle:</p>
 * <pre>
 *   Write: REST input (JSON string or Map)
 *          → LinkedHashMap  (KeyValueFieldUtil.JSONValueToHashMap — insertion order)
 *          → List&lt;Entry&lt;?&gt;&gt;  (KeyValueField.fieldValue — preserves LinkedHashMap order)
 *          → JSON array stored in contentlet_as_json
 *
 *   Read:  JSON array → List&lt;Entry&lt;?&gt;&gt;  (Jackson deserialization of this type)
 *          → OrderedKeyValueMap  (KeyValueField.asMap — preserves array order)
 *          → REST response written in insertion order  (OrderedKeyValueMap.Serializer,
 *            bypassing DotObjectMapperProvider's ORDER_MAP_ENTRIES_BY_KEYS=true)
 * </pre>
 */
@ValueType
@Immutable
@JsonDeserialize(as = KeyValueType.class)
@JsonTypeName(value = AbstractKeyValueType.TYPENAME)
public interface AbstractKeyValueType extends FieldValue<List<Entry<?>>> {

    String TYPENAME = "KeyValue";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

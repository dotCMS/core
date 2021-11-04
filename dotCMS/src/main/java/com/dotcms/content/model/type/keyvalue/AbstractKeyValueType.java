package com.dotcms.content.model.type.keyvalue;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.dotcms.content.model.type.keyvalue.KeyValueType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * KeyValue-Field json representation
 */
@ValueTypeStyle
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
    };

    /**
     * {@inheritDoc}
     */
    @JsonProperty("value")
    @Parameter
    List<Entry<?>> value();

}

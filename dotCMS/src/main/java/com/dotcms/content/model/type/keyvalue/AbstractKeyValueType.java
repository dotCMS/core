package com.dotcms.content.model.type.keyvalue;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;

/**
 * KeyValue-Field json representation
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

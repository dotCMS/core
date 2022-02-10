package com.dotcms.content.model.type.select;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Long Single-Select Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = LongSelectFieldType.class)
@JsonTypeName(value = AbstractLongSelectFieldType.TYPENAME)
public interface AbstractLongSelectFieldType extends FieldValue<Long> {

    String TYPENAME = "Long-Select";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

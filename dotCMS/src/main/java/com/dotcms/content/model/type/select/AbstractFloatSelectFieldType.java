package com.dotcms.content.model.type.select;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Float Single-Select Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = FloatSelectFieldType.class)
@JsonTypeName(value = AbstractFloatSelectFieldType.TYPENAME)
public interface AbstractFloatSelectFieldType extends FieldValue<Float> {

    String TYPENAME = "Float-Select";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

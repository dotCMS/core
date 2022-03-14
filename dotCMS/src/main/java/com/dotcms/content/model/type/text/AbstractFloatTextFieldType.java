package com.dotcms.content.model.type.text;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Float Stored Value Text-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = FloatTextFieldType.class)
@JsonTypeName(value = AbstractFloatTextFieldType.TYPENAME)
public interface AbstractFloatTextFieldType extends FieldValue<Float> {

    String TYPENAME = "Float";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

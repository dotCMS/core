package com.dotcms.content.model.type.radio;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Boolean Radio Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = BoolRadioFieldType.class)
@JsonTypeName(value = AbstractBoolRadioFieldType.TYPENAME)
public interface AbstractBoolRadioFieldType extends FieldValue<Boolean> {

    String TYPENAME = "Bool-Radio";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

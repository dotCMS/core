package com.dotcms.content.model.type.radio;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Long Radio Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = LongRadioFieldType.class)
@JsonTypeName(value = AbstractLongRadioFieldType.TYPENAME)
public interface AbstractLongRadioFieldType extends FieldValue<Long> {

    String TYPENAME = "Long-Radio";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}

}

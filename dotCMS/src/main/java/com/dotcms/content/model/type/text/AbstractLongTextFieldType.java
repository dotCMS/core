package com.dotcms.content.model.type.text;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Long Value Stored Text-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = LongTextFieldType.class)
@JsonTypeName(value = AbstractLongTextFieldType.TYPENAME)
public interface AbstractLongTextFieldType extends FieldValue<Long> {

    String TYPENAME = "Long";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

package com.dotcms.content.model.type.hidden;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Custom Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = LongHiddenFieldType.class)
@JsonTypeName(value = AbstractLongHiddenFieldType.TYPENAME)
public interface AbstractLongHiddenFieldType extends FieldValue<Long> {

    String TYPENAME = "Long-Hidden";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

package com.dotcms.content.model.type.select;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Bool Single-Select Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = BoolSelectFieldType.class)
@JsonTypeName(value = AbstractBoolSelectFieldType.TYPENAME)
public interface AbstractBoolSelectFieldType extends FieldValue<Boolean> {

    String TYPENAME = "Bool-Select";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

package com.dotcms.content.model.type.select;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Text Multi-Select Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = MultiSelectFieldType.class)
@JsonTypeName(value = AbstractMultiSelectFieldType.TYPENAME)
public interface AbstractMultiSelectFieldType extends FieldValue<String> {

    String TYPENAME = "Multi-Select";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

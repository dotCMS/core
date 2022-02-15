package com.dotcms.content.model.type;

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
@JsonDeserialize(as = CustomFieldType.class)
@JsonTypeName(value = AbstractCustomFieldType.TYPENAME)
public interface AbstractCustomFieldType extends FieldValue<String> {

    String TYPENAME = "Custom";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

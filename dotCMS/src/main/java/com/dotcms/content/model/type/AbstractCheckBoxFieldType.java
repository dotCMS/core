package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * CheckBox-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = CheckBoxFieldType.class)
@JsonTypeName(value = AbstractCheckBoxFieldType.TYPENAME)
public interface AbstractCheckBoxFieldType extends FieldValue<String> {

    String TYPENAME = "CheckBox";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

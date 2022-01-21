package com.dotcms.content.model.type.text;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Regular Text Value Text-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = TextFieldType.class)
@JsonTypeName(value = AbstractTextFieldType.TYPENAME)
public interface AbstractTextFieldType extends FieldValue<String> {

    String TYPENAME = "Text";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

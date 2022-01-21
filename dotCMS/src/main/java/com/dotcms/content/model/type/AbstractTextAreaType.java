package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * TextArea-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = TextAreaType.class)
@JsonTypeName(value = AbstractTextAreaType.TYPENAME)
public interface AbstractTextAreaType extends FieldValue<String> {

    String TYPENAME = "TextArea";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Wysiwyg-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = WysiwygType.class)
@JsonTypeName(value = AbstractWysiwygType.TYPENAME)
public interface AbstractWysiwygType extends FieldValue<String> {

    String TYPENAME = "Wysiwyg";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

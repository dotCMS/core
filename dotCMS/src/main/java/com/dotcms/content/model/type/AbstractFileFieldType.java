package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;


/**
 * File-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = FileFieldType.class)
@JsonTypeName(value = AbstractFileFieldType.TYPENAME)
public interface AbstractFileFieldType extends FieldValue<String> {

    String TYPENAME = "File";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

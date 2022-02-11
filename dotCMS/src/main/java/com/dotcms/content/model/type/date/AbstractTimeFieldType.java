package com.dotcms.content.model.type.date;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import org.immutables.value.Value.Immutable;

/**
 * Time Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = TimeFieldType .class)
@JsonTypeName(value = AbstractTimeFieldType.TYPENAME)
public interface AbstractTimeFieldType extends FieldValue<Instant> {

    String TYPENAME = "Time";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}

}

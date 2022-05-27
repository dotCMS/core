package com.dotcms.content.model.type.hidden;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;
import org.immutables.value.Value.Immutable;

/**
 * Custom Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = DateHiddenFieldType.class)
@JsonTypeName(value = AbstractDateHiddenFieldType.TYPENAME)
public interface AbstractDateHiddenFieldType extends FieldValue<Date> {

    String TYPENAME = "Date-Hidden";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}

}

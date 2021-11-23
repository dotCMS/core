package com.dotcms.content.model.type.date;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.dotcms.content.model.type.AbstractBinaryFieldType;
import com.dotcms.content.model.type.AbstractBinaryFieldType.Builder;
import com.dotcms.content.model.type.BinaryFieldType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import org.immutables.value.Value.Immutable;

/**
 * DateField json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = DateFieldType.class)
@JsonTypeName(value = AbstractDateFieldType.TYPENAME)
public interface AbstractDateFieldType extends FieldValue<Instant> {

    String TYPENAME = "Date";

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}

}

package com.dotcms.content.model.type.system;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Constant-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = ConstantFieldType.class)
@JsonTypeName(value = AbstractConstantFieldType.TYPENAME)
public interface AbstractConstantFieldType extends FieldValue<String> {

    String TYPENAME = "Constant";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    };

    abstract class Builder implements FieldValueBuilder {

    }

}

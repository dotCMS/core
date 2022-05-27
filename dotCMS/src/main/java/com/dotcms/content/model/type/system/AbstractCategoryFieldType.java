package com.dotcms.content.model.type.system;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;


/**
 * Binary-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = CategoryFieldType.class)
@JsonTypeName(value = AbstractCategoryFieldType.TYPENAME)
public interface AbstractCategoryFieldType extends FieldValue<List<String>> {

    String TYPENAME = "Categories";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    };

    abstract class Builder implements FieldValueBuilder {}

}

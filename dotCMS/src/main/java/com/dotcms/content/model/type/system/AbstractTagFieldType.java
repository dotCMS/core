package com.dotcms.content.model.type.system;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;

/**
 * Tag-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = TagFieldType.class)
@JsonTypeName(value = AbstractTagFieldType.TYPENAME)
public interface AbstractTagFieldType extends FieldValue <List<String>> {

    String TYPENAME = "Tags";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    };

    abstract class Builder implements FieldValueBuilder {}

}

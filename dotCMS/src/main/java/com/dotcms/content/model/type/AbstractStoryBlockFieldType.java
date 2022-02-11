package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

@ValueType
@Immutable
@JsonDeserialize(as = StoryBlockFieldType.class)
@JsonTypeName(value = AbstractStoryBlockFieldType.TYPENAME)
public interface AbstractStoryBlockFieldType extends FieldValue<String> {

    String TYPENAME = "StoryBlock";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

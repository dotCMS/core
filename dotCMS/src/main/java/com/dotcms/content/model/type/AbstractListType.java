package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;

/**
 * List json representation for any List Like field we might need
 */
@Immutable
@JsonDeserialize(as = ListType.class)
@JsonTypeName(value = AbstractListType.TYPENAME)
@ValueType
public interface AbstractListType<T> extends FieldValue<List<Object>> {

    String TYPENAME = "List";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}

package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.ListType;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@JsonDeserialize(as = ListType.class)
@JsonTypeName(value = AbstractListType.TYPENAME)
@ValueTypeStyle
public interface AbstractListType<T> extends FieldValue<List<?>> {

    String TYPENAME = "List";

    @Override
    default String type() {
        return TYPENAME;
    };

    @JsonProperty("value")
    @Parameter
    List<?> value();

}

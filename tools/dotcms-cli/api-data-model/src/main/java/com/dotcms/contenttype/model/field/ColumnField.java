package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableColumnField.class)
@JsonDeserialize(as = ImmutableColumnField.class)
public abstract class ColumnField extends Field {

    @Nullable
    public abstract String variable();

}

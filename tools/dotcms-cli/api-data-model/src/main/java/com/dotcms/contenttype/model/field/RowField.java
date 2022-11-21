package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRowField.class)
@JsonDeserialize(as = ImmutableRowField.class)
public abstract class RowField extends Field {

    @Nullable
    public abstract String variable();

}

package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableWysiwygField.class)
@JsonDeserialize(as = ImmutableWysiwygField.class)
public abstract class WysiwygField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType() {
        return DataTypes.LONG_TEXT;
    };

}

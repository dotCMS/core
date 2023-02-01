package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePermissionTabField.class)
@JsonDeserialize(as = ImmutablePermissionTabField.class)
public abstract class PermissionTabField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.SYSTEM;
    };

}

package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
@Value.Immutable
@JsonSerialize(as = ImmutableHostFolderField.class)
@JsonDeserialize(as = ImmutableHostFolderField.class)
public abstract class HostFolderField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.SYSTEM;
    };

}

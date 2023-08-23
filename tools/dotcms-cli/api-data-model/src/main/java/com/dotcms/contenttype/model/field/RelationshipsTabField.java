package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRelationshipsTabField.class)
@JsonDeserialize(as = ImmutableRelationshipsTabField.class)
public abstract class RelationshipsTabField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.SYSTEM;
    }

}

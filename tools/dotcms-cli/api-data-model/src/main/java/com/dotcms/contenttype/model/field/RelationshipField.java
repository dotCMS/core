package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRelationshipField.class)
@JsonDeserialize(as = ImmutableRelationshipField.class)
public abstract class RelationshipField extends Field {

}

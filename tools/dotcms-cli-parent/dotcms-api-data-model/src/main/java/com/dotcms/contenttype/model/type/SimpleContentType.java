package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableSimpleContentType.class)
@JsonDeserialize(as = ImmutableSimpleContentType.class)
@Value.Immutable
public abstract class SimpleContentType extends ContentType {

}

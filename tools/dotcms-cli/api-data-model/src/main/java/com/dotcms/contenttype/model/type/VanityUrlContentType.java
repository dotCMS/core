package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableVanityUrlContentType.class)
@JsonDeserialize(as = ImmutableVanityUrlContentType.class)
@Value.Immutable
public abstract class VanityUrlContentType extends ContentType {

}

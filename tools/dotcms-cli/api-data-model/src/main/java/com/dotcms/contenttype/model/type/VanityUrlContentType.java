package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
@Value.Immutable
@JsonSerialize(as = ImmutableVanityUrlContentType.class)
@JsonDeserialize(as = ImmutableVanityUrlContentType.class)
public abstract class VanityUrlContentType {

}

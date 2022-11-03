package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableDotAssetContentType.class)
@JsonDeserialize(as = ImmutableDotAssetContentType.class)
@Value.Immutable
public abstract class DotAssetContentType extends ContentType {

}

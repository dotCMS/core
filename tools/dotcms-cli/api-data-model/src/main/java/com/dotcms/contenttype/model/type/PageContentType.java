package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePageContentType.class)
@JsonDeserialize(as = ImmutablePageContentType.class)
public abstract class PageContentType extends ContentType {

}

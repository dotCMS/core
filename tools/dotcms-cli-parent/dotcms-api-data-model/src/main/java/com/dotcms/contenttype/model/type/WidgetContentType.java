package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableWidgetContentType.class)
@JsonDeserialize(as = ImmutableWidgetContentType.class)
public abstract class WidgetContentType extends ContentType {

}

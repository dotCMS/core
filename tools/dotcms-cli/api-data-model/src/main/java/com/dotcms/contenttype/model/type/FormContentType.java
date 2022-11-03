package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableFormContentType.class)
@JsonDeserialize(as = ImmutableFormContentType.class)
@Value.Immutable
public abstract class FormContentType extends ContentType {

}

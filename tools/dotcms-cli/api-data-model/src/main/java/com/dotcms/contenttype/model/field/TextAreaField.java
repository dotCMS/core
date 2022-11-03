package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableTextAreaField.class)
@JsonDeserialize(as = ImmutableTextAreaField.class)
public abstract class TextAreaField extends Field {

}

package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableLineDividerField.class)
@JsonDeserialize(as = ImmutableLineDividerField.class)
public abstract class LineDividerField extends Field {

}

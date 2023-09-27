package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePersonaContentType.class)
@JsonDeserialize(as = ImmutablePersonaContentType.class)
public abstract class PersonaContentType extends ContentType {

}

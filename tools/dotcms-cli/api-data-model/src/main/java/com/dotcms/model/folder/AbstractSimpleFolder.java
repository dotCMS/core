package com.dotcms.model.folder;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = SimpleFolder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractSimpleFolder {

    String name();
}

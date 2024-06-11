package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = BuildVersion.class)
public interface AbstractBuildVersion {

    String name();

    String version();

    long timestamp();

    String revision();

}

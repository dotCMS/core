package com.dotcms.model.config;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = WorkspaceInfo.class)
public interface AbstractWorkspaceInfo {
    String name();

    default String version() {return "1.0.0"; }

    default String description() {return " DO NOT ERASE ME !!! I am a marker file required by dotCMS CLI."; }

}

package com.dotcms.model.config;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.UUID;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = WorkspaceInfo.class)
public interface AbstractWorkspaceInfo {
    @Value.Default
    default String name(){return "dotCMS-cli workspace"; }

    //This id helps us to identify the workspace when testing the CLI.
    @Value.Default
    default String id() {return UUID.randomUUID().toString(); }

    //TODO: We need to add a serious versioning system here.
    @Value.Default
    default String version() {return "1.0.0"; }

    @Value.Default
    default String description() {return " DO NOT ERASE ME !!! I am a marker file required by dotCMS CLI."; }

}

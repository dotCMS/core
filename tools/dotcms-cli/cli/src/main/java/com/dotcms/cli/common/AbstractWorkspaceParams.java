package com.dotcms.cli.common;

import com.dotcms.model.annotation.ValueType;
import java.nio.file.Path;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
public interface AbstractWorkspaceParams {

    Path workspacePath();

    @Value.Default
    default boolean userProvided() { return false; }

}

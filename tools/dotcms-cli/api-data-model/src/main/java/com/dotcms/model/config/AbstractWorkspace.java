package com.dotcms.model.config;


import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = Workspace.class)
public interface AbstractWorkspace {

    String FILES_NAMESPACE = "files";
    String CONTENT_TYPES_NAMESPACE = "content-types";
    String SITES_NAMESPACE = "sites";
    String LANGUAGES_NAMESPACE = "languages";

    Path root();

    @Value.Derived
    default Path contentTypes() {
        return Paths.get(root().toString(), CONTENT_TYPES_NAMESPACE);
    }

    @Value.Derived
    default Path sites() {
        return Paths.get(root().toString(), SITES_NAMESPACE);
    }

    @Value.Derived
    default Path languages() {
        return Paths.get(root().toString(), LANGUAGES_NAMESPACE);
    }

    @Value.Derived
    default Path files() {
        return Paths.get(root().toString(), FILES_NAMESPACE);
    }

    @Nullable
    String id();

}

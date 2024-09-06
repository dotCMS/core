package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;

@ValueType
@Value.Immutable
@JsonDeserialize(as = FolderView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractFolderView {

    @Nullable
    String host();

    String path();

    String name();

    @Nullable
    String title();

    @Nullable
    Instant modDate();

    @Nullable
    String identifier();

    @Nullable
    String inode();

    @Nullable
    Boolean showOnMenu();

    @Nullable
    Integer sortOrder();

    @Nullable
    String filesMasks();

    @Nullable
    String defaultFileType();

    @Value.Default
    default int level() {
        return 0;
    }

    @Value.Default
    default boolean explicitGlobInclude() {
        return false;
    }

    @Value.Default
    default boolean explicitGlobExclude() {
        return false;
    }

    @Value.Default
    default boolean implicitGlobInclude() {
        return true;
    }

    @Nullable
    @JsonUnwrapped
    AssetVersionsView assets();

    @Nullable
    List<FolderView> subFolders();

    @Auxiliary
    Optional<FolderSync> sync();
}

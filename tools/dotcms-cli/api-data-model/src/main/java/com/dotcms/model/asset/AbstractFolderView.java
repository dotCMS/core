package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = FolderView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractFolderView {

    @Nullable
    String site();

    String path();

    String name();

    @Nullable
    Instant modDate();

    @Nullable
    String identifier();

    @Nullable
    String inode();

    @Value.Default
    default int level() {
        return 0;
    }

    @Value.Default
    default boolean include() {
        return true;
    }

    @Nullable
    @JsonUnwrapped
    AssetVersionsView assets();

    @Nullable
    List<FolderView> subFolders();
}

package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;

@ValueType
@Value.Immutable
@JsonDeserialize(as = FolderView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractFolderView {

    @Nullable
    abstract String host();

    abstract String path();

    abstract String name();

    @Nullable
    abstract String title();

    @Nullable
    abstract Instant modDate();

    @Nullable
    abstract String identifier();

    @Nullable
    abstract String inode();

    @Nullable
    abstract Boolean showOnMenu();

    @Nullable
    abstract Integer sortOrder();

    @Nullable
    abstract String filesMasks();

    @Nullable
    abstract String defaultFileType();

    @Value.Default
    int level() {
        return 0;
    }

    @Value.Default
    boolean explicitGlobInclude() {
        return false;
    }

    @Value.Default
    boolean explicitGlobExclude() {
        return false;
    }

    @Value.Default
    boolean implicitGlobInclude() {
        return true;
    }

    @Nullable
    @JsonUnwrapped
    abstract AssetVersionsView assets();

    @Nullable
    abstract List<FolderView> subFolders();

    abstract Optional<Boolean> markForPush();

    abstract Optional<Boolean> markForDelete();

    //TODO: Apparently these two arent used anywhere
    //abstract Optional<String> localStatus();
    //abstract Optional<String> localLanguage();

    @Auxiliary
    abstract Optional<FolderSyncMeta> syncMeta();

}

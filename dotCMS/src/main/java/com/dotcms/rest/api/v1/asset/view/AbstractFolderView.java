package com.dotcms.rest.api.v1.asset.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = FolderView.Builder.class)
@JsonInclude(Include.NON_EMPTY)
public interface AbstractFolderView extends WebAssetView {

        String path();

        String name();

        Instant modDate();

        String identifier();

        String inode();

        @Nullable
        @JsonUnwrapped
        AssetVersionsView assets();

        @Nullable
        List<FolderView> subFolders();
}

package com.dotcms.rest.api.v1.asset;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = ResolvedAssetAndPath.Builder.class)
public interface AbstractResolvedAssetAndPath {

    String host();

    Host resolvedHost();

    @Nullable
    String path();

    Folder resolvedFolder();

    @Nullable
    String asset();

    @Nullable
    FileAsset resolvedFileAsset();
}

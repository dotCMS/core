package com.dotcms.rest.api.v1.asset;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * Resolved Asset And Path is a json representation of a resolved asset and path
 */
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

    @JsonIgnore
    @Default
    default boolean newFolder() {return false;}

}

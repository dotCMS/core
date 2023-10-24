package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

@ValueType
@Value.Immutable
@JsonDeserialize(as = FolderSyncMeta.class)
public interface AbstractFolderSyncMeta {

    String UNKNOWN = "unknown";

    @Value.Default
    default boolean explicitGlobInclude() { return false;}

    @Value.Default
    default boolean explicitGlobExclude() { return false;}

    @Value.Default
    default boolean implicitGlobInclude() { return true;}

    @Default
    default boolean markedForPush(){return false;}

    @Default
    default boolean markedForDelete(){return false;}

    @Default
    default String localStatus(){return UNKNOWN;}

    @Default
    default String localLanguage(){return UNKNOWN;}
}

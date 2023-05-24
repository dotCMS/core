package com.dotcms.rest.api.v1.asset;


import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = FolderAndAsset.Builder.class)
public interface AbstractFolderAndAsset {

        Folder folder();

        @Nullable
        String asset();

}

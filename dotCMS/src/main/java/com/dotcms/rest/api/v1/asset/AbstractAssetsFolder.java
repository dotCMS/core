package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = AssetsFolder.Builder.class)
public interface AbstractAssetsFolder extends SimpleWebAsset {

        List<Asset> assets();

        List<AssetsFolder> subFolders();
}

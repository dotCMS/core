package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = AssetsFolder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractAssetsFolder extends SimpleWebAsset {

    int level();

    List<Asset> assets();

    List<AssetsFolder> subFolders();
}

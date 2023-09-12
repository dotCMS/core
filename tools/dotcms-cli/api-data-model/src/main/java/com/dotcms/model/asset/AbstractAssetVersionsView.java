package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = AssetVersionsView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractAssetVersionsView {

    List<AssetView> versions();
}

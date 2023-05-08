package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = AssetView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractAssetView {

    String path();

    String name();

    Instant modDate();

    String identifier();

    String inode();

    String sha256();

    Long size();

    boolean live();

    String lang();

}

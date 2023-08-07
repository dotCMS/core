package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Map;

@ValueType
@Value.Immutable
@JsonDeserialize(as = AssetView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractAssetView {

    String name();

    Instant modDate();

    String identifier();

    String inode();

    long sortOrder();

    boolean live();

    boolean working();

    String lang();

    Map<String, Object> metadata();

}

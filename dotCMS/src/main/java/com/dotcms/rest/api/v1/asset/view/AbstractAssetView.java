package com.dotcms.rest.api.v1.asset.view;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import org.immutables.value.Value;

/**
 * Asset View is a json representation of an asset
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = AssetView.Builder.class)
public interface AbstractAssetView  extends WebAssetView {

    String name();
    String identifier();
    String inode();
    Instant modDate();
    boolean live();
    boolean working();
    String lang();
    long sortOrder();
    Map<String, Object> metadata();
}

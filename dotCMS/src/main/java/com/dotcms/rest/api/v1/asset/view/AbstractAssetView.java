package com.dotcms.rest.api.v1.asset.view;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = AssetView.Builder.class)
public interface AbstractAssetView  {

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

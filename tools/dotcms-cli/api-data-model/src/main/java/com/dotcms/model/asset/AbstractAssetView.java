package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.smallrye.common.constraint.Nullable;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value.Auxiliary;

@ValueType
@Value.Immutable
@JsonDeserialize(as = AssetView.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractAssetView {

    String name();

    @Nullable
    Instant modDate();

    @Nullable
    String identifier();

    @Nullable
    String inode();

    long sortOrder();

    boolean live();

    boolean working();

    String lang();

    Map<String, Object> metadata();

    @Auxiliary
    Optional<AssetSync> sync();

}

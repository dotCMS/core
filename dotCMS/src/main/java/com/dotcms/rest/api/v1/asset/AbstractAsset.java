package com.dotcms.rest.api.v1.asset;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Set;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = Asset.Builder.class)
public interface AbstractAsset extends SimpleWebAsset {
    String sha256();
    Long size();

    boolean live();
    String lang();
}

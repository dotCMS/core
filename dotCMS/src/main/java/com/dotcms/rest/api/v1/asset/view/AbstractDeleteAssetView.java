package com.dotcms.rest.api.v1.asset.view;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = DeleteAssetView.Builder.class)
@JsonInclude(Include.NON_EMPTY)
public interface AbstractDeleteAssetView extends WebAssetView{

    String assetPath();
    @Nullable
    Boolean live();
    @Nullable
    String language();


}

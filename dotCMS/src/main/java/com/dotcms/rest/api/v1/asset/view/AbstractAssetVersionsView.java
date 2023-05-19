package com.dotcms.rest.api.v1.asset.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

/**
 * Asset Versions View is a json representation of a list of asset versions
 * This should contain all active files within a folder including every lang version
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = AssetVersionsView.Builder.class)
@JsonInclude(Include.NON_EMPTY)
public interface AbstractAssetVersionsView extends WebAssetView {

    List<AssetView> versions();

}

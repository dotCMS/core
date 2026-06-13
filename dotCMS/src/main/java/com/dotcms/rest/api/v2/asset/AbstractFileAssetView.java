package com.dotcms.rest.api.v2.asset;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Lightweight view returned by write operations (save/publish) in the v2 file-asset endpoint.
 * Contains the persisted asset's key metadata so callers can verify what was stored.
 *
 * <p>Follows the same {@code @Value.Immutable} {@code Abstract*} convention as the v1 asset views
 * (e.g. {@link com.dotcms.rest.api.v1.asset.view.AbstractAssetView}) so the v2 package stays
 * aligned with the REST view conventions.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*", passAnnotations = Schema.class)
@Value.Immutable
@JsonDeserialize(as = FileAssetView.class)
@Schema(description = "File asset view returned after a save or publish operation")
public interface AbstractFileAssetView {

    @Schema(description = "Asset identifier", example = "48190c8c-42c4-46af-8d1a-0cd5db894797")
    String identifier();

    @Schema(description = "Asset inode", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    String inode();

    @Schema(description = "File name", example = "banner.vtl")
    String name();

    @Schema(description = "Host-qualified path to the asset",
            example = "//demo.dotcms.com/application/containers/default/banner.vtl")
    String path();

    @Schema(description = "Language tag for the asset version", example = "en-US")
    String lang();

    @Schema(description = "Whether this is the live (published) version", example = "false")
    boolean live();

    @Schema(description = "Whether this is the working version", example = "true")
    boolean working();

    @Schema(description = "File size in bytes as stored in the content repository", example = "4096")
    long fileSize();
}

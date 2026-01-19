package com.dotcms.rest.api.v1.publishing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Represents a preview of an asset within a publishing bundle.
 * Used in the publishing jobs list to provide a quick overview of bundle contents.
 *
 * @author dotCMS
 * @since Jan 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = AssetPreviewView.class)
@JsonDeserialize(as = AssetPreviewView.class)
@Schema(description = "Preview of an asset in a publishing bundle")
public interface AbstractAssetPreviewView {

    /**
     * The unique identifier of the asset.
     *
     * @return Asset identifier
     */
    @Schema(
            description = "Asset identifier",
            example = "abc123-content-id",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String id();

    /**
     * Human-readable title of the asset.
     *
     * @return Asset title
     */
    @Schema(
            description = "Human-readable asset title",
            example = "Homepage Content Update",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String title();

    /**
     * The type of asset (e.g., contentlet, template, container).
     *
     * @return Asset type
     */
    @Schema(
            description = "Asset type (e.g., contentlet, template, container, folder, host)",
            example = "contentlet",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String type();

}

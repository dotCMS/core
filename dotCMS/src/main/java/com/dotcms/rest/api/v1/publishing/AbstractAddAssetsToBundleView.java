package com.dotcms.rest.api.v1.publishing;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Result of adding assets to a bundle.
 * Contains the bundle details and per-asset error information.
 *
 * @author hassandotcms
 * @since Mar 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = AddAssetsToBundleView.class)
@JsonDeserialize(as = AddAssetsToBundleView.class)
@Schema(description = "Result of adding assets to a bundle")
public interface AbstractAddAssetsToBundleView {

    /**
     * The bundle identifier used (new or existing).
     *
     * @return Bundle ID
     */
    @Schema(
            description = "Bundle identifier (new or existing)",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String bundleId();

    /**
     * The bundle name.
     *
     * @return Bundle name or null
     */
    @Schema(
            description = "Name of the bundle",
            example = "My Content Bundle"
    )
    @Nullable
    String bundleName();

    /**
     * Whether a new bundle was created by this call.
     *
     * @return true if a new bundle was created
     */
    @Schema(
            description = "Whether a new bundle was created by this call",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean created();

    /**
     * Total number of assets submitted for addition.
     * Subtract {@code errors().size()} to get the count of successfully added assets.
     *
     * @return Total assets submitted
     */
    @Schema(
            description = "Total number of assets submitted (subtract errors count for successful adds)",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int total();

    /**
     * Per-asset error messages as human-readable strings (e.g., permission denied).
     * Empty list means all assets were added successfully.
     *
     * @return List of error messages
     */
    @Schema(
            description = "Per-asset error messages as human-readable strings. " +
                    "Empty list = all assets added successfully",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> errors();

}

package com.dotcms.rest.api.v1.publishing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Result of removing a single asset from a bundle.
 * Contains success/failure status and a human-readable message.
 *
 * @author hassandotcms
 * @since Mar 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = RemoveAssetResultView.class)
@JsonDeserialize(as = RemoveAssetResultView.class)
@Schema(description = "Result of removing a single asset from a bundle")
public interface AbstractRemoveAssetResultView {

    /**
     * The asset identifier that was processed.
     *
     * @return Asset ID
     */
    @Schema(
            description = "The asset identifier that was processed",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String assetId();

    /**
     * Whether the asset was successfully removed.
     *
     * @return true if removal was successful
     */
    @Schema(
            description = "Whether the asset was successfully removed from the bundle",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean success();

    /**
     * Human-readable result message.
     *
     * @return Result message
     */
    @Schema(
            description = "Human-readable result message",
            example = "Asset removed from bundle",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String message();

}

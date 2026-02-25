package com.dotcms.rest.api.v1.publishing;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Result of pushing a bundle to environments.
 * This view confirms the bundle was successfully queued for publishing.
 *
 * @author hassandotcms
 * @since Jan 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = PushBundleResultView.class)
@JsonDeserialize(as = PushBundleResultView.class)
@Schema(description = "Result of pushing a bundle to environments")
public interface AbstractPushBundleResultView {

    /**
     * Bundle identifier that was pushed.
     *
     * @return Bundle ID
     */
    @Schema(
            description = "Bundle identifier",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String bundleId();

    /**
     * Operation type performed.
     *
     * @return Operation: publish, expire, or publishexpire
     */
    @Schema(
            description = "Operation type performed",
            example = "publish",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String operation();

    /**
     * Scheduled publish date in ISO 8601 format.
     *
     * @return Publish date or null if not applicable
     */
    @Schema(
            description = "Scheduled publish date in ISO 8601 format",
            example = "2025-03-15T14:30:00-05:00"
    )
    @Nullable
    String publishDate();

    /**
     * Scheduled expire date in ISO 8601 format.
     *
     * @return Expire date or null if not applicable
     */
    @Schema(
            description = "Scheduled expire date in ISO 8601 format",
            example = "2025-04-15T14:30:00-05:00"
    )
    @Nullable
    String expireDate();

    /**
     * List of environment IDs the bundle was pushed to.
     *
     * @return Environment IDs
     */
    @Schema(
            description = "List of environment IDs the bundle was pushed to",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> environments();

    /**
     * Filter key used for publishing.
     *
     * @return Filter key
     */
    @Schema(
            description = "Filter key used for publishing",
            example = "ForcePush.yml",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String filterKey();

}

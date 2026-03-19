package com.dotcms.rest.api.v1.publishing;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.time.Instant;

/**
 * Represents all timestamps associated with a publishing job.
 * Includes bundle creation timestamps, publishing phase timestamps,
 * and audit record timestamps.
 *
 * @since Jan 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = TimestampsView.class)
@JsonDeserialize(as = TimestampsView.class)
@Schema(description = "Publishing job timestamps")
public interface AbstractTimestampsView {

    /**
     * When bundle creation started.
     *
     * @return Bundle start timestamp or null if not started
     */
    @Schema(
            description = "When bundle creation started (ISO 8601 format)",
            example = "2026-01-15T10:30:00Z"
    )
    @Nullable
    Instant bundleStart();

    /**
     * When bundle creation completed.
     *
     * @return Bundle end timestamp or null if not completed
     */
    @Schema(
            description = "When bundle creation completed (ISO 8601 format)",
            example = "2026-01-15T10:31:22Z"
    )
    @Nullable
    Instant bundleEnd();

    /**
     * When publishing to endpoints started.
     *
     * @return Publish start timestamp or null if not started
     */
    @Schema(
            description = "When publishing to endpoints started (ISO 8601 format)",
            example = "2026-01-15T10:30:05Z"
    )
    @Nullable
    Instant publishStart();

    /**
     * When publishing to endpoints completed.
     *
     * @return Publish end timestamp or null if not completed
     */
    @Schema(
            description = "When publishing to endpoints completed (ISO 8601 format)",
            example = "2026-01-15T10:31:18Z"
    )
    @Nullable
    Instant publishEnd();

    /**
     * When the audit record was created.
     *
     * @return Creation date
     */
    @Schema(
            description = "When the audit record was created (ISO 8601 format)",
            example = "2026-01-15T10:29:55Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    Instant createDate();

    /**
     * When the status was last updated.
     *
     * @return Status update timestamp or null
     */
    @Schema(
            description = "When the status was last updated (ISO 8601 format)",
            example = "2026-01-15T10:31:22Z"
    )
    @Nullable
    Instant statusUpdated();

}

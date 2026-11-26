package com.dotcms.rest.api.v1.index;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What {@code DELETE /api/v1/esindex/failed} did (#37270). Replaces the bare {@code true} the
 * endpoint returned before.
 *
 * @param cleared always {@code true} when the request succeeded
 * @param count   how many parked records were deleted
 */
@Schema(description = "Result of clearing the parked reindex records")
public record ClearedFailedRecordsView(
        @Schema(description = "True when the clear ran", example = "true")
        boolean cleared,
        @Schema(description = "Parked records deleted; records still being retried are kept",
                example = "380")
        int count) {
}
